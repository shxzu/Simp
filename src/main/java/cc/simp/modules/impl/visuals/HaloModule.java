package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.Util.mc;
import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(label = "Halo", category = ModuleCategory.VISUALS)
public class HaloModule extends Module {

    private final ModeProperty<Quality> quality = new ModeProperty<>("Quality", Quality.SMOOTH);
    private final Property<Boolean> showInFirstPerson = new Property<>("Show in First Person", false);
    private final Property<Boolean> rotate = new Property<>("Rotate", true);
    private final NumberProperty radius = new NumberProperty("Radius", 0.4, 0.2, 1.0, 0.05);
    private final NumberProperty height = new NumberProperty("Height", 0.5, 0.0, 1.5, 0.1);
    private final NumberProperty thickness = new NumberProperty("Thickness", 0.08, 0.02, 0.2, 0.01);
    private final NumberProperty tilt = new NumberProperty("Tilt", 15.0, 0.0, 45.0, 5.0);

    private enum Quality {
        LOW("Low", 32),
        NORMAL("Normal", 64),
        HIGH("High", 128),
        SMOOTH("Smooth", 256);

        public final String name;
        public final int segments;

        Quality(String name, int segments) {
            this.name = name;
            this.segments = segments;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.gameSettings.thirdPersonView == 0 && !showInFirstPerson.getValue()) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glDisable(GL_TEXTURE_2D);
        GL11.glEnable(GL_LINE_SMOOTH);
        GL11.glEnable(GL_BLEND);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glDisable(GL_DEPTH_TEST);

        float partialTicks = mc.timer.renderPartialTicks;

        final double x = mc.thePlayer.lastTickPosX +
                (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks -
                mc.getRenderManager().viewerPosX;
        final double y = (mc.thePlayer.lastTickPosY +
                (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks -
                mc.getRenderManager().viewerPosY) +
                mc.thePlayer.getEyeHeight() + height.getValue();
        final double z = mc.thePlayer.lastTickPosZ +
                (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks -
                mc.getRenderManager().viewerPosZ;

        final double adjustedY = mc.thePlayer.isSneaking() ? y - 0.2 : y;

        GL11.glTranslated(x, adjustedY, z);

        final double rotation = rotate.getValue() ? System.currentTimeMillis() / 50.0 : 0;
        GL11.glRotated(rotation, 0, 1, 0);

        final double tiltAngle = tilt.getValue();
        GL11.glRotated(tiltAngle, 1, 0, 0);

        Color color = ColorProcess.getColor();
        final int segments = quality.getValue().segments;
        final double rad = radius.getValue();
        final double thick = thickness.getValue();

        // Outer ring
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.8f);
            GL11.glVertex3d(rad * cos, 0, rad * sin);

            GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.6f);
            GL11.glVertex3d((rad - thick) * cos, 0, (rad - thick) * sin);
        }
        GL11.glEnd();

        // Inner glow
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.4f);
        GL11.glVertex3d(0, 0, 0);
        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.1f);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            GL11.glVertex3d((rad - thick) * Math.cos(angle), 0, (rad - thick) * Math.sin(angle));
        }
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL_DEPTH_TEST);
        GlStateManager.enableCull();
        GL11.glDisable(GL_LINE_SMOOTH);
        GL11.glEnable(GL_TEXTURE_2D);
        GL11.glPopMatrix();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    };

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
