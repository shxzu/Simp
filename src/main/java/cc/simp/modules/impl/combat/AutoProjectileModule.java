package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Projectile", category = ModuleCategory.COMBAT)
public final class AutoProjectileModule extends Module {

    public static ModeProperty<TargetSelectionProcess.Entities> entities = new ModeProperty<>("Entities", TargetSelectionProcess.Entities.Optimal);
    private final NumberProperty minRange = new NumberProperty("Min Range", 3.0, 1.0, 8.0, 0.1);
    private final NumberProperty maxRange = new NumberProperty("Max Range", 4.5, 1.0, 8.0, 0.1);
    private final NumberProperty maxDelay = new NumberProperty("Max Delay", 100.0, 0.0, 1000.0, 5.0);
    private final NumberProperty fov = new NumberProperty("FOV", 90.0, 0.0, 360.0, 1.0);
    private final Property<Boolean> ka = new Property<>("Only On Kill Aura", false);
    private final Property<Boolean> rotate = new Property<>("Rotate", true);
    private final NumberProperty predictSize = new NumberProperty("Predict Size", 2, rotate::getValue, 0.1f, 10, 0.1f);

    private EntityLivingBase currentTarget;
    private int oldSlot;
    private long lastThrowTime;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {

        if ((!Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && ka.getValue())) {
            return;
        }

        currentTarget = TargetSelectionProcess.getTarget();
        TargetSelectionProcess.setEntities(entities.getValue());

        if (currentTarget == null || !mc.thePlayer.canEntityBeSeen(currentTarget) || mc.thePlayer.isUsingItem()) {
            reset();
            return;
        }

        float range = mc.thePlayer.getDistanceToEntity(currentTarget);

        if (range >= minRange.getValue() && range <= maxRange.getValue()) {
            if (getRotationDifference(currentTarget) <= fov.getValue()) {
                if (System.currentTimeMillis() - lastThrowTime >= maxDelay.getValue() || currentTarget.hurtTime <= 3) {
                    int projectile = findProjectile();
                    if (projectile != -1) {
                        throwProjectile(projectile);
                    }
                }
            }
        } else {
            reset();
        }

        if (rotate.getValue() && TargetSelectionProcess.getTarget() == null && range > minRange.getValue() && range <= maxRange.getValue()) {
            float[] finalRotation = RotationUtils.faceTrajectory(currentTarget, true, predictSize.getValue().floatValue(), 0.03f, 2f);

            RotationProcess.setRotations(new Vector2f(finalRotation[0], finalRotation[1]), 10, MovementFix.NORMAL);
        }
    };

    private float getRotationDifference(EntityLivingBase entity) {
        float[] rotations = getRotationsToEntity(entity);
        float yawDiff = Math.abs(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = Math.abs(rotations[1] - mc.thePlayer.rotationPitch);
        return Math.max(yawDiff, pitchDiff);
    }

    private float[] getRotationsToEntity(EntityLivingBase entity) {
        double x = entity.posX - mc.thePlayer.posX;
        double y = entity.posY + entity.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = entity.posZ - mc.thePlayer.posZ;

        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / Math.PI));

        return new float[]{yaw, pitch};
    }

    private int findProjectile() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && (stack.getItem() instanceof ItemEgg || stack.getItem() instanceof ItemSnowball)) {
                return i;
            }
        }
        return -1;
    }

    private void throwProjectile(int projectileSlot) {
        oldSlot = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = projectileSlot;
        lastThrowTime = System.currentTimeMillis();
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
        mc.thePlayer.inventory.currentItem = oldSlot;
        oldSlot = -1;
    }

    private void reset() {
        if (oldSlot != -1) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            oldSlot = -1;
        }
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        oldSlot = -1;
        lastThrowTime = 0;
    }

    @Override
    public void onDisable() {
        reset();
    }
}