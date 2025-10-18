package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.DraggingProcess;
import cc.simp.processes.FontProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;

import java.awt.*;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Target Interface", category = ModuleCategory.VISUALS)
public final class TargetInterfaceModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Simp);

    public enum Mode {
        Simp,
        Astolfo
    }

    private static boolean positionInitialized = false;
    public static EntityLivingBase target;

    private static final int HUD_WIDTH = 150;
    private static final int HUD_HEIGHT = 40;
    private static final int VANILLA_BAR_HEIGHT = 45;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        setSuffix(mode.getValue().toString());
        switch (mode.getValue()) {
            case Astolfo -> drawAstolfoTargetInterface();
            case Simp -> drawSimpTargetInterface();
        }
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        switch (mode.getValue()) {
            case Astolfo -> drawAstolfoTargetInterface();
            case Simp -> drawSimpTargetInterface();
        }
    };

    private void initializePosition(ScaledResolution sr) {
        if (!positionInitialized) {
            DraggingProcess.components.put("TargetInterface", new DraggingProcess.DraggableComponent((sr.getScaledWidth() - HUD_WIDTH) / 2.0,
                    sr.getScaledHeight() - HUD_HEIGHT - VANILLA_BAR_HEIGHT - 5));
            positionInitialized = true;
        }
    }

    private void drawAstolfoTargetInterface() {
        target = KillAuraModule.target;

        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        if (mc.currentScreen instanceof GuiChat) {
            target = mc.thePlayer;
        } else {
            if (target == null) return;
        }

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(HUD_HEIGHT);
        draggableComponent.setWidth(HUD_WIDTH);

        GlStateManager.pushMatrix();
        GlStateManager.translate(draggableComponent.getX(), draggableComponent.getY(), 0);

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float healthPercentage = health / maxHealth;

        Color color = ColorProcess.getColor();

        Gui.drawRect(0, 0, 125, 36, new Color(0, 0, 0, 150).getRGB());

        Gui.drawRect(37, 26, 89, 32, new Color(0, 0, 0, 255).getRGB());

        int healthWidth = (int) (52 * healthPercentage);
        Gui.drawRect(37, 26, 37 + healthWidth, 32, color.getRGB());

        GuiInventory.drawEntityOnScreen(15, 32, 16, -target.rotationYaw, target.rotationPitch, target);

        mc.fontRendererObj.drawString(target.getName(), 38, 2, -1, true);

        GlStateManager.pushMatrix();
        GlStateManager.scale(2,2,2);
        mc.fontRendererObj.drawStringWithShadow((Math.round(target.getHealth() * 10f) / 10f + "❤").replace(".0", ""), 19,
                5, color.getRGB());
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
    }

    private void drawSimpTargetInterface() {
        target = KillAuraModule.target;

        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        if (mc.currentScreen instanceof GuiChat) {
            target = mc.thePlayer;
        } else {
            if (target == null) return;
        }

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(HUD_HEIGHT);
        draggableComponent.setWidth(HUD_WIDTH);

        GlStateManager.pushMatrix();
        GlStateManager.translate(draggableComponent.getX(), draggableComponent.getY(), 0);

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float healthPercentage = health / maxHealth;

        Color color = Color.WHITE;

        Gui.drawRect(0, 0, 125, 36, new Color(0, 0, 0, 180).getRGB());

        Gui.drawRect(37, 26, 89, 32, new Color(0, 0, 0, 255).getRGB());

        int healthWidth = (int) (52 * healthPercentage);
        Gui.drawRect(37, 26, 37 + healthWidth, 32, color.getRGB());

        if(target instanceof EntityPlayer) {
            renderPlayerSkin(target, 2, 2);
        } else {
            renderPlayerSkin(mc.thePlayer, 2, 2);
        }

        FontProcess.getFont("bold").drawString(target.getName(), 38, 2, color.getRGB());

        FontProcess.getFont("bold").drawString((Math.round(target.getHealth() * 10f) / 10f + "").replace(".0", ""), 38,
                13, color.getRGB());

        GlStateManager.popMatrix();
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
