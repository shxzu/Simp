package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Tracers", category = ModuleCategory.VISUALS)
public final class TracersModule extends Module {

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = event -> {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity.equals(mc.thePlayer)) continue;
            if (entity.isDead) continue;
            if (AntiBotModule.botList.contains(entity)) continue;

            final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks;
            final double y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks) + 1.62F;
            final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks;

            RenderUtils.drawLine(
                    mc.getRenderManager().renderPosX,
                    mc.getRenderManager().renderPosY + mc.thePlayer.getEyeHeight(),
                    mc.getRenderManager().renderPosZ,
                    x, y, z,
                    ColorProcess.getColor(),
                    1.5F
            );
        }
    };
}