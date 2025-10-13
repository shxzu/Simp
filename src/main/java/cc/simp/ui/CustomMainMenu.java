package cc.simp.ui;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.awt.*;
import java.io.IOException;

public class CustomMainMenu extends GuiScreen {

    private ResourceLocation backgroundImage;
    private ResourceLocation logoImage;
    private CustomFontRenderer titleFont;
    private CustomFontRenderer buttonFont;
    private CustomFontRenderer changelogFont;
    private CustomFontRenderer timeFont;

    private int buttonWidth = 120;
    private int buttonHeight = 30;
    private int buttonSpacing = 8;
    private int altButtonWidth = 120;
    private int altButtonHeight = 25;

    private int buttonsYOffset = 60;
    private int logoYOffset = -150;
    private int timeYOffset = -210;

    private long startTime;
    private String[] changelogEntries = {
            "- Added Custom MainMenu",
            "- Added FontRendering",
            "- Added Some Skibidi Bypasses",
            "- Added Some other shit"
    };

    public CustomMainMenu() {
        backgroundImage = new ResourceLocation("simp/images/mainmenu.jpg");
        logoImage = new ResourceLocation("simp/images/simp.png");

        try {
            titleFont = new CustomFontRenderer("simp", 32, Font.BOLD, true, true);
            buttonFont = new CustomFontRenderer("apple", 18, Font.PLAIN, true, true);
            changelogFont = new CustomFontRenderer("simp", 16, Font.PLAIN, true, true);
            timeFont = new CustomFontRenderer("simp", 56, Font.BOLD, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        startTime = System.currentTimeMillis();
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableAlpha();

        drawBackgroundImage();

        GlStateManager.enableAlpha();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());

        float heightScale = this.height / 480.0f;
        int scaledTimeYOffset = (int)(timeYOffset * Math.min(heightScale, 1.2f));

        int timeX = (this.width - timeFont.getStringWidth(currentTime)) / 2;
        int timeY = this.height / 2 + scaledTimeYOffset;
        timeFont.drawStringWithShadow(currentTime, timeX, timeY, 0xFFFFFF);

        drawLogo();

        drawCustomButtons(mouseX, mouseY);

        drawChangelog();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackgroundImage() {
        mc.getTextureManager().bindTexture(backgroundImage);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(0, this.height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(this.width, this.height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(this.width, 0, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(0, 0, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
    }

    private void drawLogo() {
        int logoWidth = 200;
        int logoHeight = 200;
        int logoX = (this.width - logoWidth) / 2;
        int logoY = this.height / 2 + logoYOffset;

        mc.getTextureManager().bindTexture(logoImage);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawTexturedRect(logoX, logoY, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);
        GlStateManager.disableBlend();
    }

    private void drawCustomButtons(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int startY = this.height / 2 + buttonsYOffset;

        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startX = centerX - totalWidth / 2;

        boolean singlePlayerHover = drawButton(startX, startY, buttonWidth, buttonHeight, "Singleplayer", mouseX, mouseY);

        boolean multiPlayerHover = drawButton(startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight, "Multiplayer", mouseX, mouseY);

        boolean quitHover = drawButton(startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight, "Quit", mouseX, mouseY);

        int altButtonX = startX + buttonWidth + buttonSpacing + (buttonWidth - altButtonWidth) / 2;
        int altButtonY = startY + buttonHeight + buttonSpacing;
        boolean altManagerHover = drawButton(altButtonX, altButtonY, altButtonWidth, altButtonHeight, "AltManager", mouseX, mouseY);
    }

    private boolean drawButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);
        RenderUtils.drawRoundedRect(x, y, width, height, 6, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);

        return hovered;
    }

    private void drawChangelog() {
        int changelogWidth = 170;
        int changelogX = 20;
        int changelogY = 20;

        long time = System.currentTimeMillis() - startTime;
        float hue = (time % 3000) / 3000.0f;
        Color titleColor = Color.getHSBColor(hue, 0.8f, 1.0f);

        changelogFont.drawStringWithShadow("Changelog", changelogX + 8, changelogY + 6, titleColor.getRGB());

        int entryY = changelogY + changelogFont.getHeight() + 14;
        for (int i = 0; i < changelogEntries.length; i++) {
            float entryHue = ((time + i * 500) % 3000) / 3000.0f;
            Color entryColor = Color.getHSBColor(entryHue, 0.7f, 0.95f);

            changelogFont.drawStringWithShadow(changelogEntries[i], changelogX + 8, entryY, entryColor.getRGB());
            entryY += changelogFont.getHeight() + 4;
        }

        int bgHeight = entryY - changelogY + 6;
        RenderUtils.drawRoundedRect(changelogX, changelogY, changelogWidth, bgHeight, 8, new Color(20, 25, 30, 120));
    }

    private void drawRoundedRect(int x, int y, int width, int height, float radius, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);

        Gui.drawRect(x + (int)radius, y, x + width - (int)radius, y + height, color);
        Gui.drawRect(x, y + (int)radius, x + (int)radius, y + height - (int)radius, color);
        Gui.drawRect(x + width - (int)radius, y + (int)radius, x + width, y + height - (int)radius, color);

        drawCircle(x + radius, y + radius, radius, color);
        drawCircle(x + width - radius, y + radius, radius, color);
        drawCircle(x + radius, y + height - radius, radius, color);
        drawCircle(x + width - radius, y + height - radius, radius, color);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawRoundedRectBorder(int x, int y, int width, int height, float radius, float borderWidth, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(borderWidth);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        GL11.glVertex2d(x + radius, y);
        GL11.glVertex2d(x + width - radius, y);

        for (int i = 0; i <= 10; i++) {
            double angle = (i * 90.0 / 10) * Math.PI / 180.0;
            GL11.glVertex2d(x + width - radius + Math.cos(angle) * radius,
                    y + radius - Math.sin(angle) * radius);
        }

        GL11.glVertex2d(x + width, y + radius);
        GL11.glVertex2d(x + width, y + height - radius);

        for (int i = 0; i <= 10; i++) {
            double angle = (i * 90.0 / 10) * Math.PI / 180.0;
            GL11.glVertex2d(x + width - radius + Math.sin(angle) * radius,
                    y + height - radius + Math.cos(angle) * radius);
        }

        GL11.glVertex2d(x + width - radius, y + height);
        GL11.glVertex2d(x + radius, y + height);

        for (int i = 0; i <= 10; i++) {
            double angle = (i * 90.0 / 10) * Math.PI / 180.0;
            GL11.glVertex2d(x + radius - Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius);
        }

        GL11.glVertex2d(x, y + height - radius);
        GL11.glVertex2d(x, y + radius);

        for (int i = 0; i <= 10; i++) {
            double angle = (i * 90.0 / 10) * Math.PI / 180.0;
            GL11.glVertex2d(x + radius - Math.sin(angle) * radius,
                    y + radius - Math.cos(angle) * radius);
        }

        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawCircle(float x, float y, float radius, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(x, y);

        for (int i = 0; i <= 360; i += 6) {
            double angle = i * Math.PI / 180.0;
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }

        GL11.glEnd();
    }

    private void drawTexturedRect(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double)x, (double)(y + height), 0.0D).tex((double)(u * f), (double)((v + (float)height) * f1)).endVertex();
        worldrenderer.pos((double)(x + width), (double)(y + height), 0.0D).tex((double)((u + (float)width) * f), (double)((v + (float)height) * f1)).endVertex();
        worldrenderer.pos((double)(x + width), (double)y, 0.0D).tex((double)((u + (float)width) * f), (double)(v * f1)).endVertex();
        worldrenderer.pos((double)x, (double)y, 0.0D).tex((double)(u * f), (double)(v * f1)).endVertex();
        tessellator.draw();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            int centerX = this.width / 2;
            int startY = this.height / 2 + buttonsYOffset;

            int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
            int startX = centerX - totalWidth / 2;

            if (isMouseOverButton(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight)) {
                mc.displayGuiScreen(new GuiSelectWorld(this));
            }

            if (isMouseOverButton(mouseX, mouseY, startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight)) {
                mc.displayGuiScreen(new GuiMultiplayer(this));
            }

            if (isMouseOverButton(mouseX, mouseY, startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight)) {
                mc.shutdown();
            }

            int altButtonX = startX + buttonWidth + buttonSpacing + (buttonWidth - altButtonWidth) / 2;
            int altButtonY = startY + buttonHeight + buttonSpacing;
            if (isMouseOverButton(mouseX, mouseY, altButtonX, altButtonY, altButtonWidth, altButtonHeight)) {
            }
        }
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}