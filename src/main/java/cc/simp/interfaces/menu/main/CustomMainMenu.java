package cc.simp.interfaces.menu.main;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.interfaces.menu.alt.AltManagerGui;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

import java.awt.*;

public class CustomMainMenu extends GuiScreen {

    private final ResourceLocation backgroundImage;
    private final ResourceLocation logoImage;
    private final CustomFontRenderer buttonFont;
    private final CustomFontRenderer changelogFont;
    private final CustomFontRenderer timeFont;

    private final int buttonWidth = 120;
    private final int buttonHeight = 30;
    private final int buttonSpacing = 8;
    private final int altButtonWidth = 120;
    private final int altButtonHeight = 25;

    private final int buttonsYOffset = 60;

    private final long startTime;
    private final String[] changelogEntries;

    public CustomMainMenu() {
        backgroundImage = new ResourceLocation("simp/images/mainmenu.png");
        logoImage = new ResourceLocation("simp/images/simp.png");
        startTime = System.currentTimeMillis();
        buttonFont = FontProcess.getFont("simp");
        changelogFont = FontProcess.getFont("arial");
        timeFont = FontProcess.getFont("bold");

        this.changelogEntries = fetchCommits(4);
    }

    private String[] fetchCommits(int count) {
        List<String> entries = new ArrayList<>();
        entries.add("- Failed to fetch commits.");

        final int MAX_MSG_LENGTH = 40;

        try {
            String apiURL = "https://api.github.com/repos/shxzu/Simp/commits?per_page=" + count;
            URL url = new URL(apiURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Simp-Minecraft-Client");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

                String responseBody = new Scanner(connection.getInputStream()).useDelimiter("\\A").next();

                Pattern commitPattern = Pattern.compile(
                        "\"commit\":\\{.*?\"message\":\"(.*?)\".*?\"author\":\\{.*?\"name\":\"(.*?)\"",
                        Pattern.DOTALL
                );
                Matcher matcher = commitPattern.matcher(responseBody);

                entries.clear();
                while (matcher.find()) {
                    String message = matcher.group(1).split("\\n")[0].trim();
                    String author = matcher.group(2).trim();

                    message = message
                            .replaceAll("\\\\n", " ") // Replace \n with space
                            .replaceAll("\\\\r", "") // Remove \r
                            .replaceAll("\\\\\"", "\"") // Unescape quotes
                            .replaceAll("^Commit #\\d+ ", "") // Remove "Commit #N " prefix
                            .trim();

                    if (message.isEmpty()) {
                        message = "No commit message";
                    }

                    if (message.length() > MAX_MSG_LENGTH) {
                        message = message.substring(0, MAX_MSG_LENGTH) + "...";
                    }

                    entries.add("- " + message + " (" + author + ")");
                }

                if (entries.isEmpty()) {
                    entries.add("- No commits found or list is empty.");
                }
            } else {
                entries.add("- GitHub API request failed: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching commits: " + e.getMessage());
            entries.add("- Error fetching: " + e.getClass().getSimpleName());
        }

        return entries.toArray(new String[0]);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableAlpha();

        RenderUtils.drawImage(backgroundImage, 0, 0, this.width, this.height);

        GlStateManager.enableAlpha();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());

        float heightScale = this.height / 480.0f;
        int timeYOffset = -210;
        int scaledTimeYOffset = (int)(timeYOffset * Math.min(heightScale, 1.2f));

        int timeX = (this.width - timeFont.getStringWidth(currentTime)) / 2;
        int timeY = this.height / 2 + scaledTimeYOffset;
        timeFont.drawStringWithShadow(currentTime, timeX, timeY, 0xFFFFFF);

        RenderUtils.drawImage(logoImage, (float) width / 2 - (float) 157 / 2, height / 10f, 157, 125);

        drawCustomButtons(mouseX, mouseY);

        drawChangelog();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCustomButtons(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int startY = this.height / 2 + buttonsYOffset;

        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startX = centerX - totalWidth / 2;

        drawButton(startX, startY, buttonWidth, buttonHeight, "Singleplayer", mouseX, mouseY);

        drawButton(startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight, "Multiplayer", mouseX, mouseY);

        drawButton(startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight, "Quit", mouseX, mouseY);

        int altButtonX = startX + buttonWidth + buttonSpacing + (buttonWidth - altButtonWidth) / 2;
        int altButtonY = startY + buttonHeight + buttonSpacing;
        drawButton(altButtonX, altButtonY, altButtonWidth, altButtonHeight, "AltManager", mouseX, mouseY);
    }

    private void drawButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);
        RenderUtils.drawRoundedRect(x, y, width, height, 6, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);

    }

    private void drawChangelog() {
        int changelogX = 20;
        int changelogY = 20;
        float changelogWidth = 200;

        long time = System.currentTimeMillis() - startTime;
        float hue = (time % 3000) / 3000.0f;
        Color titleColor = Color.getHSBColor(hue, 0.8f, 1.0f);

        changelogFont.drawStringWithShadow("Recent Commits", changelogX + 8, changelogY + 6, titleColor.getRGB());

        int entryY = changelogY + changelogFont.getHeight() + 14;

        for (int i = 0; i < changelogEntries.length; i++) {
            float entryHue = ((time + i * 500) % 3000) / 3000.0f;
            Color entryColor = Color.getHSBColor(entryHue, 0.7f, 0.95f);

            changelogFont.drawStringWithShadow(changelogEntries[i], changelogX + 8, entryY, entryColor.getRGB());
            entryY += changelogFont.getHeight() + 4;
            changelogWidth = (float) changelogFont.getStringWidth(changelogEntries[i]) + 10;
        }

        int bgHeight = entryY - changelogY + 6;
        RenderUtils.drawRoundedRect(changelogX, changelogY, changelogWidth, bgHeight, 8, new Color(20, 25, 30, 120));
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
                mc.displayGuiScreen(new AltManagerGui());
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