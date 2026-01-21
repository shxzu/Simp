package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.DraggingProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EnumPlayerModelParts;

import java.awt.*;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Session Information", category = ModuleCategory.VISUALS)
public final class SessionInformationModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Simp);

    private enum Mode {
        Simp("Simp"),
        Modern("Modern");

        public String name;
        Mode(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    private static boolean positionInitialized = false;

    private static final int LABEL_HEIGHT = 12;
    private static final int HUD_HEIGHT = 40;
    private static final int VANILLA_BAR_HEIGHT = 45;
    private static final int MIN_WIDTH = 125;

    private String currentServer;
    private long sessionStartTime = 0;
    private long lastServerTime;

    @EventLink
    public Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        if (sessionStartTime == 0) {
            sessionStartTime = System.currentTimeMillis();
        }
        updateServerInfo();
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        setSuffix(mode.getValue().toString());

        if (System.currentTimeMillis() - lastServerTime > 5000) {
            updateServerInfo();
            lastServerTime = System.currentTimeMillis();
        }

        switch (mode.getValue()) {
            case Simp -> drawSimpSessionInfo();
            case Modern -> drawModernSessionInfo();
        }
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        switch (mode.getValue()) {
            case Simp -> drawSimpSessionInfo();
            case Modern -> drawModernSessionInfo();
        }
    };

    private void initializePosition(ScaledResolution sr, int width) {
        if (!positionInitialized && !DraggingProcess.components.containsKey("TargetInterface")) {
            DraggingProcess.components.put("TargetInterface", new DraggingProcess.DraggableComponent((sr.getScaledWidth() - width) / 2.0,
                    sr.getScaledHeight() - HUD_HEIGHT - LABEL_HEIGHT - VANILLA_BAR_HEIGHT - 5));
            positionInitialized = true;
        }
    }

    private void drawSimpSessionInfo() {
        ScaledResolution sr = new ScaledResolution(mc);

        String playingOnText = "Playing on " + currentServer;
        String playTimeText = "Play Time: " + getFormattedPlayTime();

        int playingOnWidth = FontProcess.getFont("simp").getStringWidth(playingOnText);
        int playTimeWidth = FontProcess.getFont("simp").getStringWidth(playTimeText);
        int maxTextWidth = Math.max(playingOnWidth, playTimeWidth);
        int hudWidth = Math.max(MIN_WIDTH, 38 + maxTextWidth + 5);

        initializePosition(sr, hudWidth);

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(HUD_HEIGHT + LABEL_HEIGHT);
        draggableComponent.setWidth(hudWidth);

        GlStateManager.pushMatrix();
        GlStateManager.translate(draggableComponent.getX(), draggableComponent.getY(), 0);

        Color color = Color.WHITE;

        Gui.drawRect(0, 0, hudWidth, LABEL_HEIGHT, new Color(0, 0, 0, 220).getRGB());

        String labelText = "Session Info";
        int labelTextWidth = FontProcess.getFont("simp").getStringWidth(labelText);
        FontProcess.getFont("simp").drawString(labelText, (hudWidth - labelTextWidth) / 2, 2, color.getRGB());

        Gui.drawRect(0, LABEL_HEIGHT, hudWidth, LABEL_HEIGHT + 36, new Color(0, 0, 0, 180).getRGB());

        RenderUtils.resetColor();

        renderPlayerSkin(mc.thePlayer, 2, LABEL_HEIGHT + 2);

        FontProcess.getFont("bold").drawString(mc.thePlayer.getName(), 38, LABEL_HEIGHT + 2, color.getRGB());

        FontProcess.getFont("simp").drawString(playingOnText, 38, LABEL_HEIGHT + 13, color.getRGB());

        FontProcess.getFont("simp").drawString(playTimeText, 38, LABEL_HEIGHT + 23, color.getRGB());

        RenderUtils.resetColor();

        GlStateManager.popMatrix();
    }

    private void drawModernSessionInfo() {
        ScaledResolution sr = new ScaledResolution(mc);

        String playingOnText = "Playing on " + currentServer;
        String playTimeText = "Play Time: " + getFormattedPlayTime();

        int playingOnWidth = FontProcess.getFont("simp").getStringWidth(playingOnText);
        int playTimeWidth = FontProcess.getFont("simp").getStringWidth(playTimeText);
        int maxTextWidth = Math.max(playingOnWidth, playTimeWidth);
        int hudWidth = Math.max(MIN_WIDTH, 38 + maxTextWidth + 5);

        initializePosition(sr, hudWidth);

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(HUD_HEIGHT + LABEL_HEIGHT);
        draggableComponent.setWidth(hudWidth);

        GlStateManager.pushMatrix();
        GlStateManager.translate(draggableComponent.getX(), draggableComponent.getY(), 0);

        Color color = Color.WHITE;
        Color gradientStart = new Color(ColorProcess.getColor().getRed(), ColorProcess.getColor().getGreen(), ColorProcess.getColor().getBlue(), 200);

        RenderUtils.drawRoundedRect(0, 0, hudWidth, LABEL_HEIGHT + 36, 6, true, gradientStart);

        RenderUtils.drawRoundedRect(2, LABEL_HEIGHT - 1, hudWidth - 4, 1, 0.5f, gradientStart.brighter());

        String labelText = "Session Info";
        int labelTextWidth = FontProcess.getFont("simp").getStringWidth(labelText);
        FontProcess.getFont("simp").drawString(labelText, (hudWidth - labelTextWidth) / 2, 2, color.getRGB());

        RenderUtils.resetColor();

        renderPlayerSkin(mc.thePlayer, 2, LABEL_HEIGHT + 2);

        FontProcess.getFont("bold").drawString(mc.thePlayer.getName(), 38, LABEL_HEIGHT + 2, color.getRGB());

        FontProcess.getFont("simp").drawString(playingOnText, 38, LABEL_HEIGHT + 13, new Color(180, 180, 180).getRGB());

        FontProcess.getFont("simp").drawString(playTimeText, 38, LABEL_HEIGHT + 23, new Color(180, 180, 180).getRGB());

        RenderUtils.resetColor();

        GlStateManager.popMatrix();
    }

    private String getFormattedPlayTime() {
        if (sessionStartTime == 0) {
            return "00:00:00";
        }

        long playTimeMillis = System.currentTimeMillis() - sessionStartTime;
        long seconds = playTimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateServerInfo() {
        currentServer = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }

    private void renderPlayerSkin(EntityLivingBase player, int x, int y) {
        List<NetworkPlayerInfo> playerInfoList = GuiPlayerTabOverlay.field_175252_a.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());
        for (NetworkPlayerInfo info : playerInfoList) {
            if (mc.theWorld.getPlayerEntityByUUID(info.getGameProfile().getId()) == player) {
                mc.getTextureManager().bindTexture(info.getLocationSkin());

                Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, 32, 32, 64, 64);

                if (player.func_175148_a(EnumPlayerModelParts.HAT)) {
                    Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, 32, 32, 64, 64);
                }

                break;
            }
        }
    }
}