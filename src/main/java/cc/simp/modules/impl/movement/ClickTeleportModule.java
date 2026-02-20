package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.MiddleClickEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Click Teleport", category = ModuleCategory.MOVEMENT)
public final class ClickTeleportModule extends Module {

    private MovingObjectPosition targetBlock = null;
    private final List<Vec3> pathPositions = new ArrayList<>();

    @EventLink
    public final Listener<MiddleClickEvent> onMiddleClick = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Raycast to find the block at any range
        MovingObjectPosition result = rayTraceBlock(1000);

        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // Check if the block is not air
            if (mc.theWorld.getBlockState(result.getBlockPos()).getBlock().getMaterial() != Material.air) {
                // Teleport to the block position
                double x = result.getBlockPos().getX() + 0.5;
                double y = result.getBlockPos().getY() + 1.0;
                double z = result.getBlockPos().getZ() + 0.5;

                mc.thePlayer.setPosition(x, y, z);
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;

                // Cancel the event to prevent default middle-click behavior
                event.setCancelled();
            }
        }
    };

    @EventLink
    public final Listener<Render3DEvent> onRender3D = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Update path visualization
        pathPositions.clear();
        targetBlock = rayTraceBlock(1000);

        if (targetBlock != null && targetBlock.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // Check if the block is not air
            if (mc.theWorld.getBlockState(targetBlock.getBlockPos()).getBlock().getMaterial() != Material.air) {
                // Create a path from player to target block
                Vec3 start = mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.getEyeHeight(), 0);
                Vec3 end = new Vec3(
                    targetBlock.getBlockPos().getX() + 0.5,
                    targetBlock.getBlockPos().getY() + 1.0,
                    targetBlock.getBlockPos().getZ() + 0.5
                );

                // Add intermediate points for smooth line
                int segments = 20;
                for (int i = 0; i <= segments; i++) {
                    double t = (double) i / segments;
                    pathPositions.add(new Vec3(
                        start.xCoord + (end.xCoord - start.xCoord) * t,
                        start.yCoord + (end.yCoord - start.yCoord) * t,
                        start.zCoord + (end.zCoord - start.zCoord) * t
                    ));
                }

                // Render the path
                renderPath();
            }
        }
    };

    private MovingObjectPosition rayTraceBlock(double range) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks);
        Vec3 lookVec = mc.thePlayer.getLook(mc.timer.renderPartialTicks);
        Vec3 endPos = eyePos.addVector(
            lookVec.xCoord * range,
            lookVec.yCoord * range,
            lookVec.zCoord * range
        );

        return mc.theWorld.rayTraceBlocks(eyePos, endPos, false, false, false);
    }

    private void renderPath() {
        if (pathPositions.size() < 2) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.disableCull();
        GL11.glDepthMask(false);

        GL11.glLineWidth(3.0f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // Draw the line
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (Vec3 pos : pathPositions) {
            double renderX = pos.xCoord - mc.getRenderManager().renderPosX;
            double renderY = pos.yCoord - mc.getRenderManager().renderPosY;
            double renderZ = pos.zCoord - mc.getRenderManager().renderPosZ;

            worldRenderer.pos(renderX, renderY, renderZ)
                .color(
                    ColorProcess.getColor().getRed() / 255.0f,
                    ColorProcess.getColor().getGreen() / 255.0f,
                    ColorProcess.getColor().getBlue() / 255.0f,
                    0.8f
                ).endVertex();
        }

        tessellator.draw();

        // Draw target block outline
        if (targetBlock != null) {
            double x = targetBlock.getBlockPos().getX() - mc.getRenderManager().renderPosX;
            double y = targetBlock.getBlockPos().getY() - mc.getRenderManager().renderPosY;
            double z = targetBlock.getBlockPos().getZ() - mc.getRenderManager().renderPosZ;

            GL11.glLineWidth(2.0f);

            // Draw box outline
            worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

            float r = ColorProcess.getColor().getRed() / 255.0f;
            float g = ColorProcess.getColor().getGreen() / 255.0f;
            float b = ColorProcess.getColor().getBlue() / 255.0f;

            // Bottom face
            worldRenderer.pos(x, y, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y, z).color(r, g, b, 0.6f).endVertex();

            // Top face
            worldRenderer.pos(x, y + 1, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y + 1, z).color(r, g, b, 0.6f).endVertex();

            tessellator.draw();

            // Draw vertical lines
            worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(x, y, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y + 1, z).color(r, g, b, 0.6f).endVertex();

            worldRenderer.pos(x + 1, y, z).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, 0.6f).endVertex();

            worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, 0.6f).endVertex();

            worldRenderer.pos(x, y, z + 1).color(r, g, b, 0.6f).endVertex();
            worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, 0.6f).endVertex();

            tessellator.draw();
        }

        // Restore GL state
        GL11.glLineWidth(1.0f);
        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
