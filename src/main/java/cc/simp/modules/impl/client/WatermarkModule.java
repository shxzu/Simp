package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.overlay.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.font.FontManager;
import cc.simp.utils.client.font.TrueTypeFontRenderer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.CLIENT)
public final class WatermarkModule extends Module {

    public WatermarkModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        MinecraftFontRenderer minecraftFontRenderer = mc.minecraftFontRendererObj;
        TrueTypeFontRenderer CFont = FontManager.CSGO_FR;
        ScaledResolution sr = new ScaledResolution(mc);
        float hue = (System.currentTimeMillis() % 3000) / 3000f;

        String text = "S" + EnumChatFormatting.GRAY + "imp " + EnumChatFormatting.WHITE + Simp.INSTANCE.BUILD + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "FPS " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + EnumChatFormatting.GRAY + "]";

        if(FontManagerModule.fontTypeProperty.getValue() == FontManagerModule.FontType.TAHOMA) {
            CFont.drawStringWithShadow(text, 2, 2, Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        } else {
            minecraftFontRenderer.drawString(text, 2, 2, Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        }
    };

}
