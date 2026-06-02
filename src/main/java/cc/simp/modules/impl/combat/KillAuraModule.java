package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BlinkProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.InventoryUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.RayCastUtils;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
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
    public static final Property<Boolean> newCombat = new Property<>("1.9+ Combat Delays", false);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.Fake);
    public static Property<Boolean> alwaysShowBlocking = new  Property<>("Always Show Blocking", true, () -> ab.getValue() == AutoBlock.Legit);

    private final Property<Boolean> advanced = new Property<>("Advanced", false);
    private final Property<Boolean> missChance = new Property<>("Miss Chance", true, advanced::getValue);
    private final NumberProperty missRate = new NumberProperty("Miss Rate", 5, () -> advanced.getValue() && missChance.getValue(), 0, 20, 1);

    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Regular);
    private final Property<Boolean> predictiveRotations = new Property<>("Predictive Rotations", true, () -> rotations.getValue() == Rotations.Regular);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5);
    public static final Property<Boolean> jitter = new Property<>("Jitter Rotations", false);
    private final NumberProperty jitterFactor = new NumberProperty("Jitter Factor", 5, jitter::getValue, 1, 10, 1);
    public static final Property<Boolean> fix = new Property<>("Move Fix", true);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> legit = new Property<>("Simulate Mouse Clicks", true);
    public static final Property<Boolean> raycast = new Property<>("Ray Cast", true);
    private final Property<Boolean> teams = new Property<>("Teams", false);

    public enum Rotations {
        Regular,
        Polar,
        Snap,
        None
    }

    public enum AutoBlock {
        None,
        Fake,
        Blink,
        Switch,
        Legit,
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
    public int hitTicks;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = ignored -> {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null) {
            resetCombatState(false);
            return;
        }

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
            canAttack = true;
            return;
        }

        if (target == null) {
            unblock();
            canAttack = true;
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
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) {
            this.hitTicks++;
        }
    };

    @EventLink
    public final Listener<HitSlowDownEvent> hitSlowDownEventListener = e -> {
        if (sprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = ignored -> resetCombatState(true);

    @EventLink
    public final Listener<TickEvent> tickEventListener = ignored -> elapsedTicks++;

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = event -> {
        // Pasted from Novoline. Why cancel packets tho? It works I ain't gonna question it.
        if (ab.getValue() == AutoBlock.NCP && autoBlocking) {
            if (event.getPacket() instanceof C07PacketPlayerDigging packet) {
                if (packet.getStatus().equals(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)) {
                    event.setCancelled(true);
                }
            }

            if (event.getPacket() instanceof C08PacketPlayerBlockPlacement packet) {
                if (packet.getPlacedBlockDirection() == 255) {
                    event.setCancelled(true);
                }
            }
        }
    };

    private void calculateRotations() {
        if (mc.thePlayer == null || target == null || rotations.getValue() == Rotations.None) return;

        Vector2f rotation = RotationUtils.calculate(target, predictiveRotations.getValue(), seekRange.getValue());

        if (jitter.getValue()) {
            float jitterAmount = jitterFactor.getValue().floatValue();
            rotation.x += (float) ((Math.random() - 0.5) * jitterAmount);
            rotation.y += (float) ((Math.random() - 0.5) * jitterAmount);
        }

        float targetYaw = rotation.x;
        float targetPitch = rotation.y;
        float rotSpeed;

        rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());

        switch (rotations.getValue()) {
            case Regular:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
            case Polar: {
                Vector2f polar = generatePolarRotation(target, targetYaw, targetPitch, rotSpeed);
                RotationProcess.setRotations(polar, rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
            }
            break;
            case Snap:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), 180, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
        }
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null) return;

        if (target == null || !InventoryUtils.isHoldingSword()) {
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
                mc.gameSettings.keyBindUseItem.setPressed(mc.thePlayer.getDistanceToEntity(target) < 3 && hitTicks <= 5 && mc.thePlayer.ticksSinceVelocity >= 5);
                autoBlocking = alwaysShowBlocking.getValue() || mc.thePlayer.getDistanceToEntity(target) < 3 && hitTicks <= 5 && mc.thePlayer.ticksSinceVelocity >= 5;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = blockTicks >= 2;
                break;
            case Vanilla, NCP:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case Blink:
                if (isObjectMouseOverBlock() && mc.playerController.curBlockDamageMP != 0) {
                    blockTicks = 0;
                }
                blockTicks++;
                if (blockTicks >= 3) blockTicks = 1;

                switch (blockTicks) {
                    case 1:
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        autoBlocking = true;
                        BlinkProcess.disable();
                        break;
                    case 2:
                        if (mc.thePlayer.ticksExisted % 2 == 0) {
                            BlinkProcess.enable();
                        }
                        break;
                }
                break;

            case Switch:
                if (isObjectMouseOverBlock() && mc.playerController.curBlockDamageMP != 0) {
                    blockTicks = 0;
                }
                blockTicks++;
                if (blockTicks >= 3) blockTicks = 1;

                switch (blockTicks) {
                    case 1:
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        autoBlocking = true;
                        PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                        break;
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
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTicks = -1;

        if (ab.getValue() == AutoBlock.Blink) {
            BlinkProcess.disable();
        }

        if (ab.getValue() == AutoBlock.Fake) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.Legit && autoBlocking && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InventoryUtils.isHoldingSword() && ab.getValue() != AutoBlock.Legit) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void attack() {
        if (mc.thePlayer == null || mc.playerController == null || target == null || !canAttack) return;

        if (!hitTimerDone()) return;

        double dist = mc.thePlayer.getDistanceToEntity(target);
        if (dist > killRange.getValue()) return;

        if (advanced.getValue() && missChance.getValue()) {
            if (shouldMiss) {
                shouldMiss = false;
                this.hitTicks = 0;
                attackTimer.reset();
                return;
            }
            if (Math.random() * 100 < missRate.getValue()) {
                shouldMiss = true;
                this.hitTicks = 0;
                attackTimer.reset();
                return;
            }
        }

        if (raycast.getValue()) {
            MovingObjectPosition mop = RayCastUtils.rayCast(RotationProcess.rotations, killRange.getValue());
            if (mop == null || mop.entityHit != target) return;
        }

        if (!legit.getValue()) {
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
        } else {
            mc.clickMouse();
        }
        this.hitTicks = 0;
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (!newCombat.getValue()) {
            if (attackTimer.hasTimeElapsed(delay, false)) {
                // compute a sane random cps between min and max (min may be equal/less than max)
                double minVal = min.getValue();
                double maxVal = max.getValue();
                if (maxVal <= 0) maxVal = 1.0; // avoid divide by zero
                if (minVal < 0) minVal = 0.0;
                // ensure ordering
                if (minVal > maxVal) {
                    double t = minVal;
                    minVal = maxVal;
                    maxVal = t;
                }

                double cps = MathUtils.getRandom(minVal, maxVal);
                cps = Math.max(1.0, cps); // ensure at least 1 CPS

                returnVal = true;
                attackTimer.reset();
                delay = (long) (1000.0 / cps);
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
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return 3;
        }

        int toolDelay = 3;
        if (mc.thePlayer.inventory.getCurrentItem() == null) {
            return toolDelay;
        } else {
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
                toolDelay = 12;
            } else if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemAxe) {
                toolDelay = 25;
            } else if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemPickaxe) {
                toolDelay = 16;
            } else if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemTool) {
                toolDelay = 20;
            }
        }
        return toolDelay;
    }

    private boolean shouldBlockPredictive() {
        if (mc.thePlayer == null || target == null) return false;

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
        if (deltaMag < 1.0E-6 || lookMag < 1.0E-6) {
            return false;
        }

        double dotProduct = (deltaX * targetLookX + deltaY * targetLookY + deltaZ * targetLookZ) / (deltaMag * lookMag);

        return dotProduct > 0.5 && target.swingProgress > 0;
    }

    /**
     * Generate a highly non-deterministic "polar" rotation around the base target angles.
     * The result is intentionally noisy and uses multiple independent randomness sources so
     * that repeated invocations produce very different results while still generally
     * pointing near the target (so gameplay remains functional).
     */
    private Vector2f generatePolarRotation(EntityLivingBase targetEntity, float baseYaw, float basePitch, float rotSpeed) {
        // seed components: time, target id, tick count, and a fresh random
        long time = System.nanoTime();
        int id = targetEntity != null ? targetEntity.getEntityId() : 0;
        long ticks = mc.thePlayer != null ? mc.thePlayer.ticksExisted : 0;
        long seed = time ^ (((long) id) << 32) ^ (ticks * 0x9E3779B97F4A7C15L) ^ Double.doubleToLongBits(Math.random());

        java.util.Random rng = new java.util.Random(seed);

        // base aim toward target, then apply multiple harmonic and stochastic components
        double t = (System.currentTimeMillis() / 1000.0) + rng.nextDouble() * 10.0;

        // component frequencies (use incommensurate primes + randomness)
        double[] freqs = new double[]{0.07 + rng.nextDouble() * 0.05, 0.13 + rng.nextDouble() * 0.08, 0.29 + rng.nextDouble() * 0.12};
        // amplitudes derived from rotSpeed and additional randomness, clamped to safe ranges
        double maxBaseAmp = Math.max(1.0, Math.min(12.0, rotSpeed * 2.5));
        double[] ampsYaw = new double[]{rng.nextDouble() * maxBaseAmp, rng.nextDouble() * (maxBaseAmp / 1.5), rng.nextDouble() * (maxBaseAmp / 2.0)};
        double[] ampsPitch = new double[]{rng.nextDouble() * (maxBaseAmp / 2.0), rng.nextDouble() * (maxBaseAmp / 3.0), rng.nextDouble() * (maxBaseAmp / 4.0)};

        double yawOffset = 0.0;
        double pitchOffset = 0.0;

        for (int i = 0; i < freqs.length; i++) {
            double phase = rng.nextDouble() * Math.PI * 2.0;
            yawOffset += ampsYaw[i] * Math.sin(2.0 * Math.PI * freqs[i] * t + phase);
            pitchOffset += ampsPitch[i] * Math.cos(2.0 * Math.PI * freqs[i] * t + phase * 0.7);
        }

        // add short-lived spikes and gaussian micro-noise to break smooth patterns
        if (rng.nextDouble() < 0.08) { // occasional spike
            yawOffset += (rng.nextDouble() - 0.5) * 30.0 * rng.nextDouble();
            pitchOffset += (rng.nextDouble() - 0.5) * 12.0 * rng.nextDouble();
        }

        yawOffset += rng.nextGaussian() * (0.5 + rng.nextDouble() * 2.0);
        pitchOffset += rng.nextGaussian() * (0.3 + rng.nextDouble() * 1.0);

        // small deterministic component influenced by target movement to preserve responsiveness
        if (targetEntity != null) {
            double vx = targetEntity.motionX;
            double vz = targetEntity.motionZ;
            double velFactor = Math.sqrt(vx * vx + vz * vz);
            yawOffset += velFactor * (rng.nextDouble() * 8.0 - 4.0);
            pitchOffset += velFactor * (rng.nextDouble() * 3.0 - 1.5);
        }

        // final scaling: bias offsets to be proportional but clamped so we still are usable
        double yawClamp = 35.0; // don't exceed extreme yaw offsets
        double pitchClamp = 30.0; // don't exceed extreme pitch offsets

        double finalYaw = baseYaw + clamp(yawOffset, -yawClamp, yawClamp);
        double finalPitch = basePitch + clamp(pitchOffset, -pitchClamp, pitchClamp);

        // ensure pitch stays inside valid looking angles
        finalPitch = (float) Math.max(-90.0, Math.min(90.0, finalPitch));

        return new Vector2f((float) finalYaw, (float) finalPitch);
    }

    private double clamp(double v, double a, double b) {
        if (v < a) return a;
        if (v > b) return b;
        return v;
    }

    @Override
    public void onEnable() {
        // initialize delay using the same safe min/max handling as hitTimerDone
        double minVal = min.getValue();
        double maxVal = max.getValue();
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double t = minVal;
            minVal = maxVal;
            maxVal = t;
        }
        double cps = MathUtils.getRandom(minVal, maxVal);
        cps = Math.max(1.0, cps);
        delay = (long) (1000.0 / cps);
        elapsedTicks = 0;
        shouldMiss = false;
        canAttack = true;
        autoBlocking = false;
        blockTicks = -1;
        attackTimer.reset();
        super.onEnable();
    }

    public void onDisable() {
        resetCombatState(true);
        super.onDisable();
    }

    private void resetCombatState(boolean clearTargetList) {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }

        target = null;
        shouldMiss = false;
        elapsedTicks = 0;
        delay = 0;
        blockTicks = -1;
        attackTimer.reset();

        if (clearTargetList) {
            targetList.clear();
        }
    }

    private boolean isObjectMouseOverBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }
}
