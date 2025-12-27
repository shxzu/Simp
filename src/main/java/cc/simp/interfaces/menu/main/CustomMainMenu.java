package cc.simp.interfaces.menu.main;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.interfaces.menu.alt.AltManagerGui;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.net.URI;

import java.awt.*;
import java.io.IOException;

public class CustomMainMenu extends GuiScreen {

    private final ResourceLocation backgroundImage;
    private final ResourceLocation logoImage;
    private final CustomFontRenderer buttonFont;
    private final CustomFontRenderer changelogFont;
    private final CustomFontRenderer timeFont;

    private final int buttonWidth = 120;
    private final int buttonHeight = 30;
    private final int buttonSpacing = 8;
    private final int buttonsYOffset = 60;

    private final long startTime;
    ArrayList<String> changelogEntries;

    public CustomMainMenu() {
        backgroundImage = new ResourceLocation("simp/images/mainmenu.jpg");
        logoImage = new ResourceLocation("simp/images/simp_light.png");
        startTime = System.currentTimeMillis();
        buttonFont = FontProcess.getFont("simp");
        changelogFont = FontProcess.getFont("simp");
        timeFont = FontProcess.getFont("big");
    }

    @Override
    public void initGui() {

        try {
            changelogEntries = new ArrayList<>(
                    fetchLatestCommitMessages("shxzu", "Simp", 4)
            );
        } catch (IOException e) {

            changelogEntries = new ArrayList<>();

            if ("HTTP_403".equals(e.getMessage())) {
                changelogEntries.add("403: Github Ratelimited");
            } else {
                changelogEntries.add("Failed to load");
            }

        } catch (Exception e) {
            changelogEntries = new ArrayList<>();
            changelogEntries.add("unexpected error");
        }

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
        timeFont.drawStringWithShadow(currentTime, (float) timeX, (float) timeY, 0xFFFFFF);

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

        drawButton(startX, startY, buttonWidth, buttonHeight, "singleplayer", mouseX, mouseY);

        drawButton(startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight, "multiplayer", mouseX, mouseY);

        drawButton(startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight, "quit", mouseX, mouseY);

        drawButton(startX + (buttonWidth + buttonSpacing), startY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight, "alts", mouseX, mouseY);
    }

    private void drawButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        long time = System.currentTimeMillis() - startTime;
        long weightX = 100L, weightY = 600L, speed = 3000L;
        float normal = 3000f;
        float hue = ((time + (x*weightX) + (y * weightY)) % speed) / normal;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);

        RenderUtils.drawRect(x, y - 1, width, 1, Color.getHSBColor(hue, 0.5f, 0.95f));
        RenderUtils.drawRect(x, y, width, height, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, 0xFFFFFF);

    }

    private void drawChangelog() {
        int x = 20;
        int y = 20;
        int width = 170;

        long time = System.currentTimeMillis() - startTime;
        float baseHue = (time % 3000) / 3000.0f;

        int fontHeight = changelogFont.getHeight();
        int titleOffset = fontHeight + 14;
        int entrySpacing = fontHeight + 4;

        int totalHeight = titleOffset + changelogEntries.size() * entrySpacing + 2;

        RenderUtils.drawRoundedRect(
                x,
                y,
                width,
                totalHeight,
                8,
                true,
                new Color(20, 25, 30, 120)
        );

        changelogFont.drawStringWithShadow(
                "changelog",
                x + 8,
                y + 6,
                Color.getHSBColor(baseHue, 0.8f, 1.0f).getRGB()
        );

        int entryY = y + titleOffset;

        for (int i = 0; i < changelogEntries.size(); i++) {
            String text = changelogEntries.get(i).toLowerCase();
            if (text.length() > 20) text = text.substring(0, 20);

            float entryHue = ((time + i * 500) % 3000) / 3000.0f;

            changelogFont.drawStringWithShadow(
                    text,
                    x + 8,
                    entryY,
                    Color.getHSBColor(entryHue, 0.5f, 0.95f).getRGB()
            );

            entryY += entrySpacing;
        }
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

            if (isMouseOverButton(mouseX, mouseY, startX + (buttonWidth + buttonSpacing), startY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight)) {
                mc.displayGuiScreen(new AltManagerGui());
            }
        }
    }

    public static ArrayList<String> fetchLatestCommitMessages(
            String owner,
            String repo,
            int limit
    ) throws Exception {

        String url = "https://api.github.com/repos/"
                + owner + "/" + repo + "/commits?per_page=" + limit;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(3))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Java-GitHub-Client")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status != 200) {
            throw new IOException("HTTP_" + status);
        }

        JsonArray commits =
                JsonParser.parseString(response.body()).getAsJsonArray();

        ArrayList<String> messages = new ArrayList<>(commits.size());

        for (int i = 0; i < commits.size(); i++) {
            JsonObject commitObj = commits.get(i).getAsJsonObject();
            JsonObject commit = commitObj.getAsJsonObject("commit");

            messages.add(commit.get("message").getAsString());
        }

        return messages;
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