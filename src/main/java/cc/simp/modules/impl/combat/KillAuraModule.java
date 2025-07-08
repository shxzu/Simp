package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SprintModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.RotationUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MathHelper;

import java.util.Comparator;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    private final Property<Boolean> raytraceProperty = new Property<>("Raytrace", false);
    private final Property<Boolean> keepSprintProperty = new Property<>("KeepSprint", false);
    public static DoubleProperty rangeProperty = new DoubleProperty("Range", 3, 3, 6, 0.1);
    private final EnumProperty<TargetType> targetTypeProperty = new EnumProperty<>("Target", TargetType.PLAYERS);
    private final DoubleProperty attackDelayProperty = new DoubleProperty("Attack Delay", 100, 0, 1000, 10);

    public enum TargetType {
        PLAYERS,
        MOBS,
        ANIMALS,
        ALL
    }

    public KillAuraModule() {
        setSuffixListener(targetTypeProperty);
    }
    
    public static EntityLivingBase target;
    private long lastAttackTime;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        updateTarget();

        if (!targetIsValid()) {
            target = null;
            return;
        }

        float[] rotations = getRotations();
        event.setYaw(rotations[0]);
        event.setPitch(rotations[1]);
        RotationUtils.serverYaw = event.getYaw();
        RotationUtils.serverPitch = event.getPitch();

        if (event.isPre()) {
            if (raytraceProperty.getValue()) {
                MovingObjectPosition trace = mc.objectMouseOver;
                if (trace == null || trace.entityHit != target) return;
            }

            long currentTime = System.nanoTime();
            if (currentTime - lastAttackTime >= attackDelayProperty.getValue() * 1_000_000) {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);

                if (keepSprintProperty.getValue()) {
                    mc.thePlayer.setSprinting(MovementUtils.canSprint(Simp.INSTANCE.getModuleManager().getModule(SprintModule.class).omniProperty.getValue()));
                } else {
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.motionX *= 0.6D;
                        mc.thePlayer.motionZ *= 0.6D;
                        mc.thePlayer.setSprinting(false);
                    }
                }

                lastAttackTime = currentTime;
            }
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

    private void handleSprinting(float yaw) {
        double x = -MathHelper.sin(yaw * 0.017453292F);
        double z = MathHelper.cos(yaw * 0.017453292F);
        boolean behind = target.posX * x + target.posZ * z < 0.0D;
        mc.thePlayer.setSprinting(!behind && MovementUtils.canSprint(Simp.INSTANCE.getModuleManager().getModule(SprintModule.class).omniProperty.getValue()));
    }

    @Override
    public void onDisable() {
        target = null;
    }
}