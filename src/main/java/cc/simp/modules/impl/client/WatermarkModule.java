package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.overlay.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.*;
import java.util.List;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.CLIENT)
public final class WatermarkModule extends Module {

    public WatermarkModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        String text = "S" + EnumChatFormatting.GRAY + "imp " + EnumChatFormatting.WHITE + Simp.INSTANCE.BUILD + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + "FPS " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + EnumChatFormatting.GRAY + "]";

        fontRenderer.drawStringWithShadow(text, 2f, 2f, Color.RED.getRGB());
    };

}
