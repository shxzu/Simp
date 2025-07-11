package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.impl.packet.PacketSendEvent;
import cc.simp.event.impl.player.ClickEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SprintModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.modules.impl.player.SmoothRotationsModule;
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
import jdk.jfr.internal.EventClassBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.Comparator;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    private final DoubleProperty minAttackDelayProperty = new DoubleProperty("Min Attack Delay", 40.0, 0.0, 400.0, 1);
    private final DoubleProperty maxAttackDelayProperty = new DoubleProperty("Max Attack Delay", 40.0, 0.0, 400.0, 1);
    public static DoubleProperty rangeProperty = new DoubleProperty("Range", 3, 3, 6, 0.1);
    private final EnumProperty<TargetType> targetTypeProperty = new EnumProperty<>("Target", TargetType.PLAYERS);
    public static EnumProperty<AutoBlockType> autoBlockTypeProperty = new EnumProperty<>("Auto Block", AutoBlockType.FAKE);
    private final Property<Boolean> keepSprintProperty = new Property<>("KeepSprint", false);
    private final Property<Boolean> raytraceProperty = new Property<>("Raytrace", false);

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

        updateTarget();

        if (!targetIsValid()) {
            target = null;
            return;
        }

        if (event.isPre()) {
            if (Simp.INSTANCE.getModuleManager().getModule(SmoothRotationsModule.class).isEnabled()) {
                event.setYaw(RotationUtils.smoothYaw(getRotations()[0]));
                event.setPitch(RotationUtils.smoothPitch(getRotations()[1]));
            } else {
                event.setYaw(getRotations()[0]);
                event.setPitch(getRotations()[1]);
            }
            rotated = true;
        }
    };

    @EventLink
    public final Listener<ClickEvent> clickEventListener = event -> {

        if (target != null && !Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() && mc.currentScreen == null && rotated) {
            event.setCancelled(true);
        }

        if (raytraceProperty.getValue() && target != null) {
            if (!isLookingAtEntity())
                return;
        }

        this.attack();

    };

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = event -> {
        if (!rotated) return;
        if (raytraceProperty.getValue() && target != null) {
            if (!isLookingAtEntity())
                return;
        }
        autoBlock(event);
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
        if(hitResult == null) return false;
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
        if (this.hitTimeHelper.hasTimeElapsed(this.randomDelay)) {

            int tickCounter = mc.thePlayer.getCurrentEquippedItem().getItem().coolDownTicks;

            if (raytraceProperty.getValue()) {
                if (isLookingAtEntity()) {
                    if(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword || mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemTool) {
                        if (mc.thePlayer.ticksSinceLastSwing >= tickCounter) {
                            mc.clickMouse();
                        }
                    } else {
                        mc.clickMouse();
                    }
                    this.hitTimeHelper.reset();
                } else return;
            } else {
                if(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword || mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemTool) {
                    if (mc.thePlayer.ticksSinceLastSwing >= tickCounter) {
                        mc.thePlayer.swingItem();
                        mc.playerController.attackEntity(mc.thePlayer, target);
                    }
                } else {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                }
                this.hitTimeHelper.reset();
            }

            if (keepSprintProperty.getValue()) {
                mc.thePlayer.setSprinting(MovementUtils.canSprint(Simp.INSTANCE.getModuleManager().getModule(SprintModule.class).omniProperty.getValue()));
            }

        }
        this.setRandomDelay();
        this.hitTimeHelper.reset();
    }

    public void autoBlock(PacketSendEvent event) {

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

        if (target == null && autoBlocking) {
            autoBlocking = false;
            mc.thePlayer.clearItemInUse();
        }

    }

    @Override
    public void onDisable() {
        autoBlocking = false;
        rotated = false;
        target = null;
    }
}