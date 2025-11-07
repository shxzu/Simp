package cc.simp.interfaces.menu.alt.microsoft;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.interfaces.menu.alt.AltManagerGui;
import cc.simp.interfaces.menu.alt.SessionChanger;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

public class GuiLoginMicrosoft extends GuiScreen {
    private GuiTextField username, password;

    public static String statusString;
    public static boolean didTheThing = false;

    private final CustomFontRenderer titleFont;
    private final CustomFontRenderer buttonFont;
    private final CustomFontRenderer statusFont;

    private final int buttonWidth = 140;
    private final int buttonHeight = 25;
    private final int buttonSpacing = 8;

    public GuiLoginMicrosoft() {
        titleFont = FontProcess.getFont("simp");
        buttonFont = FontProcess.getFont("simp");
        statusFont = FontProcess.getFont("simp");
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0).getRGB());
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);

        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;

        titleFont.drawStringWithShadow("Microsoft Login", centerX - titleFont.getStringWidth("Microsoft Login") / 2, centerY - 80, ColorProcess.getColor().getRGB());

        this.username.drawTextBox();
        this.password.drawTextBox();

        if (!didTheThing) {
            statusString = "Email & Password";
            statusFont.drawStringWithShadow(statusString, centerX - statusFont.getStringWidth(statusString) / 2, centerY - 105, 0xFFFFFF);
        } else {
            statusString = "Logged Into: " + Minecraft.getMinecraft().getSession().getUsername() + "!";
            statusFont.drawStringWithShadow(statusString, centerX - statusFont.getStringWidth(statusString) / 2, centerY - 105, ColorProcess.getColor().getRGB());
        }

        drawCustomButtons(mouseX, mouseY, centerX, centerY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCustomButtons(int mouseX, int mouseY, int centerX, int centerY) {
        int startX = centerX - buttonWidth / 2;
        int startY = centerY + 10;

        drawButton(startX, startY, buttonWidth, buttonHeight, "login (pass)", mouseX, mouseY);
        drawButton(startX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "login (oauth)", mouseX, mouseY);
        drawButton(startX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight, "cancel", mouseX, mouseY);
    }

    private void drawButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);
        RenderUtils.drawRoundedRect(x, y, width, height, 6, true, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);
    }

    @Override
    public void initGui() {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;

        (this.username = new GuiTextField(100, this.fontRendererObj, centerX - buttonWidth / 2, centerY - 50, buttonWidth, 20)).setFocused(true);
        (this.password = new GuiTextField(101, this.fontRendererObj, centerX - buttonWidth / 2, centerY - 25, buttonWidth, 20)).setFocused(false);
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        try {
            super.keyTyped(character, key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (character == '\t') {
            if (this.username.isFocused()) {
                this.username.setFocused(false);
                this.password.setFocused(true);
            } else {
                this.username.setFocused(true);
                this.password.setFocused(false);
            }
        }
        if (character == '\r') {
            handlePasswordLogin();
        }

        if (didTheThing) {
            didTheThing = false;
        }

        this.username.textboxKeyTyped(character, key);
        this.password.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        try {
            super.mouseClicked(mouseX, mouseY, button);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username.mouseClicked(mouseX, mouseY, button);
        this.password.mouseClicked(mouseX, mouseY, button);

        final ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = this.width / 2;
        int centerY = sr.getScaledHeight() / 2;
        int startX = centerX - buttonWidth / 2;
        int startY = centerY + 10;

        if (isMouseOverButton(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight)) {
            handlePasswordLogin();
        } else if (isMouseOverButton(mouseX, mouseY, startX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight)) {
            handleOAuthLogin();
        } else if (isMouseOverButton(mouseX, mouseY, startX, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight)) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    private void handlePasswordLogin() {
        if (this.username.getText().isEmpty()) {
            statusString = "You need to enter an email!";
            didTheThing = false;
            return;
        }
        SessionChanger.getInstance().setUserMicrosoft(this.username.getText(), this.password.getText());
        saveAltToFile(this.username.getText(), this.password.getText(), Minecraft.getMinecraft().getSession().getUsername());
        didTheThing = true;
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
        this.password.updateCursorCounter();
    }

    private void handleOAuthLogin() {
        statusString = "Awaiting for response for Microsoft login...";
        CompletableFuture<Void> future = new CompletableFuture<>();

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                if (login.isGood()) {
                    mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                    saveOAuthAltToFile(login.username, login.newRefreshToken);
                    didTheThing = true;
                } else {
                    statusString = "Failed to login with Microsoft OAuth";
                    didTheThing = false;
                }
                future.complete(null);
            } else {
                statusString = "Failed to get refresh token";
                didTheThing = false;
                future.complete(null);
            }
        });

        try {
            future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAltToFile(String email, String password, String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoft|" + sessionUsername + "|" + email + "|" + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveOAuthAltToFile(String username, String refreshToken) {
        // Save to alts.txt
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File altsFile = new File(dir, "alts.txt");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(altsFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoftOAuth|" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save refresh token to tokens.txt
        File tokensFile = new File(dir, "tokens.txt");
        try (FileWriter fw = new FileWriter(tokensFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println(username + "|" + refreshToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Session createMsSession() {
        statusString = "Awaiting for response for Microsoft login...";
        CompletableFuture<Session> future = new CompletableFuture<>();
        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                System.out.println("Refresh token: " + refreshToken);
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                future.complete(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
            }
        });
        return future.join();
    }
}
