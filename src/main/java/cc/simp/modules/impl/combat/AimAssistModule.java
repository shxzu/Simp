package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.misc.MovementFix;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Aim Assist", category = ModuleCategory.COMBAT)
public final class AimAssistModule extends Module {

    private final NumberProperty searchRange = new NumberProperty("Search Range", 4.0, 1.0, 8.0, 0.1);
    private final Property<Boolean> onlyOnClick = new Property<>("Only On Click", true);
    private final NumberProperty resetTime = new NumberProperty("Reset Time", 500.0, () -> onlyOnClick.getValue(), 0.0, 1000.0, 1.0);
    private final Property<Boolean> teamCheck = new Property<>("Team Check", false);
    private final Property<Boolean> targetESP = new Property<>("Target ESP", false);

    private EntityLivingBase target;
    private boolean angleCalled;
    private long lastClickTime;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
            angleCalled = true;

            if (onlyOnClick.getValue() && Mouse.isButtonDown(0) && angleCalled) {
                lastClickTime = System.currentTimeMillis();
            }

            if (!onlyOnClick.getValue() || System.currentTimeMillis() - lastClickTime <= resetTime.getValue()) {
                target = getTarget(searchRange.getValue().floatValue(), teamCheck.getValue());
            } else {
                target = null;
            }

            if (target == null) {
                return;
            }

            if (angleCalled && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                float[] rotations = getRotationsToEntity(target);
                RotationProcess.setRotations(new Vector2f(rotations[0], rotations[1]), 5, MovementFix.NORMAL);
            }

            angleCalled = false;
    };

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        if (target != null && targetESP.getValue()) {
            double x = target.posX - mc.getRenderManager().renderPosX;
            double y = target.posY - mc.getRenderManager().renderPosY + target.height;
            double z = target.posZ - mc.getRenderManager().renderPosZ;
            RenderUtils.drawCircle(x, y, 0.5, Color.RED.getRGB());
        }
    };

    private EntityLivingBase getTarget(float range, boolean teamCheck) {
        EntityLivingBase currentTarget = null;
        double closestDistance = range;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer && !entity.isDead) {
                EntityLivingBase living = (EntityLivingBase) entity;

                if (living instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) living;
                    if (teamCheck && isOnSameTeam(player)) {
                        continue;
                    }
                }

                double distance = mc.thePlayer.getDistanceToEntity(living);
                if (distance <= closestDistance && mc.thePlayer.canEntityBeSeen(living)) {
                    closestDistance = distance;
                    currentTarget = living;
                }
            }
        }

        return currentTarget;
    }

    private boolean isOnSameTeam(EntityPlayer player) {
        if (mc.thePlayer.getTeam() != null && player.getTeam() != null) {
            return mc.thePlayer.getTeam().isSameTeam(player.getTeam());
        }
        return false;
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

    @Override
    public void onEnable() {
        target = null;
        angleCalled = false;
        lastClickTime = 0;
    }
}