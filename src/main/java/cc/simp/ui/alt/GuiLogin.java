package cc.simp.ui.alt;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;

import cc.simp.utils.client.font.FontManager;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

public class GuiLogin extends GuiScreen {
    private GuiTextField username;

    @Override
    protected void actionPerformed(final GuiButton button) {
        if (button.id == 0) {
            if(this.username.getText().equals("")) {
                this.mc.displayGuiScreen(new GuiLogin());
            } else {
                SessionChanger.getInstance().setUserOffline(this.username.getText());
                saveAltToFile(this.username.getText());
            }

        } else if (button.id == 1) {
        	this.mc.displayGuiScreen(new AltManagerGui());
        } else if (button.id == 2) {
        	String text = generateRandomString();
        	SessionChanger.getInstance().setUserOffline(text);
        	saveAltToFile(text);
        	this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    public void drawScreen(final int x2, final int y2, final float z2) {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0).getRGB());
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        this.username.drawTextBox();
        Gui.drawCustomCenteredString(FontManager.TAHOMA, "Username", (int)(this.width / 2), (int)(sr.getScaledHeight() / 2 - 65), -1);
        super.drawScreen(x2, y2, z2);
    }

    @Override
    public void initGui() {
        final ScaledResolution sr = new ScaledResolution(this.mc);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 50 - 10, this.height / 2 - 20, 120, 20, I18n.format("Login (Cracked)", new Object[0])));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 50 - 10, this.height / 2, 120, 20, I18n.format("Gen Cracked", new Object[0])));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 50 - 10, this.height / 2 + 20, 120, 20, I18n.format("Cancel", new Object[0])));
        (this.username = new GuiTextField(100, this.minecraftFontRendererObj, this.width / 2 - 50 - 10, sr.getScaledHeight() / 2 - 50, 120, 20)).setFocused(true);
        Keyboard.enableRepeatEvents(true);
    }
    
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();
        
        // Add 4 letters
        for (int i = 0; i < 4; i++) {
            result.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

     // Add 4 numbers
        for (int i = 0; i < 4; i++) {
            result.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }

        // Optionally, shuffle the string if you don't want a fixed pattern
        // result = new StringBuilder(result.toString()).reverse(); // Example: simple reversal, use more complex shuffling if needed

        return result.toString();
    }
   


    @Override
    protected void keyTyped(final char character, final int key) {
        try {
            super.keyTyped(character, key);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (character == '\t' && !this.username.isFocused()) {
            this.username.setFocused(true);
        }
        if (character == '\r') {
            this.actionPerformed(this.buttonList.get(0));
        }
        this.username.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int x2, final int y2, final int button) {
        try {
            super.mouseClicked(x2, y2, button);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.username.mouseClicked(x2, y2, button);
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
    	File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("cracked|" + sessionUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}