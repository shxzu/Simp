package cc.simp.utils.render;

import cc.simp.utils.Util;
import cc.simp.utils.render.shaders.RoundedShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RenderUtils extends Util {

    public static RoundedShader roundedShader = new RoundedShader("roundedRect");
    public static RoundedShader roundedOutlineShader = new RoundedShader("roundRectOutline");
    private static final RoundedShader roundedTexturedShader = new RoundedShader("roundRectTexture");
    private static final RoundedShader roundedGradientShader = new RoundedShader("roundedRectGradient");

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, RoundedShader roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }


    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShader.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms((float) x, (float) y, (float) width, (float) height, (float) radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShader.drawQuads((float) x - 1, (float) y - 1, (float) width + 2,  (float) height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRectNoShaders(float x, float y, float width, float height, float radius, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_POLYGON);
        // Top-left corner
        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(x + radius + Math.sin(Math.toRadians(i)) * -radius, y + radius + Math.cos(Math.toRadians(i)) * -radius);
        }
        // Bottom-left corner
        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(x + radius + Math.sin(Math.toRadians(i)) * -radius, y + height - radius + Math.cos(Math.toRadians(i)) * -radius);
        }
        // Bottom-right corner
        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(x + width - radius + Math.sin(Math.toRadians(i)) * radius, y + height - radius + Math.cos(Math.toRadians(i)) * radius);
        }
        // Top-right corner
        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(x + width - radius + Math.sin(Math.toRadians(i)) * radius, y + radius + Math.cos(Math.toRadians(i)) * radius);
        }
        GL11.glEnd();

        GlStateManager.resetColor();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public enum ArrowDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        Pair<Color, Color> colors = Pair.of(new Color(228, 143, 255), new Color(255, 113, 82));
        GlStateManager.color(1, 1, 1, 1);
        GlUtils.color(RenderUtils.interpolateColorsBackAndForth(15, 15 * 5, colors.getLeft(), colors.getRight(), false).getRGB());
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        GlUtils.setup2DRendering(true);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x, y, 0.0D).color(color.getRGB()).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).color(color.getRGB()).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(color.getRGB()).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).color(color.getRGB()).endVertex();
        tessellator.draw();

        GlUtils.end2DRendering();
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

    public static void drawBorderedRect(float x, float y, float width, float height, final float outlineThickness, int rectColor, int outlineColor, boolean top, boolean right, boolean bottom, boolean left) {
        Gui.drawRect(x, y, width, height, rectColor);
        glEnable(GL_LINE_SMOOTH);
        color(outlineColor);

        GlUtils.setup2DRendering();

        glLineWidth(outlineThickness);
        float cornerValue = (float) (outlineThickness * .19);

        glBegin(GL_LINES);
        //left start
        glVertex2d(x, y);
        //left end
        glVertex2d(x, left ? y + height + cornerValue : y);
        //right start
        glVertex2d(x + width, y + height + cornerValue);
        //right end
        glVertex2d(x + width, right ? y - cornerValue : y + height + cornerValue);
        //top start
        glVertex2d(x, y);
        //top end
        glVertex2d(top ? x + width : x, y);
        //bottom start
        glVertex2d(x, y + height);
        //bottom end
        glVertex2d(bottom ? x + width : x, y + height);
        glEnd();

        GlUtils.end2DRendering();

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

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static Color astolfoColors(int yOffset, int yTotal) {
        float speed = 2900F;
        float hue = (float) (System.currentTimeMillis() % (int) speed) + ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5f);
        }
        hue += 0.5F;
        return new Color(Color.HSBtoRGB(hue, 0.5f, 1F));
    }

    public static Color rainbowColors(int yOffset, int yTotal) {
        float speed = 2900F;
        float hue = (float) (System.currentTimeMillis() % (int) speed) + ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5f);
        }
        hue += 0.5F;
        return new Color(Color.HSBtoRGB(hue, 0.9f, 1F));
    }

}