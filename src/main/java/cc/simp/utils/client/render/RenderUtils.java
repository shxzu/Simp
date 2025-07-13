package cc.simp.utils.client.render;

import cc.simp.utils.client.Util;
import cc.simp.utils.client.misc.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.client.render.GLUtils.color;
import static org.lwjgl.opengl.GL11.*;

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
        glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL_LINE_SMOOTH);
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
        glDisable(GL11.GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        GL11.glPopMatrix();
    }

    public static void drawCircle(double x, double y, double radius, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        GL11.glEnable(GL11.GL_BLEND);
        glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for (int i = 0; i <= 360; i++) {
            glVertex2d(x + Math.sin(i * Math.PI / 180) * radius, y + Math.cos(i * Math.PI / 180) * radius);
        }
        GL11.glEnd();
        glDisable(GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        glDisable(GL11.GL_BLEND);
    }

    public static void drawBorder(float x, float y, float width, float height, final float outlineThickness, int outlineColor) {
        // Enable line smoothing for anti-aliasing
        glEnable(GL_LINE_SMOOTH);
        // Set the line color (assuming 'color' utility correctly sets glColor4f)
        color(outlineColor);

        // Setup 2D rendering (e.g., orthographic projection, disable depth test)
        GLUtils.setup2DRendering();

        // Set the thickness of the line
        glLineWidth(outlineThickness);

        // Use GL_LINE_LOOP to draw a connected rectangle outline
        glBegin(GL_LINE_LOOP);
        // Top-left corner
        glVertex2d(x, y);
        // Top-right corner
        glVertex2d(x + width, y);
        // Bottom-right corner
        glVertex2d(x + width, y + height);
        // Bottom-left corner
        glVertex2d(x, y + height);
        glEnd();

        // End 2D rendering (restore previous OpenGL state)
        GLUtils.end2DRendering();

        // Disable line smoothing
        glDisable(GL_LINE_SMOOTH);
    }

    // This will set the alpha limit to a specified value ranging from 0-1
    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }

    // This method colors the next avalible texture with a specified alpha value ranging from 0-1
    public static void color(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, alpha);
    }

    // Colors the next texture without a specified alpha value
    public static void color(int color) {
        color(color, (float) (color >> 24 & 255) / 255.0F);
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
        glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float imgWidth, float imgHeight) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
    }

    public static float interpolate(float old,
                                    float now,
                                    float partialTicks) {

        return old + (now - old) * partialTicks;
    }

    public static int getRainbowFromEntity(long currentMillis, int speed, int offset, boolean invert, float alpha) {
        float time = ((currentMillis + (offset * 300L)) % speed) / (float) speed;
        int rainbow = Color.HSBtoRGB(invert ? 1.0F - time : time, 0.9F, 0.9F);
        int r = (rainbow >> 16) & 0xFF;
        int g = (rainbow >> 8) & 0xFF;
        int b = rainbow & 0xFF;
        int a = (int) (alpha * 255.0F);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static int fadeBetween(int startColor, int endColor, float progress) {
        if (progress > 1)
            progress = 1 - progress % 1;

        return fadeTo(startColor, endColor, progress);
    }

    public static int fadeBetween(int startColor, int endColor) {
        return fadeBetween(startColor, endColor, (System.currentTimeMillis() % 2000) / 1000.0F);
    }

    public static int fadeTo(int startColor, int endColor, float progress) {
        float invert = 1.0F - progress;
        int r = (int) ((startColor >> 16 & 0xFF) * invert +
                (endColor >> 16 & 0xFF) * progress);
        int g = (int) ((startColor >> 8 & 0xFF) * invert +
                (endColor >> 8 & 0xFF) * progress);
        int b = (int) ((startColor & 0xFF) * invert +
                (endColor & 0xFF) * progress);
        int a = (int) ((startColor >> 24 & 0xFF) * invert +
                (endColor >> 24 & 0xFF) * progress);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static void start3D() {
        GL11.glDisable((int)3553);
        GL11.glDisable((int)2929);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)3042);
        GL11.glDepthMask((boolean)false);
        GlStateManager.disableCull();
    }

    public static void stop3D() {
        GlStateManager.enableCull();
        GL11.glEnable((int)3553);
        GL11.glEnable((int)2929);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
    }

    public static void renderBoundingBox(AxisAlignedBB aabb, Color color, float alpha) {
        AxisAlignedBB bb = aabb;
        GlStateManager.pushMatrix();
        GLUtils.startBlend();
        GLUtils.setup2DRendering();
        GLUtils.enableCaps(GL_BLEND, GL_POINT_SMOOTH, GL_POLYGON_SMOOTH, GL_LINE_SMOOTH);

        glLineWidth(5);
        float actualAlpha = .3f * alpha;
        glColor4f(color.getRed(), color.getGreen(), color.getBlue(), actualAlpha);
        color(color.getRGB(), actualAlpha);
        RenderGlobal.renderCustomBoundingBox(bb, true, false);

        GLUtils.disableCaps();
        GLUtils.endBlend();
        GLUtils.end2DRendering();

        GlStateManager.popMatrix();
    }

    public static void renderFilledBoundingBox(AxisAlignedBB aabb, Color color, float alpha) {
        AxisAlignedBB bb = aabb;
        GlStateManager.pushMatrix();
        GLUtils.setup2DRendering();
        GLUtils.enableCaps(GL_BLEND, GL_POINT_SMOOTH, GL_POLYGON_SMOOTH, GL_LINE_SMOOTH);

        glLineWidth(5);
        float actualAlpha = .3f * alpha;
        glColor4f(color.getRed(), color.getGreen(), color.getBlue(), actualAlpha);
        color(color.getRGB(), actualAlpha);
        RenderGlobal.renderCustomBoundingBox(bb, true, true);

        GLUtils.disableCaps();
        GLUtils.end2DRendering();

        GlStateManager.popMatrix();
    }

}
