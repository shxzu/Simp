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
import cc.simp.utils.render.GlUtils;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.shaders.RoundedShader;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;
import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(label = "Watermark", category = ModuleCategory.VISUALS)
public final class WatermarkModule extends Module {

    public static final ModeProperty<Type> type = new ModeProperty<>("Client Watermark Type", Type.Simple);
    public static final Property<Boolean> info = new Property<>("Watermark Info", true, () -> type.getValue() != Type.GameSense && type.getValue() != Type.Logo && type.getValue() != Type.Island);

    public enum Type {
        Simple,
        Exhibition,
        GameSense,
        Logo,
        Island
    }

    private long lastServerTime;
    private long lastUpdateTime;
    private String currentServer;
    private float islandWidth;
    private float islandHeight;
    private ResourceLocation clientLogo;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        if (type.getValue() != Type.Logo && type.getValue() != Type.Island) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            String text = "Simp";
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
        } else if (type.getValue() == Type.Logo) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/simp_light.png"), 2, 2, (float) 157 / 2, (float) 125 / 2);
        } else if (type.getValue() == Type.Island) {
            renderDynamicIsland(sr);
        }
    };

    private void renderDynamicIsland(ScaledResolution sr) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        if (System.currentTimeMillis() - lastServerTime > 5000) {
            updateServerInfo();
            lastServerTime = System.currentTimeMillis();
        }

        int centerX = sr.getScaledWidth() / 2;
        int yPos = 5;

        String serverText = currentServer != null ? currentServer : "Loading...";

        int smallTextWidth, fontHeight;
        smallTextWidth = fr.getStringWidth(" | " + serverText + " | " + Simp.VERSION);
        fontHeight = fr.getHeight();

        int totalWidth = smallTextWidth + 29;
        int height = 24;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        float smoothTime = 0.15f;
        float alpha = 1.0f - (float) Math.exp(-deltaTime / smoothTime);

        islandHeight = RenderUtils.lerp(islandHeight, height, alpha);
        islandWidth = RenderUtils.lerp(islandWidth, totalWidth, alpha);

        int startX = (int) (centerX - islandWidth / 2);

        RenderUtils.drawRoundedRect(centerX - islandWidth / 2 - 1, yPos - 1, islandWidth + 2, islandHeight + 2, 1.5f, new Color(0, 0, 0, 100));

        int logoSize = 256;
        int logoX = startX + 5;
        int logoY = yPos + (height - logoSize) / 2;
        clientLogo = new ResourceLocation("simp/images/simp_light.png");
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.1, 0.1, 0.1);
        float newLogoX = (float) (logoX / 0.1 - 55);
        float newLogoY = (float) (logoY / 1.25 + 130);
        RenderUtils.drawImage(clientLogo, (int) newLogoX, (int) newLogoY, 255, 255);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();

        int textX = logoX + 20;
        int textY = yPos + (height - fontHeight) / 2 + 1;

        fr.drawString(
                " | " + serverText + " | " + Simp.VERSION,
                textX,
                textY,
                0xAAAAAA
        );
    }

    private void updateServerInfo() {
        currentServer = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}
