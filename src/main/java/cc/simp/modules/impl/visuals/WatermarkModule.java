package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
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
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.VISUALS)
public final class WatermarkModule extends Module {

    public static final ModeProperty<Type> type = new ModeProperty<>("Client Watermark Type", Type.Simple);
    public static final Property<Boolean> info = new Property<>("Watermark Info", true, () -> type.getValue() != Type.GameSense && type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst && type.getValue() != Type.Nursultan);
    public static final Property<String> customName = new Property<>("Custom Name", "Simp");

    public enum Type {
        Simple,
        Exhibition,
        GameSense,
        Logo,
        Island,
        Tutorial2017,
        Wurst,
        Nursultan
    }

    private long lastServerTime;
    private long lastUpdateTime;
    private String currentServer;
    private float islandWidth;
    private float islandHeight;
    private ResourceLocation clientLogo;
    private ResourceLocation wurstLogo = new ResourceLocation("simp/images/wurst.png");

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        setSuffix(type.getValue().toString());
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        String clientName = customName.getValue();

        if (type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            String text = clientName;
            if (type.getValue() == Type.Exhibition) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " ";
                    if (info.getValue())
                        text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
                }
            } else if (type.getValue() == Type.Simple) {
                if (info.getValue()) {
                    text = clientName + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
                } else {
                    text = clientName;
                }
            } else if (type.getValue() == Type.GameSense) {
                String serverInfo = (mc.getCurrentServerData() != null) ? mc.getCurrentServerData().serverIP : "Singleplayer";
                text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                        clientName, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
                RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
            }
            fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());

        } else if (type.getValue() == Type.Logo) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/simp_light.png"), 2, 2, (float) 157 / 2, (float) 125 / 2);

        } else if (type.getValue() == Type.Island) {
            renderDynamicIsland(sr, clientName);

        } else if (type.getValue() == Type.Tutorial2017) {
            RenderUtils.drawRect(2, 2, fr.getStringWidth(clientName) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString(clientName, 4, 4, 0x5555FF);
            RenderUtils.drawRect(2, 15, fr.getStringWidth("FPS: " + mc.getDebugFPS()) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString("FPS: " + mc.getDebugFPS(), 4, 17, -1);

        } else if (type.getValue() == Type.Wurst) {
            RenderUtils.drawRect(0, 10, 223, 21, new Color(255, 255, 255, 100));
            RenderUtils.drawImage(wurstLogo, 0, 10f, 758 / 8.5f, 192 / 8.5f);
            mc.fontRendererObj.drawString("v7.46.1" + " MC1.8.9 (outdated)",  92, 17, Color.BLACK.getRGB());

        }

        if (type.getValue() == Type.Nursultan) {
            if (mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) == null || mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime() == 0) return;
            int ping = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
            String skibidi = EnumChatFormatting.BLUE + clientName + EnumChatFormatting.WHITE + " - " + mc.getDebugFPS() + " FPS" + " - " + ping + "ms";
            RenderUtils.drawRoundedRect(2, 2, fr.getStringWidth(skibidi) + 4, fr.FONT_HEIGHT + 4, 6, new Color(0, 0, 0, 200));
            fr.drawStringWithShadow(skibidi, 4, 4, Color.WHITE.getRGB());
        }

    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        String clientName = customName.getValue();

        if (type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            String text = clientName;
            if (type.getValue() == Type.Exhibition) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " ";
                    if (info.getValue())
                        text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
                }
            } else if (type.getValue() == Type.Simple) {
                if (info.getValue()) {
                    text = clientName + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
                } else {
                    text = clientName;
                }
            } else if (type.getValue() == Type.GameSense) {
                String serverInfo = (mc.getCurrentServerData() != null) ? mc.getCurrentServerData().serverIP : "Singleplayer";
                text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                        clientName, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
                RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
            }
            fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
        } else if (type.getValue() == Type.Logo) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/simp_light.png"), 2, 2, (float) 157 / 2, (float) 125 / 2);
        } else if (type.getValue() == Type.Island) {
            renderDynamicIsland(sr, clientName);
        } else if (type.getValue() == Type.Tutorial2017) {
            RenderUtils.drawRect(2, 2, fr.getStringWidth(clientName) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString(clientName, 4, 4, 0x5555FF);
            RenderUtils.drawRect(2, 15, fr.getStringWidth("FPS: " + mc.getDebugFPS()) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString("FPS: " + mc.getDebugFPS(), 4, 17, -1);
        } else if (type.getValue() == Type.Wurst) {
            RenderUtils.drawRect(0, 10, 223, 21, new Color(255, 255, 255, 100));
            RenderUtils.drawImage(wurstLogo, 0, 10f, 758 / 8.5f, 192 / 8.5f);
            mc.fontRendererObj.drawString("v7.46.1" + " MC1.8.9 (outdated)",  92, 17, Color.BLACK.getRGB());
        }
        if (type.getValue() == Type.Nursultan) {
            if (mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) == null || mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime() == 0) return;
            int ping = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
            String skibidi = clientName + " - " + mc.getDebugFPS() + " FPS" + " - " + ping + "ms";
            RenderUtils.drawRoundedRect(2, 2, fr.getStringWidth(skibidi) + 4, fr.FONT_HEIGHT + 4, 6, new Color(0, 0, 0, 200));
            fr.drawStringWithShadow(skibidi, 4, 4, Color.WHITE.getRGB());
        }
    };

    private void renderDynamicIsland(ScaledResolution sr, String clientName) {
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