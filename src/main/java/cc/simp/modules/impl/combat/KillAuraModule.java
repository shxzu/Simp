package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.LagProcess; 
import cc.simp.processes.RotationProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Noise;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.InventoryUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.RayCastUtils;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MovementFix;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    public static ModeProperty<TargetSelectionProcess.Mode> mode = new ModeProperty<>("Mode", TargetSelectionProcess.Mode.Adaptive);
    public static ModeProperty<TargetSelectionProcess.Entities> entities = new ModeProperty<>("Entities", TargetSelectionProcess.Entities.Optimal);
    private final NumberProperty switchSpeed = new NumberProperty("Switch Speed", 2, () -> mode.getValue() == TargetSelectionProcess.Mode.Switch, 0, 10, 1);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 4.2, 3, 6, 0.1);
    public static NumberProperty killRange = new NumberProperty("Kill Range", 3, 3, 6, 0.1);
    public static NumberProperty blockingRange = new NumberProperty("Blocking Range", 4.2, 3, 6, 0.1);
    public static final Property<Boolean> newCombat = new Property<>("New Combat Delays", false);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.Fake);
    private final NumberProperty legitBlockInterval = new NumberProperty("Legit Block Interval", 4, () -> ab.getValue() == AutoBlock.Legit, 2, 10, 1);
    private final Property<Boolean> legitRandomize = new Property<>("Legit Randomize", true, () -> ab.getValue() == AutoBlock.Legit);

    private final Property<Boolean> advanced = new Property<>("Advanced", false);
    private final Property<Boolean> missChance = new Property<>("Miss Chance", true, advanced::getValue);
    private final NumberProperty missRate = new NumberProperty("Miss Rate", 5, () -> advanced.getValue() && missChance.getValue(), 0, 20, 1);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, advanced::getValue, 0, 10, 0.5);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, advanced::getValue, 0, 10, 0.5);

    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Regular);
    private final NumberProperty speed = new NumberProperty("Rotation Speed", 5, () -> !advanced.getValue(), 0, 10, 1);
    public static final Property<Boolean> jitter = new Property<>("Jitter Rotations", false);
    public static final Property<Boolean> fix = new Property<>("Move Fix", true);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> onlyInAirSprint = new Property<>("Keep Sprint Only In Air", false, sprint::getValue);
    public static final Property<Boolean> legit = new Property<>("Legit", true);
    public static final Property<Boolean> raycast = new Property<>("Ray Cast", true);
    private final Property<Boolean> teams = new Property<>("Teams", false);

    public enum Rotations {
        Regular,
        Puhfy, // omg i did it @puhfy! r u proud?
        Polar,
        Snap,
        Player,
        None
    }

    public enum AutoBlock {
        None,
        Fake,
        Blink,
        Switch,
        Legit,
        Predictive,
        NCP,
        Vanilla
    }

    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    List<Entity> targetList = new CopyOnWriteArrayList<>();
    private static final Timer attackTimer = new Timer();
    int blockTicks = 0;
    static long delay = 0;
    static int elapsedTicks = 0;
    private boolean shouldMiss = false;
    private boolean wasBlocking = false;
    private int blockCooldown = 0;
    private Noise noise;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        setSuffix(mode.getValue().toString());

        TargetSelectionProcess.setMode(mode.getValue());
        TargetSelectionProcess.setEntities(entities.getValue());
        TargetSelectionProcess.setSeekRange(seekRange.getValue().floatValue());
        TargetSelectionProcess.setDontTargetTeams(teams.getValue());
        TargetSelectionProcess.setSwitchTime(switchSpeed.getValue().intValue());

        targetList = TargetSelectionProcess.getTargetList();
        target = TargetSelectionProcess.getTarget();

        if (targetList.isEmpty()) {
            target = null;
            unblock();
            return;
        }

        if (target == null) {
            unblock();
            return;
        }

        calculateRotations();
        if (ab.getValue() != AutoBlock.None) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockingRange.getValue() && InventoryUtils.isHoldingSword()) {
                autoblock();
            }
        }
        attack();
    };

    @EventLink
    public final Listener<HitSlowDownEvent> hitSlowDownEventListener = e -> {
        if (sprint.getValue() && (!onlyInAirSprint.getValue() || !mc.thePlayer.onGround)) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        unblock();
        canAttack = true;
        target = null;
        targetList.clear();
    };

    @EventLink
    public final Listener<TickEvent> tickEventListener = e -> {
        elapsedTicks++;
    };

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = event -> {
        // Pasted from Novoline. Why cancel packets tho? It works I ain't gonna question it.
        if (ab.getValue() == AutoBlock.NCP && autoBlocking) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();

                if (packet.getStatus().equals(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)) {
                    event.setCancelled(true);
                }
            }

            if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) event.getPacket();

                if (packet.getPlacedBlockDirection() == 255) {
                    event.setCancelled(true);
                }
            }
        }
    };

    private void calculateRotations() {
        if (target == null || rotations.getValue() == Rotations.None) return;

        Vector2f rotation = RotationUtils.calculate(target, mode.getValue() == TargetSelectionProcess.Mode.Adaptive, seekRange.getValue());

        if (rotations.getValue() == Rotations.Puhfy) {
            rotation = RotationUtils.puhfyRotations(target);
        }

        if (jitter.getValue()) {
            rotation.x += (float) ((Math.random() - 0.5) * 2);
            rotation.y += (float) ((Math.random() - 0.5) * 2);
        }

        float targetYaw = rotation.x;
        float targetPitch = rotation.y;
        float rotSpeed;

        if (advanced.getValue()) {
            rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        } else {
            final double minRotationSpeed = this.speed.getValue();
            final double maxRotationSpeed = this.speed.getValue() * Math.random();
            rotSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);
        }

        switch (rotations.getValue()) {
            case Regular:
            case Puhfy:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
            case Polar:
                int sped = 8;
                int existed = mc.thePlayer.ticksExisted * sped;

                float horizontalScaleDevide = (float) (1.9f
                        + Math.max(-0.65, ((3 - mc.thePlayer.getDistanceToEntity(target)) / 3)));
                float verticalScaleDevide = (float) (1.5f
                        + Math.max(-0.65, ((3 - mc.thePlayer.getDistanceToEntity(target)) / 3)));

                double randomizedX = target.posX + (noise.GetNoise(existed + 50, existed + 250) / horizontalScaleDevide);
                double randomizedY = target.posY + 0.7
                        + (noise.GetNoise(existed + 100, existed + 100) / verticalScaleDevide);
                double randomizedZ = target.posZ + (noise.GetNoise(existed + 0, existed + 150) / horizontalScaleDevide);

                Vector2f rots = new Vector2f(RotationUtils.getNormalRotationsFromPosition(randomizedX, randomizedY, randomizedZ,
                        RotationProcess.rotations.getX(), RotationProcess.rotations.getY(), rotSpeed, rotSpeed)[0], RotationUtils.getNormalRotationsFromPosition(randomizedX, randomizedY, randomizedZ,
                        RotationProcess.rotations.getX(), RotationProcess.rotations.getY(), rotSpeed, rotSpeed)[1]);

                RotationProcess.setRotations(rots, rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
            case Snap:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), 180, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
            case Player:
                float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
                float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

                float maxRotationStep = rotSpeed * 18.0f;
                yawDiff = MathHelper.clamp_float(yawDiff, -maxRotationStep, maxRotationStep);
                pitchDiff = MathHelper.clamp_float(pitchDiff, -maxRotationStep, maxRotationStep);

                mc.thePlayer.rotationYaw += yawDiff;
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchDiff, -90.0f, 90.0f);
                break;
        }
    }

    private void autoblock() {
        if (target == null || !InventoryUtils.isHoldingSword() || BadPacketsProcess.bad()) {
            if (autoBlocking) {
                unblock();
            }
            return;
        }

        switch (ab.getValue()) {
            case Fake:
                autoBlocking = true;
                break;

            case Legit:
                int interval = legitBlockInterval.getValue().intValue();
                if (legitRandomize.getValue()) {
                    interval += (mc.thePlayer.ticksExisted % 3) - 1;
                    interval = Math.max(2, interval);
                }

                if (mc.thePlayer.ticksExisted % interval == 0) {
                    if (!wasBlocking && blockCooldown == 0) {
                        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                        autoBlocking = true;
                        wasBlocking = true;
                        blockCooldown = 2;
                    }
                    canAttack = false;
                } else {
                    if (wasBlocking && blockCooldown == 0) {
                        if (!hitTimerDone() || mc.thePlayer.getDistanceToEntity(target) > killRange.getValue()) {
                            mc.thePlayer.stopUsingItem();
                            wasBlocking = false;
                            autoBlocking = false;
                            canAttack = true;
                        }
                        blockCooldown = 2;
                    }
                }

                if (blockCooldown > 0) blockCooldown--;
                break;

            case Predictive:
                boolean shouldBlock = shouldBlockPredictive();

                if (shouldBlock && !wasBlocking && blockCooldown == 0) {
                    mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                    autoBlocking = true;
                    wasBlocking = true;
                    canAttack = false;
                    blockCooldown = 3;
                } else if (!shouldBlock && wasBlocking && blockCooldown == 0) {
                    // Only unblock if not about to attack
                    if (!hitTimerDone() || mc.thePlayer.getDistanceToEntity(target) > killRange.getValue()) {
                        mc.thePlayer.stopUsingItem();
                        wasBlocking = false;
                        autoBlocking = false;
                        canAttack = true;
                    }
                    blockCooldown = 3;
                }

                if (blockCooldown > 0) blockCooldown--;
                break;

            case Vanilla:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;

            case Blink:
                if (mc.playerController.curBlockDamageMP != 0 && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    blockTicks = 0;
                }
                blockTicks++;
                if (blockTicks >= 3) blockTicks = 1;

                switch (blockTicks) {
                    case 1:
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        autoBlocking = true;
                        LagProcess.dispatch();
                    case 2:
                        if (mc.thePlayer.ticksExisted % 2 == 0) {
                            LagProcess.blink();
                        }
                        break;
                }
                break;

            case Switch:
                if (mc.playerController.curBlockDamageMP != 0 && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    blockTicks = 0;
                }
                blockTicks++;
                if (blockTicks >= 3) blockTicks = 1;

                switch (blockTicks) {
                    case 1:
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        autoBlocking = true;
                        PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 8));
                    case 2:
                        if (mc.thePlayer.ticksExisted % 2 == 0) {
                            PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem)));
                        }
                        break;
                }
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) return;

        if (ab.getValue() == AutoBlock.Fake) {
            autoBlocking = false;
            return;
        }

        if ((ab.getValue() == AutoBlock.Legit || ab.getValue() == AutoBlock.Predictive) && wasBlocking) {
            mc.thePlayer.stopUsingItem();
            canAttack = true;
            autoBlocking = false;
            wasBlocking = false;
            return;
        }

        if (InventoryUtils.isHoldingSword()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        autoBlocking = false;
    }

    private void attack() {
        if (target == null || !canAttack) return;

        if (!hitTimerDone()) return;

        if (mc.thePlayer.getDistanceToEntity(target) > killRange.getValue()) return;

        if (advanced.getValue() && missChance.getValue()) {
            if (shouldMiss) {
                shouldMiss = false;
                attackTimer.reset();
                return;
            }
            if (Math.random() * 100 < missRate.getValue()) {
                shouldMiss = true;
                attackTimer.reset();
                return;
            }
        }

        if (raycast.getValue()) {
            MovingObjectPosition mop = RayCastUtils.rayCast(RotationProcess.rotations, killRange.getValue());
            if (mop == null || mop.entityHit != target) return;
        }

        if (target.getDistanceToEntity(mc.thePlayer) > killRange.getValue()) return;

        if (!legit.getValue()) {
            if (!canAttack) return;
            if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
                mc.playerController.attackEntity(mc.thePlayer, target);
                mc.thePlayer.swingItem();
            } else {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);
            }
        } else {
            mc.clickMouse();
        }
    }


    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (!newCombat.getValue()) {
            if (attackTimer.hasTimeElapsed(delay, false)) {
                returnVal = true;
                attackTimer.reset();
                delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.min(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
            }
        } else {
            if (elapsedTicks >= getNewCombatDelay()) {
                elapsedTicks = 0;
                returnVal = true;
            }
        }
        return returnVal;
    }

    private static int getNewCombatDelay() {
        int toolDelay = 3;
        if (mc.thePlayer.inventory.getCurrentItem() == null) {
            return toolDelay;
        } else {
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
                toolDelay = 12;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemTool) {
                toolDelay = 20;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemPickaxe) {
                toolDelay = 16;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemAxe) {
                toolDelay = 25;
            }
        }
        return toolDelay;
    }

    private boolean shouldBlockPredictive() {
        if (target == null) return false;

        double distance = mc.thePlayer.getDistanceToEntity(target);
        if (distance > 6.0) return false;

        double targetX = target.posX;
        double targetY = target.posY + target.getEyeHeight();
        double targetZ = target.posZ;

        double deltaX = mc.thePlayer.posX - targetX;
        double deltaY = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - targetY;
        double deltaZ = mc.thePlayer.posZ - targetZ;

        float yaw = (float) Math.toRadians(target.rotationYaw);
        float pitch = (float) Math.toRadians(target.rotationPitch);

        double targetLookX = -Math.sin(yaw) * Math.cos(pitch);
        double targetLookY = -Math.sin(pitch);
        double targetLookZ = Math.cos(yaw) * Math.cos(pitch);

        double deltaMag = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double lookMag = Math.sqrt(targetLookX * targetLookX + targetLookY * targetLookY + targetLookZ * targetLookZ);

        double dotProduct = (deltaX * targetLookX + deltaY * targetLookY + deltaZ * targetLookZ) / (deltaMag * lookMag);

        return dotProduct > 0.5 && target.swingProgress > 0;
    }


    private boolean interactable(Block block) {
        return block == Blocks.chest || block == Blocks.trapped_chest || block == Blocks.crafting_table
                || block == Blocks.furnace || block == Blocks.ender_chest || block == Blocks.enchanting_table;
    }

    @Override
    public void onEnable() {
        delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.max(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
        noise = new Noise();
        super.onEnable();
    }

    public void onDisable() {
        canAttack = true;
        target = null;
        shouldMiss = false;
        targetList.clear();
        wasBlocking = false;
        blockCooldown = 0;
        unblock();
        super.onDisable();
    }
}
