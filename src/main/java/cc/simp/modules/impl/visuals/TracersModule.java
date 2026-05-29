package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Tracers", category = ModuleCategory.VISUALS)
public class TracersModule extends Module {

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        for (EntityPlayer player : mc.theWorld.playerEntities.stream().toList()) {

            if (player.isEntityAlive() && player != mc.thePlayer && !player.isInvisible()) {
                final double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX;
                final double posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY;
                final double posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ;
                boolean old = mc.gameSettings.viewBobbing;

                GL11.glEnable(3042);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(2929);
                mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
                mc.gameSettings.viewBobbing = false;
                mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2);
                mc.gameSettings.viewBobbing = old;
                double[] color = new double[]{1.0D, 1.0D, 1.0D};
                RenderUtils.drawLine(player, color, posX, posY + player.getEyeHeight(), posZ);
                GL11.glDisable(3042);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(2929);
                GlStateManager.disableBlend();
            }
        }
    };
}
