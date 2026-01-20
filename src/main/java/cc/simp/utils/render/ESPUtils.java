package cc.simp.utils.render;

import cc.simp.utils.Util;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;

public class ESPUtils extends Util {

    public static final FloatBuffer windPos = GLAllocation.createDirectFloatBuffer(4);

    public static void draw3DRect(final float x1, final float y1, final float x2, final float y2) {
        GL11.glBegin(7);
        GL11.glVertex2d(x2, y1);
        GL11.glVertex2d(x1, y1);
        GL11.glVertex2d(x1, y2);
        GL11.glVertex2d(x2, y2);
        GL11.glEnd();
    }
    public static void drawCornerESP(final Entity entity, final float red, final float green, final float blue) {
        final float x = (float)(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosX());
        final float y = (float)(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosY());
        final float z = (float)(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0f, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.scale(-0.098, -0.098, 0.098);
        final float width = (float)(26.6 * entity.width / 2.0);
        final float height = (entity instanceof EntityPlayer) ? 12.0f : ((float)(11.98 * (entity.height / 2.0f)));
        GlStateManager.color(red, green, blue);
        draw3DRect(width, height - 1.0f, width - 4.0f, height);
        draw3DRect(-width, height - 1.0f, -width + 4.0f, height);
        draw3DRect(-width, height, -width + 1.0f, height - 4.0f);
        draw3DRect(width, height, width - 1.0f, height - 4.0f);
        draw3DRect(width, -height, width - 4.0f, -height + 1.0f);
        draw3DRect(-width, -height, -width + 4.0f, -height + 1.0f);
        draw3DRect(-width, -height + 1.0f, -width + 1.0f, -height + 4.0f);
        draw3DRect(width, -height + 1.0f, width - 1.0f, -height + 4.0f);
        GlStateManager.color(0.0f, 0.0f, 0.0f);
        draw3DRect(width, height, width - 4.0f, height + 0.2f);
        draw3DRect(-width, height, -width + 4.0f, height + 0.2f);
        draw3DRect(-width - 0.2f, height + 0.2f, -width, height - 4.0f);
        draw3DRect(width + 0.2f, height + 0.2f, width, height - 4.0f);
        draw3DRect(width + 0.2f, -height, width - 4.0f, -height - 0.2f);
        draw3DRect(-width - 0.2f, -height, -width + 4.0f, -height - 0.2f);
        draw3DRect(-width - 0.2f, -height, -width, -height + 4.0f);
        draw3DRect(width + 0.2f, -height, width, -height + 4.0f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public static void drawEntityESP(final Entity entity, final float red, final float green, final float blue, final float alpha, final float lineAlpha, final float lineWidth) {
        final float x = (float)(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosX());
        final float y = (float)(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosY());
        final float z = (float)(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosZ());
        GL11.glColor4f(red, green, blue, alpha);
        otherDrawBoundingBox(entity, x, y, z, entity.width - 0.2f, entity.height + 0.1f);
        if (lineWidth > 0.0f) {
            GL11.glLineWidth(lineWidth);
            GL11.glColor4f(red, green, blue, lineAlpha);
            otherDrawOutlinedBoundingBox(entity, x, y, z, entity.width - 0.2f, entity.height + 0.1f);
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawOutlineEntityESP(final Entity entity, final float red, final float green, final float blue, final float alpha, final float lineAlpha, final float lineWidth) {
        final float x = (float)(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosX());
        final float y = (float)(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosY());
        final float z = (float)(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosZ());
        GL11.glColor4f(red, green, blue, alpha);
        if (lineWidth > 0.0f) {
            GL11.glLineWidth(lineWidth);
            GL11.glColor4f(red, green, blue, lineAlpha);
            otherDrawOutlinedBoundingBox(entity, x, y, z, entity.width - 0.2f, entity.height + 0.1f);
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawFake2DESP(final Entity entity, final float red, final float green, final float blue) {
        final float x = (float)(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosX());
        final float y = (float)(entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosY());
        final float z = (float)(entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0f, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.scale(-0.1, -0.1, 0.1);
        GlStateManager.color(red, green, blue);
        final float width = (float)(23.3 * entity.width / 2.0);
        final float height = (entity instanceof EntityPlayer) ? 12.0f : ((float)(11.98 * (entity.height / 2.0f)));
        draw3DRect(width, height, -width, height + 0.4f);
        draw3DRect(width, -height, -width, -height + 0.4f);
        draw3DRect(width, -height + 0.4f, width - 0.4f, height + 0.4f);
        draw3DRect(-width, -height + 0.4f, -width + 0.4f, height + 0.4f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public static void render2DESP(AxisAlignedBB axisAlignedBB, Color color, float lineWidth) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        ESPUtils.windPos.clear();

        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                for (int y = 0; y < 2; y++) {
                    if (GLU.gluProject((float) (x == 1 ? axisAlignedBB.minX : axisAlignedBB.maxX),
                            (float) (y == 1 ? axisAlignedBB.minY : axisAlignedBB.maxY),
                            (float) (z == 1 ? axisAlignedBB.minZ : axisAlignedBB.maxZ),
                            ActiveRenderInfo.MODELVIEW,
                            ActiveRenderInfo.PROJECTION,
                            ActiveRenderInfo.VIEWPORT,
                            ESPUtils.windPos)) {
                        if (ESPUtils.windPos.get(2) > 1) {
                            continue;
                        }

                        double screenX = (ESPUtils.windPos.get(0) / scaledResolution.getScaleFactor());
                        double screenY = (ESPUtils.windPos.get(1) / scaledResolution.getScaleFactor());

                        minX = Math.min(screenX, minX);
                        minY = Math.min(screenY, minY);
                        maxX = Math.max(screenX, maxX);
                        maxY = Math.max(screenY, maxY);
                    }
                }
            }
        }

        if (minX != Double.MAX_VALUE) {
            minX = Math.max(0, minX);
            minY = Math.max(0, minY);
            maxX = Math.min(scaledResolution.getScaledWidth(), maxX);
            maxY = Math.min(scaledResolution.getScaledHeight(), maxY);

            double margin = 3; // Adjust margin as needed
            minX -= margin;
            minY -= margin;
            maxX += margin;
            maxY += margin;

            minY = scaledResolution.getScaledHeight() - minY;
            maxY = scaledResolution.getScaledHeight() - maxY;

            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            // Draw the filled rectangle with shading
            GL11.glColor4d(0, 0, 0, 0.2); // Dark color with transparency
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(minX, minY);
            GL11.glVertex2d(maxX, minY);
            GL11.glVertex2d(maxX, maxY);
            GL11.glVertex2d(minX, maxY);
            GL11.glEnd();

            // Draw the rectangle outline
            GL11.glColor4d(color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d,
                    color.getAlpha() / 255d);
            GL11.glLineWidth(lineWidth);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2d(minX, minY);
            GL11.glVertex2d(minX, maxY);
            GL11.glVertex2d(maxX, maxY);
            GL11.glVertex2d(maxX, minY);
            GL11.glEnd();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
    }


    public static double interpolate(double lastPos, double pos) {
        return lastPos + (pos - lastPos) * mc.timer.renderPartialTicks;
    }

    public static void otherDrawOutlinedBoundingBox(final Entity entity, final float x, final float y, final float z, double width, final double height) {
        width *= 1.5;
        final float yaw1 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 45.0f;
        float newYaw1;
        if (yaw1 < 0.0f) {
            newYaw1 = 0.0f;
            newYaw1 += 360.0f - Math.abs(yaw1);
        }
        else {
            newYaw1 = yaw1;
        }
        newYaw1 *= -1.0f;
        newYaw1 *= (float)0.017453292519943295;
        final float yaw2 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 135.0f;
        float newYaw2;
        if (yaw2 < 0.0f) {
            newYaw2 = 0.0f;
            newYaw2 += 360.0f - Math.abs(yaw2);
        }
        else {
            newYaw2 = yaw2;
        }
        newYaw2 *= -1.0f;
        newYaw2 *= (float)0.017453292519943295;
        final float yaw3 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 225.0f;
        float newYaw3;
        if (yaw3 < 0.0f) {
            newYaw3 = 0.0f;
            newYaw3 += 360.0f - Math.abs(yaw3);
        }
        else {
            newYaw3 = yaw3;
        }
        newYaw3 *= -1.0f;
        newYaw3 *= (float)0.017453292519943295;
        final float yaw4 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 315.0f;
        float newYaw4;
        if (yaw4 < 0.0f) {
            newYaw4 = 0.0f;
            newYaw4 += 360.0f - Math.abs(yaw4);
        }
        else {
            newYaw4 = yaw4;
        }
        newYaw4 *= -1.0f;
        newYaw4 *= (float)0.017453292519943295;
        final float x2 = (float)(Math.sin(newYaw1) * width + x);
        final float z2 = (float)(Math.cos(newYaw1) * width + z);
        final float x3 = (float)(Math.sin(newYaw2) * width + x);
        final float z3 = (float)(Math.cos(newYaw2) * width + z);
        final float x4 = (float)(Math.sin(newYaw3) * width + x);
        final float z4 = (float)(Math.cos(newYaw3) * width + z);
        final float x5 = (float)(Math.sin(newYaw4) * width + x);
        final float z5 = (float)(Math.cos(newYaw4) * width + z);
        final float y2 = (float)(y + height);
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }

    public static void otherDrawBoundingBox(final Entity entity, final float x, final float y, final float z, double width, final double height) {
        width *= 1.5;
        final float yaw1 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 45.0f;
        float newYaw1;
        if (yaw1 < 0.0f) {
            newYaw1 = 0.0f;
            newYaw1 += 360.0f - Math.abs(yaw1);
        }
        else {
            newYaw1 = yaw1;
        }
        newYaw1 *= -1.0f;
        newYaw1 *= (float)0.017453292519943295;
        final float yaw2 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 135.0f;
        float newYaw2;
        if (yaw2 < 0.0f) {
            newYaw2 = 0.0f;
            newYaw2 += 360.0f - Math.abs(yaw2);
        }
        else {
            newYaw2 = yaw2;
        }
        newYaw2 *= -1.0f;
        newYaw2 *= (float)0.017453292519943295;
        final float yaw3 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 225.0f;
        float newYaw3;
        if (yaw3 < 0.0f) {
            newYaw3 = 0.0f;
            newYaw3 += 360.0f - Math.abs(yaw3);
        }
        else {
            newYaw3 = yaw3;
        }
        newYaw3 *= -1.0f;
        newYaw3 *= (float)0.017453292519943295;
        final float yaw4 = MathHelper.wrapAngleTo180_float(entity.getRotationYawHead()) + 315.0f;
        float newYaw4;
        if (yaw4 < 0.0f) {
            newYaw4 = 0.0f;
            newYaw4 += 360.0f - Math.abs(yaw4);
        }
        else {
            newYaw4 = yaw4;
        }
        newYaw4 *= -1.0f;
        newYaw4 *= (float)0.017453292519943295;
        final float x2 = (float)(Math.sin(newYaw1) * width + x);
        final float z2 = (float)(Math.cos(newYaw1) * width + z);
        final float x3 = (float)(Math.sin(newYaw2) * width + x);
        final float z3 = (float)(Math.cos(newYaw2) * width + z);
        final float x4 = (float)(Math.sin(newYaw3) * width + x);
        final float z4 = (float)(Math.cos(newYaw3) * width + z);
        final float x5 = (float)(Math.sin(newYaw4) * width + x);
        final float z5 = (float)(Math.cos(newYaw4) * width + z);
        final float y2 = (float)(y + height);
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x5, y, z5).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x5, y2, z5).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }


}
