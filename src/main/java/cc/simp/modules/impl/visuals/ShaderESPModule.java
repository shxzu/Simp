package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.events.impl.render.Shader3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.shaders.ShaderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Shader ESP", category = ModuleCategory.VISUALS)
public final class ShaderESPModule extends Module {

    private Framebuffer dataFBO = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private final ShaderUtils shaderProgram = new ShaderUtils("simp/shaders/fade_outline.glsl");

    public static boolean runningShader;


    @Override
    public void onDisable() {
        runningShader = false;
    }


    @EventLink
    public final Listener<Shader3DEvent> shader3DEventListener = event -> {
        runningShader = true;
        setupBuffers();

        dataFBO.bindFramebuffer(true);

        RendererLivingEntity.NAME_TAG_RANGE = 0;
        RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 0;

        final float partialTicks = event.getPartialTicks();

        int count = 0;
        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            final Render<EntityPlayer> render = mc.getRenderManager().getEntityRenderObject(player);

            if (mc.getRenderManager() == null || render == null || player == null || player == mc.thePlayer || player.isDead || AntiBotModule.botList.contains(player) || !RenderUtils.isInViewFrustrum(player))
                continue;

            final double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
            final double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
            final double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
            final float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

            final Color color = ColorProcess.getColor();

            if (player != null) {
                render.doRender(player, x - mc.getRenderManager().renderPosX, y - mc.getRenderManager().renderPosY, z - mc.getRenderManager().renderPosZ, yaw, partialTicks);
            }

            count++;
        }

        RendererLivingEntity.NAME_TAG_RANGE = 64;
        RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 32;

        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap();
        mc.getFramebuffer().bindFramebuffer(true);
        runningShader = false;
   };

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        if (mc.gameSettings.showDebugInfo)
            return;

        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        final int programID = shaderProgram.programID;

        // TODO: you can create values for those variables passed to the shader
        final Color color = ColorProcess.getColor();
        final int radius = 7;
        final int fading = 300;

        dataFBO.bindFramebuffer(true);
        mc.getFramebuffer().bindFramebuffer(true);

        shaderProgram.init();
        passUniforms(programID, scaledResolution, color, radius, fading);

        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GlStateManager.enableBlend();
        dataFBO.bindFramebufferTexture();
        ShaderUtils.drawQuads();
        shaderProgram.unload();
        GlStateManager.disableBlend();
    };

    private void passUniforms(final int programID, final ScaledResolution scaledResolution, final Color color, final int radius, final int fading) {
        GL20.glUniform1i(GL20.glGetUniformLocation(programID, "u_diffuse_sampler"), 0);
        GL20.glUniform2f(GL20.glGetUniformLocation(programID, "u_texel_size"), 1.0F / scaledResolution.getScaledWidth(), 1.0F / scaledResolution.getScaledHeight());
        GL20.glUniform1i(GL20.glGetUniformLocation(programID, "u_radius"), radius);
        GL20.glUniform1i(GL20.glGetUniformLocation(programID, "u_fading"), fading);
        GL20.glUniform4f(
                GL20.glGetUniformLocation(programID, "u_color"),
                color.getRed() / 255.0F,
                color.getGreen() / 255.0F,
                color.getBlue() / 255.0F,
                color.getAlpha() / 255.0F
        );
    }

    private void setupBuffers() {
        try {
            dataFBO.framebufferClear();

            if (mc.displayWidth != dataFBO.framebufferWidth || mc.displayHeight != dataFBO.framebufferHeight) {
                dataFBO.deleteFramebuffer();
                dataFBO = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            }
        } catch (final Exception exception) {
        }
    }

}
