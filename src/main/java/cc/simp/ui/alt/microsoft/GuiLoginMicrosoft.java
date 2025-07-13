package cc.simp.ui.alt.microsoft;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

import cc.simp.ui.alt.AltManagerGui;
import cc.simp.ui.alt.SessionChanger;
import cc.simp.utils.client.font.FontManager;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

public class GuiLoginMicrosoft extends GuiScreen {
    private GuiTextField username, password;

    public String statusString;

    public static boolean didTheThing = false;

    @Override
    protected void actionPerformed(final GuiButton button) {
        if (button.id == 0) {
            if (this.username.getText().equals("")) {
                MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
                try {
                    MicrosoftAuthResult result = authenticator.loginWithWebview();
                } catch (MicrosoftAuthenticationException e) {
                    e.printStackTrace();
                }
            } else {
                SessionChanger.getInstance().setUserMicrosoft(this.username.getText(), this.password.getText());
                saveAltToFile(this.username.getText(), this.password.getText(), Minecraft.getMinecraft().session.getUsername());
                didTheThing = true;
            }

        }
        if (button.id == 1) {
            Session auth = this.createMsSession();
            if (auth == null) {
                didTheThing = false;
                return;
            } else {
                mc.session = auth;
                didTheThing = true;
            }
        }
        if (button.id == 2) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    public void drawScreen(final int x2, final int y2, final float z2) {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0).getRGB());
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        this.username.drawTextBox();
        this.password.drawTextBox();
        GuiLoginMicrosoft.drawCustomCenteredString(FontManager.TAHOMA, statusString, (int) (this.width / 2), (int) (sr.getScaledHeight() / 2 - 65), -1);
        if (!didTheThing) {
            statusString = "Email & Password";
        } else {
            statusString = "Logged Into: " + Minecraft.getMinecraft().session.getUsername() + "!";
        }
        super.drawScreen(x2, y2, z2);
    }

    @Override
    public void initGui() {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 50 - 10, this.height / 2, 120, 20, I18n.format("Login Microsoft", new Object[0])));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 50 - 10, this.height / 2 + 20, 120, 20, I18n.format("Login OAuth", new Object[0])));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 50 - 10, this.height / 2 + 80, 120, 20, I18n.format("Cancel", new Object[0])));
        (this.username = new GuiTextField(100, this.minecraftFontRendererObj, this.width / 2 - 50 - 10, sr.getScaledHeight() / 2 - 50, 120, 20)).setFocused(true);
        (this.password = new GuiTextField(100, this.minecraftFontRendererObj, this.width / 2 - 50 - 10, sr.getScaledHeight() / 2 - 25, 120, 20)).setFocused(false);
        Keyboard.enableRepeatEvents(true);
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
            this.password.setFocused(false);
        }
        if (character == '\t' && !this.password.isFocused()) {
            this.password.setFocused(true);
            this.username.setFocused(false);
        }
        if (character == '\r') {
            this.actionPerformed(this.buttonList.get(0));
        }

        if (didTheThing) {
            didTheThing = false;
        }

        this.username.textboxKeyTyped(character, key);
        this.password.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int x2, final int y2, final int button) {
        try {
            super.mouseClicked(x2, y2, button);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username.mouseClicked(x2, y2, button);
        this.password.mouseClicked(x2, y2, button);
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

    private void saveAltToFile(String email, String password, String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoft|" + sessionUsername + "|" + email + "|" + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltToFileOAuth(String username, String uuid, String token) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoftOAuth|" + username + "|" + uuid + "|" + token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Session createMsSession() {
        statusString = "Awaiting for response for Microsoft login...";
        CompletableFuture<Session> future = new CompletableFuture<>();
        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                System.out.println("Refresh token: " + refreshToken);
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                future.complete(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                saveAltToFileOAuth(login.username, login.uuid, login.mcToken);
            }
        });
        return future.join();
    }

}
