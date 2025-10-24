package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.Util.mc;
import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(label = "China Hat", category = ModuleCategory.VISUALS)
public final class ChinaHatModule extends Module {

    private final ModeProperty<Quality> quality = new ModeProperty<>("Quality", Quality.SMOOTH);
    private final Property<Boolean> showInFirstPerson = new Property<>("Show in First Person", false);
    private final Property<Boolean> rotate = new Property<>("Rotate", false);
    private final NumberProperty radius = new NumberProperty("Radius", 0.65, 0.3, 1.5, 0.05);
    private final NumberProperty height = new NumberProperty("Height", 0.5, 0.2, 1.5, 0.1);

    private enum Quality {
        UMBRELLA("Umbrella", 16),
        VERY_LOW("Very Low", 32),
        LOW("Low", 64),
        NORMAL("Normal", 128),
        HIGH("High", 256),
        VERY_HIGH("Very High", 512),
        SMOOTH("Smooth", 1024);

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
        GL11.glEnable(GL_POINT_SMOOTH);
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

        Color color = getChinaHatColor();

        final double rotation = rotate.getValue() ?
                ((mc.thePlayer.prevRenderYawOffset +
                        (mc.thePlayer.renderYawOffset - mc.thePlayer.prevRenderYawOffset) * partialTicks) / 60) + 20 : 0;

        final int segments = quality.getValue().segments;
        final double rad = radius.getValue();

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;

            final double outerX = x + rad * Math.cos(angle + rotation);
            final double outerZ = z + rad * Math.sin(angle + rotation);

            GL11.glColor4f(
                    color.getRed() / 255.0f,
                    color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f,
                    0.3f
            );
            GL11.glVertex3d(outerX, adjustedY - 0.25, outerZ);

            GL11.glColor4f(
                    color.getRed() / 255.0f,
                    color.getGreen() / 255.0f,
                    color.getBlue() / 255.0f,
                    0.8f
            );
            GL11.glVertex3d(x, adjustedY, z);
        }

        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL_DEPTH_TEST);
        GlStateManager.enableCull();
        GL11.glDisable(GL_LINE_SMOOTH);
        GL11.glDisable(GL_POINT_SMOOTH);
        GL11.glEnable(GL_TEXTURE_2D);
        GL11.glPopMatrix();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    };

    private Color getChinaHatColor() {
        long time = System.currentTimeMillis();
        float hue = (time % 10000) / 10000.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}