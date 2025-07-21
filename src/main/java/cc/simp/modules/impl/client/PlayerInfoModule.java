package cc.simp.modules.impl.client;

import cc.simp.event.impl.render.Render2DEvent;
import cc.simp.font.FontManager;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Player Info", category = ModuleCategory.CLIENT)
public final class PlayerInfoModule extends Module {

    public PlayerInfoModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        MinecraftFontRenderer minecraftFontRenderer = mc.minecraftFontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        float hue = (System.currentTimeMillis() % 3000) / 3000f;

        if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
            FontManager.getCurrentFont().drawStringWithShadow(EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "FPS " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + EnumChatFormatting.GRAY + "]", sr.getScaledWidth() / 1.062f, sr.getScaledHeight() - 25, -1);
            FontManager.getCurrentFont().drawStringWithShadow(EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "BPS " + EnumChatFormatting.WHITE + MovementUtils.calculateBPS() + EnumChatFormatting.GRAY + "]", sr.getScaledWidth() / 1.062f, sr.getScaledHeight() - 15, -1);
        } else {
            minecraftFontRenderer.drawStringWithShadow(EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "FPS " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + EnumChatFormatting.GRAY + "]", sr.getScaledWidth() / 1.062f, sr.getScaledHeight() - 25, -1);
            minecraftFontRenderer.drawStringWithShadow(EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "BPS " + EnumChatFormatting.WHITE + MovementUtils.calculateBPS() + EnumChatFormatting.GRAY + "]", sr.getScaledWidth() / 1.062f, sr.getScaledHeight() - 15, -1);
        }
    };

}
