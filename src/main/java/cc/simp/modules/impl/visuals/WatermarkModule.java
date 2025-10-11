package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.VISUALS)
public final class WatermarkModule extends Module {

    public static final ModeProperty<Type> type = new ModeProperty<>("Client Watermark Type", Type.Simple);
    public static final Property<Boolean> info = new Property<>("Watermark Info", true, () -> type.getValue() != Type.GameSense && type.getValue() != Type.Logo);

    public WatermarkModule() {
        toggle();
    }

    public enum Type {
        Simple,
        Logo,
        Exhibition,
        GameSense
    }

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        String text = "Xiva";
        if (type.getValue() == Type.Exhibition) {
            text = "S" + EnumChatFormatting.GRAY + "imp ";
            if (info.getValue())
                text = "S" + EnumChatFormatting.GRAY + "imp " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
        } else if (type.getValue() == Type.Simple) {
            if (info.getValue()) {
                text = "Simp" + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
            } else {
                text = "Simp";
            }
        } else if (type.getValue() == Type.GameSense) {
            String serverInfo = (mc.getCurrentServerData() != null) ? mc.getCurrentServerData().serverIP : "Singleplayer";
            text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                    Simp.NAME, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
            RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
        }
        fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
    };

}
