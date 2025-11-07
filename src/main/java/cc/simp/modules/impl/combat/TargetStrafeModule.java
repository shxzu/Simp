package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.JumpEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.FlightModule;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PlayerUtils;
import cc.simp.utils.mc.RotationUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vector3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Target Strafe", category = ModuleCategory.COMBAT)
public final class TargetStrafeModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 3.0, 0.5, 6.0, 0.1);
    private final NumberProperty speed = new NumberProperty("Strafe Speed", 1.0, 0.1, 2.0, 0.05);
    private final Property<Boolean> holdJump = new Property<>("Hold Jump", false);
    private final Property<Boolean> strictMode = new Property<>("Strict Mode", false);
    private final Property<Boolean> adaptiveRange = new Property<>("Adaptive Range", true);
    private final Property<Boolean> autoDirection = new Property<>("Auto Direction", true);
    private final Property<Boolean> renderPath = new Property<>("Render Path", true);
    private final NumberProperty pathPoints = new NumberProperty("Path Points", 32, () -> renderPath.getValue(), 8, 64, 4);
    private final Property<Boolean> renderTarget = new Property<>("Render Target", true);

    private float yaw;
    private Entity target;
    private boolean left, colliding;
    private boolean active;
    private final List<Vector3d> pathTrail = new ArrayList<>();
    private int directionSwitchTicks = 0;

    @EventLink(value = Priorities.HIGH)
    public final Listener<JumpEvent> onJump = event -> {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    };

    @EventLink(value = Priorities.HIGH)
    public final Listener<StrafeEvent> onStrafe = event -> {
        if (target != null && active) {
            event.setYaw(yaw);

            if (strictMode.getValue()) {
                event.setForward(1.0F);
                event.setStrafe(0.0F);
            }
        }
    };

    @EventLink(value = Priorities.HIGH)
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        ScaffoldModule scaffold = Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        KillAuraModule killaura = Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);

        if (scaffold != null && scaffold.isEnabled()) {
            active = false;
            target = null;
            return;
        }

        if (killaura == null || !killaura.isEnabled()) {
            active = false;
            target = null;
            return;
        }

        SpeedModule speedModule = Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class);
        FlightModule flight = Simp.INSTANCE.getModuleManager().getModule(FlightModule.class);

        boolean canStrafe = (flight != null && flight.isEnabled()) || (speedModule != null && speedModule.isEnabled());

        if (!strictMode.getValue()) {
            if (holdJump.getValue() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                active = false;
                target = null;
                return;
            }

            if (!canStrafe || !mc.gameSettings.keyBindForward.isKeyDown()) {
                active = false;
                target = null;
                return;
            }
        }

        final List<Entity> targets = killaura.targetList;

        if (targets.isEmpty()) {
            active = false;
            target = null;
            return;
        }

        active = true;
        target = targets.get(0);

        if (target == null) {
            return;
        }

        directionSwitchTicks++;

        boolean shouldSwitchDirection = false;

        if (mc.thePlayer.isCollidedHorizontally) {
            shouldSwitchDirection = true;
        }

        if (!strictMode.getValue() && !PlayerUtils.isBlockUnder(5, false)) {
            shouldSwitchDirection = true;
        }

        if (autoDirection.getValue() && directionSwitchTicks >= 40) {
            double currentDist = mc.thePlayer.getDistanceToEntity(target);
            if (currentDist < range.getValue() - 0.5) {
                shouldSwitchDirection = true;
                directionSwitchTicks = 0;
            }
        }

        if (shouldSwitchDirection && !colliding) {
            left = !left;
            colliding = true;
        } else if (!shouldSwitchDirection) {
            colliding = false;
        }

        double effectiveRange = range.getValue();

        if (adaptiveRange.getValue()) {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            effectiveRange = MathHelper.clamp_double(distance, range.getValue() * 0.5, range.getValue());
        }

        float baseYaw = RotationUtils.calculate(target).getX();
        float offset = (90 + 45) * (left ? -1 : 1);
        yaw = baseYaw + offset;

        final double strafeRange = effectiveRange + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * strafeRange + target.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * strafeRange + target.posZ;

        yaw = RotationUtils.calculate(new Vector3d(posX, target.posY, posZ)).getX();

        this.yaw = yaw;
        mc.thePlayer.movementYaw = this.yaw;

        if (strictMode.getValue()) {
            double currentSpeed = speed.getValue() * MovementUtils.getBaseMoveSpeed();
            MovementUtils.setSpeed(currentSpeed, this.yaw, 0, 1);
        }

        updatePathTrail();
    };

    @EventLink
    public final Listener<Render3DEvent> onRender3D = event -> {
        if (!active || target == null) return;

        if (renderPath.getValue()) {
            renderStrafeCircle();
        }

        if (renderTarget.getValue()) {
            renderTargetIndicator();
        }
    };

    private void updatePathTrail() {
        if (!renderPath.getValue() || target == null) return;

        pathTrail.clear();
        int points = pathPoints.getValue().intValue();

        for (int i = 0; i < points; i++) {
            float angle = (float) (2 * Math.PI * i / points);
            float yawAngle = RotationUtils.calculate(target).getX() + (float) Math.toDegrees(angle);

            double x = -MathHelper.sin((float) Math.toRadians(yawAngle)) * range.getValue() + target.posX;
            double z = MathHelper.cos((float) Math.toRadians(yawAngle)) * range.getValue() + target.posZ;

            pathTrail.add(new Vector3d(x, target.posY, z));
        }
    }

    private void renderStrafeCircle() {
        if (pathTrail.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();

        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (Vector3d point : pathTrail) {
            double x = point.getX() - mc.getRenderManager().viewerPosX;
            double y = point.getY() - mc.getRenderManager().viewerPosY;
            double z = point.getZ() - mc.getRenderManager().viewerPosZ;

            Color color = new Color(0, 255, 255, 150);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            GL11.glVertex3d(x, y, z);
        }

        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderTargetIndicator() {
        double x = target.posX - mc.getRenderManager().viewerPosX;
        double y = target.posY - mc.getRenderManager().viewerPosY;
        double z = target.posZ - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + target.height + 0.5, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();

        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
        GL11.glVertex3d(-0.2, 0, 0);
        GL11.glVertex3d(0.2, 0, 0);
        GL11.glVertex3d(0, -0.2, 0);
        GL11.glVertex3d(0, 0.2, 0);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public void onDisable() {
        active = false;
        target = null;
        pathTrail.clear();
        directionSwitchTicks = 0;
    }
}
