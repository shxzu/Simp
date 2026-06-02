package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import cc.simp.utils.render.shaders.ShaderUtils;
import org.lwjgl.opengl.GL11;
import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Shader Sky", category = ModuleCategory.VISUALS)
public final class ShaderSkyModule extends Module {

    private static final ShaderUtils SKY_SHADER = new ShaderUtils("simp/shaders/shader.fsh");

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        if (!isEnabled()) return;
        if (e.getShaderType() != ShaderEvent.ShaderType.SKY) return;

        try {
            // Save GL state
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();

            // Setup GL for full-screen rendering
            GlStateManager.loadIdentity();
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();

            // Save and modify projection matrix for screen-space rendering
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, (double) mc.displayWidth, (double) mc.displayHeight, 0.0D, -1.0D, 1.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();

            SKY_SHADER.init();

            // Set uniforms
            float time = (System.currentTimeMillis() % 100000) / 1000.0f;
            SKY_SHADER.setUniformf("time", time);
            SKY_SHADER.setUniformf("resolution", (float) mc.displayWidth, (float) mc.displayHeight);

            // Draw full-screen quad in screen space
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();

            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            worldrenderer.pos(0.0D, 0.0D, 0.0D).endVertex();
            worldrenderer.pos((double) mc.displayWidth, 0.0D, 0.0D).endVertex();
            worldrenderer.pos((double) mc.displayWidth, (double) mc.displayHeight, 0.0D).endVertex();
            worldrenderer.pos(0.0D, (double) mc.displayHeight, 0.0D).endVertex();
            tessellator.draw();

            SKY_SHADER.unload();

        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            // Restore projection matrix
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);

            // Restore GL state
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(0);
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    };

}
