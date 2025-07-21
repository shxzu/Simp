package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.Render2DEvent;
import cc.simp.font.FontManager;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.CLIENT)
public final class WatermarkModule extends Module {

    private final Property<Boolean> logoProperty = new Property<>("Show Client Logo", true);

    public WatermarkModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        MinecraftFontRenderer minecraftFontRenderer = mc.minecraftFontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        float hue = (System.currentTimeMillis() % 3000) / 3000f;
        SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        String strDate = sdfDate.format(now);

        String text = "S" + EnumChatFormatting.GRAY + "imp " + EnumChatFormatting.WHITE + Simp.INSTANCE.BUILD + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]";

        if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
            FontManager.getCurrentFont().drawStringWithShadow(text, 2, 2, Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        } else {
            minecraftFontRenderer.drawStringWithShadow(text, 2, 2, Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        }

        if(logoProperty.getValue()) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/logo.png"), 2f, sr.getScaledHeight() - 102, 100, 100);
        }
    };

}
