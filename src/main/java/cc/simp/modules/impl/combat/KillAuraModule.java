package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.impl.player.ClickEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.MovementFixModule;
import cc.simp.modules.impl.movement.SprintModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.modules.impl.player.ClientRotationsModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.PlayerUtils;
import cc.simp.utils.client.mc.RaytraceUtils;
import cc.simp.utils.client.mc.RotationUtils;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

import java.util.Comparator;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    private final DoubleProperty minAttackDelayProperty = new DoubleProperty("Min Attack Delay", 40.0, 0.0, 400.0, 1);
    private final DoubleProperty maxAttackDelayProperty = new DoubleProperty("Max Attack Delay", 40.0, 0.0, 400.0, 1);
    public static DoubleProperty rangeProperty = new DoubleProperty("Range", 3, 3, 6, 0.1);
    private final EnumProperty<TargetType> targetTypeProperty = new EnumProperty<>("Target", TargetType.PLAYERS);
    public static EnumProperty<AutoBlockType> autoBlockTypeProperty = new EnumProperty<>("Auto Block", AutoBlockType.FAKE);
    private final Property<Boolean> legitTimingsProperty = new Property<>("Legit Timings", true);
    private final Property<Boolean> keepSprintProperty = new Property<>("Keep Sprint", false);
    private final Property<Boolean> sprintRotationFixProperty = new Property<>("Sprint Rotation Fix", false, () -> keepSprintProperty.getValue() == false);

    public enum TargetType {
        PLAYERS,
        MOBS,
        ANIMALS,
        ALL
    }

    public static enum AutoBlockType {
        NONE,
        FAKE,
        VANILLA
    }

    public KillAuraModule() {
        setSuffixListener(targetTypeProperty);
    }

    public static EntityLivingBase target;
    private long randomDelay = 100L;
    private boolean rotated = false;
    private Timer hitTimeHelper = new Timer();
    public static boolean autoBlocking = false;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if (target == null && autoBlocking) {
            autoBlocking = false;
        }

        updateTarget();

        if (!targetIsValid()) {
            target = null;
            return;
        }

        if (event.isPre()) {
            Simp.INSTANCE.getRotationManager().faceEntity(target, ClientRotationsModule.rotSpeed.getValue().floatValue());
            rotated = true;

            if(sprintRotationFixProperty.getValue() && !keepSprintProperty.getValue() && !Simp.INSTANCE.getModuleManager().getModule(MovementFixModule.class).isEnabled() && !MovementFixModule.killAuraProperty.getValue()) {
                handleSprinting(Simp.INSTANCE.getRotationManager().getClientYaw());
            }

            if (!legitTimingsProperty.getValue() && target != null) {
                this.autoBlock();
                this.attack();
            }
        }
    };

    @EventLink
    public final Listener<ClickEvent> clickEventListener = event -> {
        if (legitTimingsProperty.getValue() && target != null) {

            if (!Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() && mc.currentScreen == null && rotated) {
                event.setCancelled(true);
            }

            this.autoBlock();
            this.legitAttack();
        }
    };

    private void updateTarget() {
        target = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase) entity)
                .filter(this::isValidTarget)
                .min(Comparator.comparingDouble(entity ->
                        mc.thePlayer.getDistanceToEntity(entity)))
                .orElse(null);
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity.isDead ||
                entity.getDistanceToEntity(mc.thePlayer) > rangeProperty.getValue())
            return false;

        switch (targetTypeProperty.getValue()) {
            case PLAYERS:
                return entity instanceof EntityPlayer;
            case MOBS:
                return entity instanceof EntityMob;
            case ANIMALS:
                return entity instanceof EntityAnimal;
            case ALL:
                return true;
            default:
                return false;
        }
    }

    private boolean targetIsValid() {
        return target != null && !target.isDead &&
                target.getDistanceToEntity(mc.thePlayer) <= rangeProperty.getValue();
    }

    private float[] getRotations() {
        return RotationUtils.getClosestRotations(target, 0.03f);
    }

    private boolean isLookingAtEntity() {
        boolean notOnEntity;
        if (target == null)
            return false;
        MovingObjectPosition hitResult = RaytraceUtils.getMouseOver(getRotations(), rangeProperty.getValue());
        if (hitResult == null) return false;
        notOnEntity = hitResult.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                || hitResult.entityHit != target;
        if (hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hitResult.entityHit == target) {
            mc.objectMouseOver = hitResult;
        }
        return !notOnEntity;
    }

    private void setRandomDelay() {
        if (this.minAttackDelayProperty.getValue() == 0.0 && this.maxAttackDelayProperty.getValue() == 0.0) {
            this.randomDelay = 0L;
        } else if (Math.abs(this.minAttackDelayProperty.getValue() - this.maxAttackDelayProperty.getValue()) > 0.0) {
            this.randomDelay = (long) MathUtils.nextSecureInt(this.minAttackDelayProperty.getValue().intValue(), this.maxAttackDelayProperty.getValue().intValue());
        } else {
            this.randomDelay = this.minAttackDelayProperty.getValue().longValue();
        }
    }

    private void attack() {
        if (legitTimingsProperty.getValue()) return;
        if (this.hitTimeHelper.hasTimeElapsed(this.randomDelay)) {
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
        }

        if (keepSprintProperty.getValue()) {
            mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
        }

        this.setRandomDelay();
        this.hitTimeHelper.reset();
    }

    private void legitAttack() {
        if (!legitTimingsProperty.getValue()) return;

        if (!isLookingAtEntity()) {
            return;
        }

        if (this.hitTimeHelper.hasTimeElapsed(this.randomDelay)) {
            mc.clickMouse();
        }

        if (keepSprintProperty.getValue()) {
            mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
        }

        this.setRandomDelay();
        this.hitTimeHelper.reset();
    }

    public void autoBlock() {

        if (target == null || !PlayerUtils.isHoldingSword()) {
            autoBlocking = false;
            return;
        }

        switch (autoBlockTypeProperty.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case VANILLA:
                mc.thePlayer.setItemInUse(mc.thePlayer.inventory.getCurrentItem(), 32678);
                autoBlocking = true;
                break;
        }

    }

    private void handleSprinting(float targetYaw) {
        // Only apply fix if we are actually sprinting
        if (!mc.thePlayer.isSprinting() || !mc.gameSettings.keyBindSprint.isPressed()) {
            return;
        }

        // Calculate the yaw difference between current player yaw and the yaw to target
        float currentYaw = mc.thePlayer.rotationYaw;
        float yawDifference = Math.abs(MathHelper.wrapAngleTo180_float(currentYaw - targetYaw));

        // If the turn required is greater than the threshold, stop sprinting
        if (yawDifference > 30) {
            mc.thePlayer.setSprinting(false);
            mc.gameSettings.keyBindSprint.setPressed(false);
            // Optionally, add a slight delay before allowing sprinting again if needed
        }
    }

    @Override
    public void onDisable() {
        hitTimeHelper.reset();
        if (autoBlocking) {
            autoBlocking = false;
            mc.thePlayer.clearItemInUse();
        }
        rotated = false;
        target = null;
    }
}