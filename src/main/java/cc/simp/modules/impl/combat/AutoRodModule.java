package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Rod", category = ModuleCategory.COMBAT)
public final class AutoRodModule extends Module {

    private final NumberProperty minRange = new NumberProperty("Min Range", 3.0, 1.0, 8.0, 0.1);
    private final NumberProperty maxRange = new NumberProperty("Max Range", 4.5, 1.0, 8.0, 0.1);
    private final NumberProperty maxDelay = new NumberProperty("Max Delay", 100.0, 0.0, 1000.0, 5.0);
    private final NumberProperty maxRecastDelay = new NumberProperty("Max Recast Delay", 100.0, 0.0, 1000.0, 5.0);
    private final NumberProperty fov = new NumberProperty("FOV", 90.0, 0.0, 360.0, 1.0);
    private final Property<Boolean> ka = new Property<>("Only On Kill Aura", false);
    private final Property<Boolean> rotate = new Property<>("Rotate", true);
    private final NumberProperty predictSize = new NumberProperty("Predict Size", 2, rotate::getValue, 0.1f, 10, 0.1f);
    private final Property<Boolean> players = new Property<>("Players", true);
    private final Property<Boolean> nonPlayers = new Property<>("Non Players", true);
    private final Property<Boolean> teams = new Property<>("Teams", true);
    private final Property<Boolean> invisibles = new Property<>("Invisibles", false);
    private final Property<Boolean> dead = new Property<>("Dead", false);

    private EntityLivingBase currentTarget;
    private boolean usingRod;
    private int oldSlot;
    private long lastUseTime;
    private long lastRecastTime;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {

        currentTarget = findTarget();

        if (currentTarget == null || !mc.thePlayer.canEntityBeSeen(currentTarget) || mc.thePlayer.isUsingItem()) {
            reset();
            return;
        }

        float range = mc.thePlayer.getDistanceToEntity(currentTarget);

        if (range >= minRange.getValue() && range <= maxRange.getValue()) {
            if (getRotationDifference(currentTarget) <= fov.getValue()) {
                if (!usingRod) {
                    if (System.currentTimeMillis() - lastUseTime >= maxDelay.getValue() || currentTarget.hurtTime <= 3) {
                        int rod = findRod();
                        if (rod != -1) {
                            useRod(rod);
                            usingRod = true;
                            lastRecastTime = System.currentTimeMillis();
                        }
                    }
                } else {
                    if (System.currentTimeMillis() - lastRecastTime >= maxRecastDelay.getValue() || currentTarget.hurtTime >= 9) {
                        reset();
                    }
                }
            }
        } else {
            reset();
        }

        if ((!Simp.INSTANCE.getModuleManager().getModule((KillAuraModule.class)).isEnabled() && ka.getValue())) {
            return;
        }

        if (rotate.getValue() && KillAuraModule.target == null && range > minRange.getValue() && range <= maxRange.getValue()) {
            float[] finalRotation = RotationUtils.faceTrajectory(currentTarget, true, predictSize.getValue().floatValue(), 0.03f, 2f);

            RotationProcess.setRotations(new Vector2f(finalRotation[0], finalRotation[1]), 10, MovementFix.NORMAL);
        }
    };

    private EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = maxRange.getValue() + 0.4;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distance = mc.thePlayer.getDistanceToEntity(entity);

            if (entity != mc.thePlayer && distance <= maxRange.getValue() && entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;

                if (living instanceof EntityAnimal || living instanceof EntityMob || living instanceof EntityVillager) {
                    if (!nonPlayers.getValue()) continue;
                } else if (living instanceof EntityPlayer) {
                    if (!players.getValue()) continue;
                    EntityPlayer player = (EntityPlayer) living;
                    if (!teams.getValue() && isOnSameTeam(player)) continue;
                }

                if (living.isInvisible() && !invisibles.getValue()) continue;
                if (living.isDead && !dead.getValue()) continue;

                if (distance < closestDistance) {
                    target = living;
                    closestDistance = distance;
                }
            }
        }

        return target;
    }

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

    private int findRod() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemFishingRod) {
                return i;
            }
        }
        return -1;
    }

    private void useRod(int rodSlot) {
        oldSlot = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = rodSlot;
        lastUseTime = System.currentTimeMillis();
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
    }

    private void reset() {
        if (oldSlot != -1) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            oldSlot = -1;
        }
        usingRod = false;
    }

    private boolean isOnSameTeam(EntityPlayer player) {
        if (mc.thePlayer.getTeam() != null && player.getTeam() != null) {
            return mc.thePlayer.getTeam().isSameTeam(player.getTeam());
        }
        return false;
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        usingRod = false;
        oldSlot = -1;
        lastUseTime = 0;
        lastRecastTime = 0;
    }

    @Override
    public void onDisable() {
        reset();
    }
}