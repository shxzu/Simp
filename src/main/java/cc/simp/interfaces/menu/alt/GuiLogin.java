package cc.simp.interfaces.menu.alt;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;

public class GuiLogin extends GuiScreen {
    private GuiTextField username;

    private final CustomFontRenderer titleFont;
    private final CustomFontRenderer buttonFont;

    private final int buttonWidth = 140;
    private final int buttonHeight = 25;
    private final int buttonSpacing = 8;

    public GuiLogin() {
        titleFont = FontProcess.getFont("simp");
        buttonFont = FontProcess.getFont("simp");
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0).getRGB());
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);

        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;

        titleFont.drawStringWithShadow("Cracked Login", centerX - titleFont.getStringWidth("Cracked Login") / 2, centerY - 80, ColorProcess.getColor().getRGB());

        this.username.drawTextBox();

        drawCustomButtons(mouseX, mouseY, centerX, centerY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCustomButtons(int mouseX, int mouseY, int centerX, int centerY) {
        int startX = centerX - buttonWidth / 2;
        int startY = centerY + 10;

        drawButton(startX, startY, buttonWidth, buttonHeight, "login", mouseX, mouseY);
        drawButton(startX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "random", mouseX, mouseY);
        drawButton(startX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight, "cancel", mouseX, mouseY);
    }

    private void drawButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);

        RenderUtils.drawRect(x, y - 1, width, 1, ColorProcess.getColor());
        RenderUtils.drawRect(x, y, width, height, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);
    }

    @Override
    public void initGui() {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;

        (this.username = new GuiTextField(100, this.fontRendererObj, centerX - buttonWidth / 2, centerY - 30, buttonWidth, 20)).setFocused(true);
        Keyboard.enableRepeatEvents(true);
    }

    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            result.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        for (int i = 0; i < 4; i++) {
            result.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }

        return result.toString();
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        try {
            super.keyTyped(character, key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (character == '\t' && !this.username.isFocused()) {
            this.username.setFocused(true);
        }
        if (character == '\r') {
            handleLogin();
        }
        this.username.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        try {
            super.mouseClicked(mouseX, mouseY, button);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username.mouseClicked(mouseX, mouseY, button);

        final ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;
        int startX = centerX - buttonWidth / 2;
        int startY = centerY + 10;

        if (isMouseOverButton(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight)) {
            handleLogin();
        } else if (isMouseOverButton(mouseX, mouseY, startX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight)) {
            String text = generateRandomString();
            SessionChanger.getInstance().setUserOffline(text);
            saveAltToFile(text);
            this.mc.displayGuiScreen(new AltManagerGui());
        } else if (isMouseOverButton(mouseX, mouseY, startX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight)) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    private void handleLogin() {
        if (this.username.getText().equals("")) {
            this.mc.displayGuiScreen(new GuiLogin());
        } else {
            SessionChanger.getInstance().setUserOffline(this.username.getText());
            saveAltToFile(this.username.getText());
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    @Override
    public void onGuiClosed() {
        mc.entityRenderer.loadEntityShader(null);
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.username.updateCursorCounter();
    }

    private void saveAltToFile(String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("cracked|" + sessionUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
