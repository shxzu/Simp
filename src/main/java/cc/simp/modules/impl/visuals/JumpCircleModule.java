package cc.simp.modules.impl.visuals;


import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.render.animations.Animation;
import cc.simp.utils.render.animations.Easing;
import cc.simp.utils.render.animations.OtherStupidAnimation;
import cc.simp.utils.render.animations.impl.EaseInOutQuad;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Jump Circle", category = ModuleCategory.VISUALS)
public final class JumpCircleModule extends Module {

    private final Queue<Circle> circles = new ConcurrentLinkedQueue<>();

    private final OtherStupidAnimation alphaAnimation = new OtherStupidAnimation(Easing.EASE_IN_OUT_CUBIC, 300);

    private boolean playerWasInAir = false;

    private static final float ADJUSTMENT = 0.004F;

    private static final int LINE_WIDTH = 2;

    private static final float RADIUS = 2f;

    private static final float DUAL_RADIUS = RADIUS * 2;

    @Override
    public void onDisable() {
        if (this.circles.isEmpty()) {
            return;
        }

        this.circles.clear();
    }

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        if (MovementUtils.isOnGround() && this.playerWasInAir) {
            final double lerpedX = MathUtils.lerp(mc.thePlayer.prevPosX, mc.thePlayer.posX, mc.timer.renderPartialTicks);
            final double lerpedY = MathUtils.lerp(mc.thePlayer.prevPosY, mc.thePlayer.posY, mc.timer.renderPartialTicks);
            final double lerpedZ = MathUtils.lerp(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, mc.timer.renderPartialTicks);

            circles.add(new Circle(new Vec3(lerpedX, lerpedY, lerpedZ), 0, 255));
            this.playerWasInAir = false;
        } else if (!MovementUtils.isOnGround()) {
            this.playerWasInAir = true;
        }

        for (final Circle circle : this.circles) {
            if (circle.getAlpha() <= 0f) {
                this.circles.remove(circle);
            }
        }
    };

    @EventLink
    private final Listener<Render3DEvent> render3DEventListener = this::onRender3DEvent;

    private void onRender3DEvent(final Render3DEvent render3DEvent) {
        for (final Circle circle : this.circles) {
            final Vec3 pos = circle.getPosition();
            final double y = pos.yCoord - mc.getRenderManager().renderPosY;

            circle.adjustRadius(ADJUSTMENT);

            if (circle.getRadius() <= RADIUS) {
                this.setupGLForCircleRendering();

                this.alphaAnimation.run(circle.alpha -= (float) (circle.getRadius() / DUAL_RADIUS));

                if (circle.getAlpha() > 0) {
                    circle.setAlpha(circle.alpha -= (float) (circle.getRadius() / DUAL_RADIUS));
                }

                this.renderCircleOutline(circle, pos, y);

                this.restoreGLState();
            } else {
                this.circles.remove();
            }
        }
    }

    private void setupGLForCircleRendering() {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderCircleOutline(final Circle circle, final Vec3 pos, final double y) {
        GL11.glLineWidth(LINE_WIDTH);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i <= 360; i++) {

            final Color outerColor = ColorProcess.getColor();

            final double[] outerAnglePosition = this.getPosition(pos.xCoord, pos.zCoord, i, circle.radius);

            final double x = outerAnglePosition[0] - mc.getRenderManager().renderPosX;
            final double z = outerAnglePosition[1] - mc.getRenderManager().renderPosZ;

            GL11.glColor4f(outerColor.getRed() / 255F, outerColor.getGreen() / 255F, outerColor.getBlue() / 255F, circle.getAlpha() / 255f);
            GL11.glVertex3d(x, y, z);
        }

        GL11.glEnd();
        GL11.glPopMatrix();

    }

    private void restoreGLState() {
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public double[] getPosition(final double x, final double z, double angle, final double radius) {
        angle = MathHelper.wrapAngleTo180_double(angle);

        final double math = angle * Math.PI / 180;
        final double newX = x - Math.sin(math) * radius;
        final double newZ = z + Math.cos(math) * radius;

        return new double[] {newX, newZ};
    }

    private static final class Circle {
        private final Vec3 position;
        private double radius;
        private float alpha;

        private Circle(final Vec3 position, final double radius, final float alpha) {
            this.position = position;
            this.radius = radius;
            this.alpha = alpha;
        }

        public void adjustRadius(final double adjustment) {
            this.radius += adjustment;
        }

        public Vec3 getPosition() {
            return this.position;
        }

        public double getRadius() {
            return this.radius;
        }

        public float getAlpha() {
            return this.alpha;
        }

        public void setAlpha(final float alpha) {
            this.alpha = alpha;
        }
    }
}
