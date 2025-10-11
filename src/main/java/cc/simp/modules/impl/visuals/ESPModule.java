package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldWalkModule;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.ESPUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "ESP", category = ModuleCategory.VISUALS)
public final class ESPModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Corners);
    public static Property<Boolean> players = new Property<>("Only Players", true);

    public enum Mode {
        Full,
        Box,
        Outline,
        Corners,
        Flat
    }

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = event -> {
        final float red = ColorProcess.getColor().getRed() / 255f;
        final float green = ColorProcess.getColor().getGreen() / 255f;
        final float blue = ColorProcess.getColor().getBlue() / 255f;
        switch (mode.getValue()) {
            case Full -> {
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GlStateManager.disableCull();
                GL11.glDepthMask(false);
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (players.getValue() ? entity instanceof EntityPlayer : entity != null && entity != null && !entity.equals(mc.thePlayer)) {
                        ESPUtils.drawEntityESP(entity, red, green, blue, 0.5f, 1.0f, 6.0f);
                    }
                }
                GL11.glDepthMask(true);
                GlStateManager.enableCull();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(2848);
            }
            case Box -> {
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GlStateManager.disableCull();
                GL11.glDepthMask(false);
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (players.getValue() ? entity instanceof EntityPlayer : entity != null && entity != null && !entity.equals(mc.thePlayer)) {
                        ESPUtils.drawEntityESP(entity, red, green, blue, 0.5f, 1.0f, 0.0f);
                    }
                }
                GL11.glDepthMask(true);
                GlStateManager.enableCull();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(2848);
            }
            case Mode.Outline -> {
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GlStateManager.disableCull();
                GL11.glDepthMask(false);
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (players.getValue() ? entity instanceof EntityPlayer : entity != null && entity != null && !entity.equals(mc.thePlayer)) {
                        ESPUtils.drawOutlineEntityESP(entity, red, green, blue, 0.5f, 1.0f, 0.0f);
                    }
                }
                GL11.glDepthMask(true);
                GlStateManager.enableCull();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(2848);
            }
            case Corners -> {
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GlStateManager.disableCull();
                GL11.glDepthMask(false);
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (players.getValue() ? entity instanceof EntityPlayer : entity != null && entity != null && !entity.equals(mc.thePlayer)) {
                        ESPUtils.drawCornerESP(entity, red, green, blue);
                    }
                }
                GL11.glDepthMask(true);
                GlStateManager.enableCull();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(2848);
            }
            case Flat -> {
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GlStateManager.disableCull();
                GL11.glDepthMask(false);
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (players.getValue() ? entity instanceof EntityPlayer : entity != null && entity != null && !entity.equals(mc.thePlayer)) {
                        ESPUtils.drawFake2DESP(entity, red, green, blue);
                    }
                }
                GL11.glDepthMask(true);
                GlStateManager.enableCull();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(2848);
            }
        }
    };

}
