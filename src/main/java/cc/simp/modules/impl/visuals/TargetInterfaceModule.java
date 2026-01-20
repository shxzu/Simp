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
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Target Interface", category = ModuleCategory.VISUALS)
public final class TargetInterfaceModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Simp);

    private enum Mode {
        Simp("Simp"),
        Astolfo("Astolfo"),
        Exhibition("Exhibition"),
        BlueArchive("Blue Archive"),
        Rise("Rise");

        public String name;
        Mode(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
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
            case Exhibition -> drawExhibitionTargetInterface();
            case Rise -> drawRiseTargetInterface();
            case BlueArchive -> drawBlueArchiveTargetInterface();
        }
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        switch (mode.getValue()) {
            case Astolfo -> drawAstolfoTargetInterface();
            case Simp -> drawSimpTargetInterface();
            case Exhibition -> drawExhibitionTargetInterface();
            case Rise -> drawRiseTargetInterface();
            case BlueArchive -> drawBlueArchiveTargetInterface();
        }
    };

    private void initializePosition(ScaledResolution sr) {
        if (!positionInitialized && !DraggingProcess.components.containsKey("TargetInterface")) {
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

        RenderUtils.resetColor();
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        GuiInventory.drawEntityOnScreen(15, 32, 16, -target.rotationYaw, target.rotationPitch, target);

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.popMatrix();

        mc.fontRendererObj.drawString(target.getName(), 38, 2, -1, true);

        GlStateManager.pushMatrix();
        GlStateManager.scale(2, 2, 2);
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
        Gui.drawRect(37, 26, 37 + healthWidth, 32, ColorProcess.getColor().getRGB());

        RenderUtils.resetColor();

        if (target instanceof EntityPlayer) {
            renderPlayerSkin(target, 2, 2);
        } else {
            renderPlayerSkin(mc.thePlayer, 2, 2);
        }

        FontProcess.getFont("bold").drawString(target.getName(), 38, 2, color.getRGB());

        FontProcess.getFont("bold").drawString((Math.round(target.getHealth() * 10f) / 10f + "").replace(".0", ""), 38,
                13, color.getRGB());

        GlStateManager.popMatrix();
    }

    private void drawExhibitionTargetInterface() {
        target = KillAuraModule.target;

        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        if (mc.currentScreen instanceof GuiChat) {
            target = mc.thePlayer;
        } else {
            if (target == null) return;
        }

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(42);
        draggableComponent.setWidth(150);

        float x = (float) draggableComponent.getX();
        float y = (float) draggableComponent.getY();
        final int MIN_WIDTH = 135;
        final int HEIGHT = 42;

        if (target == null) return;

        String name = target.getName();
        int width = Math.max(MIN_WIDTH, mc.fontRendererObj.getStringWidth("Name: " + name) + 60);

        Color darkest = new Color(10, 10, 10, 180);
        Color secondDarkest = new Color(22, 22, 22, 180);
        Color lightest = new Color(44, 44, 44, 180);
        Color middleColor = new Color(34, 34, 34, 180);
        Color textColor = Color.WHITE;

        // Draw borders
        Gui.drawRect((int) (x - 3.5), (int) (y - 3.5), (int) (x + width + 3.5), (int) (y + HEIGHT + 3.5), darkest.getRGB());
        Gui.drawRect((int) (x - 3), (int) (y - 3), (int) (x + width + 3), (int) (y + HEIGHT + 3), middleColor.getRGB());
        Gui.drawRect((int) (x - 1), (int) (y - 1), (int) (x + width + 1), (int) (y + HEIGHT + 1), lightest.getRGB());
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + HEIGHT), secondDarkest.getRGB());

        // Draw player model border
        float size = HEIGHT - 6;
        Gui.drawRect((int) (x + 3), (int) (y + 3), (int) (x + 3.5), (int) (y + 3 + size), lightest.getRGB());
        Gui.drawRect((int) (x + 3), (int) (y + 3 + size), (int) (x + 3 + size), (int) (y + 3 + size + 0.5), lightest.getRGB());
        Gui.drawRect((int) (x + 3 + size), (int) (y + 3), (int) (x + 3.5 + size), (int) (y + 3 + size + 0.5), lightest.getRGB());
        Gui.drawRect((int) (x + 3), (int) (y + 3), (int) (x + 3 + size), (int) (y + 3.5), lightest.getRGB());

        // Draw name
        FontProcess.getFont("bold").drawString(name, (float) (x + 8 + size), (float) (y + 6), textColor.getRGB());

        // Calculate health
        float health = target.getHealth() + target.getAbsorptionAmount();
        float maxHealth = target.getMaxHealth() + target.getAbsorptionAmount();
        float healthValue = health / maxHealth;

        // Health color interpolation
        Color healthColor;
        if (healthValue > 0.5f) {
            healthColor = RenderUtils.interpolateColorC(new Color(255, 255, 10), new Color(10, 255, 10), (healthValue - 0.5f) / 0.5f);
        } else {
            healthColor = RenderUtils.interpolateColorC(new Color(255, 10, 10), new Color(255, 255, 10), healthValue * 2);
        }

        // Draw health bar
        float healthBarWidth = width - (size + 12);
        Gui.drawRect((int) (x + 8 + size), (int) (y + 15), (int) (x + 8 + size + healthBarWidth), (int) (y + 20), darkest.getRGB());
        Gui.drawRect((int) (x + 8 + size + 0.5), (int) (y + 15.5), (int) (x + 8 + size + healthBarWidth - 0.5), (int) (y + 19.5),
                RenderUtils.blendColors(darkest, healthColor, 0.2f));

        float healthBarActualWidth = healthBarWidth - 1;
        Gui.drawRect((int) (x + 8 + size + 0.5), (int) (y + 15.5), (int) (x + 8 + size + 0.5 + healthBarActualWidth * healthValue),
                (int) (y + 19.5), healthColor.getRGB());

        // Draw health bar separators
        float increment = healthBarActualWidth / 11;
        for (int i = 1; i < 11; i++) {
            Gui.drawRect((int) (x + 8 + size + increment * i), (int) (y + 15.5), (int) (x + 8 + size + increment * i + 0.5),
                    (int) (y + 19.5), darkest.getRGB());
        }

        // Draw health and distance text
        float distance = mc.thePlayer.getDistanceToEntity(target);
        String statsText = String.format("HP: %.1f | Dist: %.1f", health, distance);
        FontProcess.getFont("simp").drawString(statsText, (float) (x + 8 + size), (float) (y + 25), textColor.getRGB());

        // Draw player model
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GuiInventory.drawEntityOnScreen((int) (x + 3 + size / 2f), (int) (y + size + 1), 18, target.rotationYaw, -target.rotationPitch, target);
        GlStateManager.popMatrix();

        // Draw armor
        RenderHelper.enableGUIStandardItemLighting();
        float separation = healthBarWidth / 5;

        for (int i = 0; i <= 3; i++) {
            if (target.getCurrentArmor(i) != null) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(target.getCurrentArmor(i),
                        (int) (x + size + 7 + (separation * (3 - i))), (int) (y + 28));
            }
        }

        // Draw held item
        if (target.getHeldItem() != null) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(target.getHeldItem(),
                    (int) (x + size + 7 + (separation * 4)), (int) (y + 28));
        }

        RenderHelper.disableStandardItemLighting();
    }

    private void drawBlueArchiveTargetInterface() {
        target = KillAuraModule.target;

        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        if (mc.currentScreen instanceof GuiChat) {
            target = mc.thePlayer;
        } else {
            if (target == null) return;
        }

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(55);
        draggableComponent.setWidth(180);

        float x = (float) draggableComponent.getX();
        float y = (float) draggableComponent.getY();

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float healthPercentage = health / maxHealth;

        // Blue Archive color scheme
        Color baBlue = new Color(41, 182, 246);
        Color baLightBlue = new Color(129, 212, 250);
        Color baWhite = new Color(255, 255, 255);
        Color baBackground = new Color(15, 20, 35, 200);
        Color baAccent = new Color(255, 213, 79);

        // Main background
        RenderUtils.drawRoundedRect(x, y, 180, 55, 4, baBackground);

        // Top accent bar
        Gui.drawRect((int) x, (int) y, (int) (x + 180), (int) (y + 3), baBlue.getRGB());

        // Left side accent
        Gui.drawRect((int) x, (int) (y + 3), (int) (x + 2), (int) (y + 55), baLightBlue.getRGB());

        // Player avatar background
        RenderUtils.drawRoundedRect(x + 5, y + 8, 40, 40, 3, new Color(25, 35, 55, 220));

        // Avatar border effect
        RenderUtils.drawRoundedRect(x + 4, y + 7, 42, 42, 3, new Color(baBlue.getRed(), baBlue.getGreen(), baBlue.getBlue(), 100));

        // Render player skin
        if (target instanceof EntityPlayer) {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);

            mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
            Gui.drawScaledCustomSizeModalRect((int) (x + 7), (int) (y + 10), 8, 8, 8, 8, 36, 36, 64, 64);

            if (((EntityPlayer) target).func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect((int) (x + 7), (int) (y + 10), 40, 8, 8, 8, 36, 36, 64, 64);
            }

            GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);

            mc.getTextureManager().bindTexture(((AbstractClientPlayer) mc.thePlayer).getLocationSkin());
            Gui.drawScaledCustomSizeModalRect((int) (x + 7), (int) (y + 10), 8, 8, 8, 8, 36, 36, 64, 64);

            if (((EntityPlayer) mc.thePlayer).func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect((int) (x + 7), (int) (y + 10), 40, 8, 8, 8, 36, 36, 64, 64);
            }

            GlStateManager.popMatrix();
        }

        // Name label background
        RenderUtils.drawRoundedRect(x + 50, y + 8, 123, 18, 2, new Color(30, 40, 60, 180));

        // Draw name
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        FontProcess.getFont("bold").drawString(target.getName(), x + 55, y + 12, baWhite.getRGB());
        GlStateManager.popMatrix();

        // HP label
        GlStateManager.pushMatrix();
        FontProcess.getFont("bold").drawString("HP", x + 55, y + 25, baAccent.getRGB());
        GlStateManager.popMatrix();

        // Health bar background
        RenderUtils.drawRoundedRect(x + 50, y + 35, 120, 10, 3, new Color(20, 30, 50, 200));

        // Health bar inner shadow
        RenderUtils.drawRoundedRect(x + 51, y + 36, 118, 8, 2, new Color(10, 15, 25, 150));

        // Calculate health bar width
        float healthBarWidth = 116 * healthPercentage;

        // Health bar gradient
        Color healthColorStart = healthPercentage > 0.5f ? baLightBlue : new Color(255, 82, 82);
        Color healthColorEnd = healthPercentage > 0.5f ? baBlue : new Color(244, 67, 54);

        if (healthBarWidth > 0) {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            drawHorizontalGradient((int) (x + 52), (int) (y + 37), (int) (x + 52 + healthBarWidth), (int) (y + 43),
                    healthColorStart.getRGB(), healthColorEnd.getRGB());
            GlStateManager.popMatrix();

            // Health bar highlight
            Gui.drawRect((int) (x + 52), (int) (y + 37), (int) (x + 52 + healthBarWidth), (int) (y + 39),
                    new Color(255, 255, 255, 60).getRGB());
        }

        // Health bar segments
        int segments = 10;
        float segmentWidth = 116f / segments;
        for (int i = 1; i < segments; i++) {
            Gui.drawRect((int) (x + 52 + segmentWidth * i), (int) (y + 37), (int) (x + 52 + segmentWidth * i + 1), (int) (y + 43),
                    new Color(20, 30, 50, 180).getRGB());
        }

        // Health text with background
        String healthText = String.format("%.1f / %.1f", health, maxHealth);
        int healthTextWidth = FontProcess.getFont("noto").getStringWidth(healthText);
        RenderUtils.drawRoundedRect(x + 110 - healthTextWidth / 2f - 2, y + 47, healthTextWidth + 4, 7, 2,
                new Color(15, 20, 35, 200));

        GlStateManager.pushMatrix();
        FontProcess.getFont("noto").drawString(healthText, x + 110 - healthTextWidth / 2f, y + 48, baWhite.getRGB());
        GlStateManager.popMatrix();

        // Distance indicator
        float distance = mc.thePlayer.getDistanceToEntity(target);
        String distText = String.format("%.1fm", distance);

        RenderUtils.drawRoundedRect(x + 148, y + 10, 25, 10, 2, new Color(30, 40, 60, 180));

        GlStateManager.pushMatrix();
        FontProcess.getFont("noto").drawString(distText, x + 152, y + 12, baLightBlue.getRGB());
        GlStateManager.popMatrix();

        // Hurt indicator
        if (target.hurtTime > 0) {
            float hurtAlpha = (target.hurtTime / 10f) * 0.3f;
            RenderUtils.drawRoundedRect(x, y, 180, 55, 4,
                    new Color(255, 50, 50, (int) (hurtAlpha * 255)));
        }

        GlStateManager.resetColor();
    }

    private void drawHorizontalGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (startColor >> 24 & 255) / 255.0F;
        float startRed = (startColor >> 16 & 255) / 255.0F;
        float startGreen = (startColor >> 8 & 255) / 255.0F;
        float startBlue = (startColor & 255) / 255.0F;

        float endAlpha = (endColor >> 24 & 255) / 255.0F;
        float endRed = (endColor >> 16 & 255) / 255.0F;
        float endGreen = (endColor >> 8 & 255) / 255.0F;
        float endBlue = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldrenderer.pos(left, top, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(left, bottom, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(right, bottom, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }


    private void drawRiseTargetInterface() {
        target = KillAuraModule.target;

        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        if (mc.currentScreen instanceof GuiChat) {
            target = mc.thePlayer;
        } else {
            if (target == null) return;
        }

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("TargetInterface");
        draggableComponent.setHeight(50);
        draggableComponent.setWidth(150);

        RiseTargetHUD.render(draggableComponent.getX(), draggableComponent.getY(), target);
    }

    // had to do this one like this due to the insane amount of code needed for the rise hud

    public static class RiseTargetHUD {

        private static final int MIN_WIDTH = 128;
        private static final int HEIGHT = 50;

        private static boolean sentParticles = false;
        private static final List<Particle> particles = new ArrayList<>();
        private static long lastUpdate = System.currentTimeMillis();
        private static float animatedHealthBar = 0;

        public static void render(double x, double y, EntityLivingBase target) {
            if (target == null) return;

            String name = target.getName();
            int width = Math.max(MIN_WIDTH, mc.fontRendererObj.getStringWidth("Name: " + name) + 60);

            Color backgroundColor = new Color(0, 0, 0, 110);
            int textColor = Color.WHITE.getRGB();

            // Draw background with rounded corners (simulated)
            RenderUtils.drawRoundedRect(x, y, width, HEIGHT, 6, backgroundColor);

            // Calculate hurt effect
            final int scaleOffset = (int) (target.hurtTime * 0.35f);

            // Calculate health
            float health = target.getHealth() + target.getAbsorptionAmount();
            float maxHealth = target.getMaxHealth() + target.getAbsorptionAmount();
            float healthPercent = health / maxHealth;
            float targetHealthBar = (width - 28) * healthPercent;

            // Animate health bar
            animatedHealthBar = lerp(animatedHealthBar, targetHealthBar, 0.18f);

            // Draw health bar with gradient
            GlStateManager.pushMatrix();
            Color color1 = ColorProcess.getColor();
            Color color2 = ColorProcess.getColor();
            drawGradientRect((int) (x + 5), (int) (y + 40), (int) (x + 5 + animatedHealthBar), (int) (y + 45), color1.getRGB(), color2.getRGB());
            GlStateManager.popMatrix();

            // Render particles
            for (Particle p : particles) {
                p.x = (float) (x + 20);
                p.y = (float) (y + 20);
                if (p.opacity > 4) p.render2D();
            }

            // Render player face
            if (target instanceof AbstractClientPlayer) {
                final double offset = -(target.hurtTime * 23);
                Color playerTint = new Color(255, (int) (255 + offset), (int) (255 + offset));
                GlStateManager.color(playerTint.getRed() / 255f, playerTint.getGreen() / 255f, playerTint.getBlue() / 255f, 1f);

                mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
                Gui.drawScaledCustomSizeModalRect((int) (x + 5 + scaleOffset / 2f), (int) (y + 5 + scaleOffset / 2f),
                        8, 8, 8, 8, (int) (30 - scaleOffset), (int) (30 - scaleOffset), 64, 64);

                GlStateManager.color(1, 1, 1, 1);
            } else {
                final double offset = -(target.hurtTime * 23);
                Color playerTint = new Color(255, (int) (255 + offset), (int) (255 + offset));
                GlStateManager.color(playerTint.getRed() / 255f, playerTint.getGreen() / 255f, playerTint.getBlue() / 255f, 1f);

                mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
                Gui.drawScaledCustomSizeModalRect((int) (x + 5 + scaleOffset / 2f), (int) (y + 5 + scaleOffset / 2f),
                        8, 8, 8, 8, (int) (30 - scaleOffset), (int) (30 - scaleOffset), 64, 64);

                GlStateManager.color(1, 1, 1, 1);
            }

            // Update particles
            if (System.currentTimeMillis() - lastUpdate >= 16) {
                for (int i = particles.size() - 1; i >= 0; i--) {
                    Particle p = particles.get(i);
                    p.updatePosition();
                    if (p.opacity < 1) particles.remove(i);
                }
                lastUpdate = System.currentTimeMillis();
            }

            // Draw health text
            double healthNum = Math.round(health * 10.0) / 10.0;
            FontProcess.getFont("simp").drawString(String.valueOf(healthNum), (float) (x + animatedHealthBar + 8), (float) (y + 38), textColor);

            // Draw name
            FontProcess.getFont("bold").drawString("Name: " + name, (float) (x + 40), (float) (y + 10), textColor);

            // Draw distance and hurt time
            float distance = mc.thePlayer.getDistanceToEntity(target);
            String statsText = String.format("Distance: %.1f Hurt: %d", distance, target.hurtTime);
            FontProcess.getFont("simp").drawString(statsText, (float) (x + 40), (float) (y + 22), textColor);

            // Spawn particles on hit
            if (target.hurtTime == 9 && !sentParticles) {
                for (int i = 0; i <= 15; i++) {
                    Particle particle = new Particle();
                    particle.init((float) (x + 20), (float) (y + 20),
                            (float) (((Math.random() - 0.5) * 2) * 1.4),
                            (float) (((Math.random() - 0.5) * 2) * 1.4),
                            (float) (Math.random() * 4),
                            i % 2 == 0 ? color1 : color2);
                    particles.add(particle);
                }
                sentParticles = true;
            }
            if (target.hurtTime == 8) sentParticles = false;
        }

        private static float lerp(float current, float target, float speed) {
            return current + (target - current) * speed;
        }

        private static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
            Gui.drawRect(left, top, right, bottom, startColor);
        }

        public static class Particle {
            public float x, y, adjustedX, adjustedY, deltaX, deltaY, size, opacity;
            public Color color;

            public void render2D() {
                Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (opacity));
                RenderUtils.drawRoundedRect(x + adjustedX, y + adjustedY, size, size, (size / 2f) - 0.5f, renderColor);
            }

            public void updatePosition() {
                for (int i = 1; i <= 2; i++) {
                    adjustedX += deltaX;
                    adjustedY += deltaY;
                    deltaY *= 0.97;
                    deltaX *= 0.97;
                    opacity -= 1f;
                    if (opacity < 1) opacity = 1;
                }
            }

            public void init(float x, float y, float deltaX, float deltaY, float size, Color color) {
                this.x = x;
                this.y = y;
                this.deltaX = deltaX;
                this.deltaY = deltaY;
                this.size = size;
                this.opacity = 254;
                this.color = color;
            }
        }
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
