package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.LagProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Adaptive);
    private final NumberProperty switchSpeed = new NumberProperty("Switch Speed", 2, () -> mode.getValue() == Mode.Switch, 0, 10, 1);
    public static ModeProperty<Entities> entities = new ModeProperty<>("Entities", Entities.Optimal);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 4.2, 3, 6, 0.1);
    public static NumberProperty killRange = new NumberProperty("Kill Range", 3, 3, 6, 0.1);
    public static NumberProperty blockingRange = new NumberProperty("Blocking Range", 4.2, 3, 6, 0.1);
    public static final Property<Boolean> newCombat = new Property<>("New Combat Delays", false);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, () -> !newCombat.getValue(),0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.Fake);
    private final NumberProperty legitBlockInterval = new NumberProperty("Legit Block Interval", 4, () -> ab.getValue() == AutoBlock.Legit, 2, 10, 1);
    private final Property<Boolean> legitRandomize = new Property<>("Legit Randomize", true, () -> ab.getValue() == AutoBlock.Legit);
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Regular);
    private final NumberProperty speed = new NumberProperty("Rotation Speed", 5, 0, 10, 1);
    public static final Property<Boolean> jitter = new Property<>("Jitter Rotations", false);
    public static final Property<Boolean> fix = new Property<>("Move Fix", true);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> legit = new Property<>("Legit", true);
    public static final Property<Boolean> raycast = new Property<>("Ray Cast", true);
    private final Property<Boolean> teams = new Property<>("Teams", false);

    public enum Mode {
        Adaptive,
        Single,
        Switch
    }

    public enum Entities {
        Optimal,
        Players,
        All
    }

    public enum Rotations {
        Regular,
        Snap,
        None
    }

    public enum AutoBlock {
        None,
        Fake,
        Blink,
        Switch,
        Legit,
        Predictive,
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

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        setSuffix(mode.getValue().toString());

        TargetSelectionProcess.setEntities(entities.getValue());
        TargetSelectionProcess.setSeekRange(seekRange.getValue().floatValue());
        TargetSelectionProcess.setDontTargetTeams(teams.getValue());

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
        if (sprint.getValue()) {
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

    private void calculateRotations() {
        if (target == null || rotations.getValue() == Rotations.None) return;

        Vector2f rotation = RotationUtils.calculate(target, mode.getValue() == Mode.Adaptive, seekRange.getValue());

        if (jitter.getValue()) {
            rotation.x += (float) ((Math.random() - 0.5) * 2);
            rotation.y += (float) ((Math.random() - 0.5) * 2);
        }

        float targetYaw = rotation.x;
        float targetPitch = rotation.y;

        switch (rotations.getValue()) {
            case Regular:
                /* Smoothing rotations */
                final double minRotationSpeed = this.speed.getValue();
                final double maxRotationSpeed = this.speed.getValue() * Math.random();
                float rotSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;

            case Snap:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), 10, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
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
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                    autoBlocking = true;
                    canAttack = false;
                } else {
                    unblock();
                }
                break;
            case Predictive:
                if (shouldBlockPredictive()) {
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                    autoBlocking = true;
                    canAttack = false;
                } else {
                    unblock();
                }
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
        if (ab.getValue() == AutoBlock.Legit || ab.getValue() == AutoBlock.Predictive) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            canAttack = true;
        }
        if (ab.getValue() != AutoBlock.Legit && ab.getValue() != AutoBlock.Predictive && InventoryUtils.isHoldingSword()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        autoBlocking = false;
    }

    private void attack() {
        if (target == null || !canAttack) return;

        if (!hitTimerDone()) return;

        if (mc.thePlayer.getDistanceToEntity(target) > killRange.getValue()) return;

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
        if(!newCombat.getValue()) {
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

        // Check if target is within attack range and looking at us
        double distance = mc.thePlayer.getDistanceToEntity(target);
        if (distance > 6.0) return false;

        // Get target's eye position and rotation
        double targetX = target.posX;
        double targetY = target.posY + target.getEyeHeight();
        double targetZ = target.posZ;

        // Calculate vector from target to player
        double deltaX = mc.thePlayer.posX - targetX;
        double deltaY = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - targetY;
        double deltaZ = mc.thePlayer.posZ - targetZ;

        // Calculate target's look vector
        float yaw = (float) Math.toRadians(target.rotationYaw);
        float pitch = (float) Math.toRadians(target.rotationPitch);

        double targetLookX = -Math.sin(yaw) * Math.cos(pitch);
        double targetLookY = -Math.sin(pitch);
        double targetLookZ = Math.cos(yaw) * Math.cos(pitch);

        // Normalize vectors
        double deltaMag = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double lookMag = Math.sqrt(targetLookX * targetLookX + targetLookY * targetLookY + targetLookZ * targetLookZ);

        // Calculate dot product (cosine of angle)
        double dotProduct = (deltaX * targetLookX + deltaY * targetLookY + deltaZ * targetLookZ) / (deltaMag * lookMag);

        // If angle is < 60 degrees (cos > 0.5), target is looking at us
        return dotProduct > 0.5 && target.swingProgress > 0;
    }

    @Override
    public void onEnable() {
        delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.max(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
        super.onEnable();
    }

    @Override
    public void onDisable() {
        canAttack = true;
        target = null;
        targetList.clear();
        unblock();
        super.onDisable();
    }
}
