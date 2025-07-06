package cc.simp.utils.client.render;

import cc.simp.utils.client.Util;
import cc.simp.utils.client.misc.MathUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class RenderUtils extends Util {
    public static double progressiveAnimation(double now, double desired, double speed) {
        double dif = Math.abs(now - desired);

        final int fps = Minecraft.getDebugFPS();

        if (dif > 0) {
            double animationSpeed = MathUtils.roundToDecimalPlace(Math.min(
                    10.0D, Math.max(0.05D, (144.0D / fps) * (dif / 10) * speed)), 0.05D);

            if (dif != 0 && dif < animationSpeed)
                animationSpeed = dif;

            if (now < desired)
                return now + animationSpeed;
            else if (now > desired)
                return now - animationSpeed;
        }

        return now;
    }

    public enum ArrowDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public static void drawArrow(float x, float y, float size, ArrowDirection direction, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_TRIANGLES);
        switch (direction) {
            case UP:
                GL11.glVertex2f(x, y + size);
                GL11.glVertex2f(x + size, y + size);
                GL11.glVertex2f(x + size / 2, y);
                break;
            case DOWN:
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + size, y);
                GL11.glVertex2f(x + size / 2, y + size);
                break;
            // Additional cases for LEFT/RIGHT can be added if needed
        }
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glPopMatrix();
    }

    public static void drawCircle(double x, double y, double radius, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for (int i = 0; i <= 360; i++) {
            GL11.glVertex2d(x + Math.sin(i * Math.PI / 180) * radius, y + Math.cos(i * Math.PI / 180) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    // A simple lerp function for smooth animations
    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static void startScissor(float x, float y, float width, float height) {
        // Enable scissor test if not already enabled
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        // Get Minecraft's display dimensions
        Minecraft mc = Minecraft.getMinecraft();
        int scaleFactor = 1;
        try {
            scaleFactor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        } catch (Exception ignored) {}

        // Calculate scissor box coordinates (converting from GUI to screen coordinates)
        int scissorX = (int) (x * scaleFactor);
        int scissorY = (int) (mc.displayHeight - (y + height) * scaleFactor);
        int scissorWidth = (int) (width * scaleFactor);
        int scissorHeight = (int) (height * scaleFactor);

        // Set the scissor box
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    /**
     * Ends the scissor test
     */
    public static void endScissor() {
        // Disable scissor test
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

}
