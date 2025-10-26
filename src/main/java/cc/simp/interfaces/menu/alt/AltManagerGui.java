package cc.simp.interfaces.menu.alt;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.interfaces.menu.alt.microsoft.GuiLoginMicrosoft;
import cc.simp.interfaces.menu.alt.microsoft.MicrosoftOAuthTranslation;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.util.Session;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class AltManagerGui extends GuiScreen {

    private static final int BOX_X = 10;
    private static final int BOX_Y = 10;
    private static final int BOX_WIDTH = 200;
    private static final int BOX_HEIGHT = 300;
    private static final int ENTRY_HEIGHT = 30;
    private static final int TOP_BOX_HEIGHT = 40;
    private static final int SCROLLBAR_WIDTH = 4;

    private ArrayList<String> alts = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;

    private final CustomFontRenderer titleFont;
    private final CustomFontRenderer buttonFont;
    private final CustomFontRenderer altFont;

    private final int buttonWidth = 120;
    private final int buttonHeight = 25;
    private final int buttonSpacing = 8;

    public AltManagerGui() {
        titleFont = FontProcess.getFont("simp");
        buttonFont = FontProcess.getFont("simp");
        altFont = FontProcess.getFont("simp");
    }

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        buttonList.clear();

        int visibleEntries = (BOX_HEIGHT - TOP_BOX_HEIGHT) / ENTRY_HEIGHT;
        maxScroll = Math.max(0, alts.size() - visibleEntries);

        super.initGui();
    }

    private void loadAltsFromFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoft|") || line.startsWith("microsoftOAuth|"))) {
                    alts.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltsToFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "alts.txt");

        try (PrintWriter out = new PrintWriter(file)) {
            for (String alt : alts) {
                out.println(alt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0).getRGB());
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);

        // Draw current user box
        RenderUtils.drawRoundedRect(BOX_X, BOX_Y, BOX_WIDTH, TOP_BOX_HEIGHT, 6, true, new Color(40, 40, 40, 180));
        titleFont.drawStringWithShadow("Current User:", BOX_X + 8, BOX_Y + 6, 0xFFFFFF);
        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        altFont.drawStringWithShadow(currentUser, BOX_X + 8, BOX_Y + 20, ColorProcess.getColor().getRGB());

        // Draw copy button
        int copyBtnX = BOX_X + BOX_WIDTH - 65;
        int copyBtnY = BOX_Y + 10;
        drawCustomButton(copyBtnX, copyBtnY, 60, 20, "copy", mouseX, mouseY);

        int listX = BOX_X + 5;
        int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;
        int listHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;

        enableScissor(listX, listY, listWidth, listHeight);
        int visibleEntries = listHeight / ENTRY_HEIGHT;

        for (int i = 0; i < visibleEntries; i++) {
            int altIndex = i + scrollOffset;
            if (altIndex >= alts.size()) break;

            int entryY = listY + i * ENTRY_HEIGHT;
            String altName = alts.get(altIndex).split("\\|")[1];

            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

            if (hovered) {
                RenderUtils.drawRoundedRect(listX, entryY, listWidth, ENTRY_HEIGHT, 4, true, new Color(60, 60, 60, 150));
            }

            altFont.drawStringWithShadow(altName, listX + 5, entryY + 8, 0xFFFFFF);

            if (hovered) {
                drawHoverButton(listX + listWidth - 165, entryY + 5, "copy", mouseX, mouseY);
                drawHoverButton(listX + listWidth - 110, entryY + 5, "login", mouseX, mouseY);
                drawHoverButton(listX + listWidth - 55, entryY + 5, "delete", mouseX, mouseY);
            }
        }

        disableScissor();

        // Draw scrollbar
        int scrollbarX = BOX_X + BOX_WIDTH - SCROLLBAR_WIDTH - 2;
        int scrollbarY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int scrollbarHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;

        RenderUtils.drawRoundedRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, 2, true, new Color(50, 50, 50, 180));

        int thumbHeight = Math.max(scrollbarHeight * visibleEntries / (alts.size() == 0 ? 1 : alts.size()), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / (maxScroll == 0 ? 1 : maxScroll);

        RenderUtils.drawRoundedRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, 2, true, new Color(100, 100, 100, 220));

        // Draw action buttons
        drawActionButtons(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawActionButtons(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int startY = this.height - 100;

        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startX = centerX - totalWidth / 2;

        drawCustomButton(startX, startY, buttonWidth, buttonHeight, "cracked", mouseX, mouseY);
        drawCustomButton(startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight, "microsoft", mouseX, mouseY);
        drawCustomButton(startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight, "back", mouseX, mouseY);
    }

    private void drawCustomButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60, 200) : new Color(40, 40, 40, 180);
        RenderUtils.drawRoundedRect(x, y, width, height, 6, true, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);
    }

    private void drawHoverButton(int x, int y, String label, int mouseX, int mouseY) {
        int w = 50, h = 20;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        Color bgColor = hovered ? new Color(80, 80, 80, 240) : new Color(60, 60, 60, 200);
        RenderUtils.drawRoundedRect(x, y, w, h, 4, true, bgColor);

        int textX = x + (w - buttonFont.getStringWidth(label)) / 2;
        int textY = y + (h - buttonFont.getHeight()) / 2;
        buttonFont.drawString(label, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);
    }

    private void enableScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int scissorX = x * scale;
        int scissorY = (sr.getScaledHeight() - y - height) * scale;
        int scissorWidth = width * scale;
        int scissorHeight = height * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Copy current user button
        int copyBtnX = BOX_X + BOX_WIDTH - 65;
        int copyBtnY = BOX_Y + 10;
        if (mouseX >= copyBtnX && mouseX <= copyBtnX + 60 && mouseY >= copyBtnY && mouseY <= copyBtnY + 20) {
            String currentUser = Minecraft.getMinecraft().getSession().getUsername();
            copyToClipboard(currentUser);
            return;
        }

        // Action buttons
        int centerX = this.width / 2;
        int startY = this.height - 100;
        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startX = centerX - totalWidth / 2;

        if (isMouseOverButton(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight)) {
            mc.displayGuiScreen(new GuiLogin());
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight)) {
            mc.displayGuiScreen(new GuiLoginMicrosoft());
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight)) {
            mc.displayGuiScreen(new GuiMainMenu());
            return;
        }

        int listX = BOX_X + 5;
        int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;
        int listHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;
        int visibleEntries = listHeight / ENTRY_HEIGHT;

        for (int i = 0; i < visibleEntries; i++) {
            int altIndex = i + scrollOffset;
            if (altIndex >= alts.size()) break;

            int entryY = listY + i * ENTRY_HEIGHT;

            int copyX = listX + listWidth - 165;
            int loginX = listX + listWidth - 110;
            int delX = listX + listWidth - 55;
            int btnY = entryY + 5;
            int btnW = 50, btnH = 20;

            if (mouseX >= copyX && mouseX <= copyX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                copyToClipboard(alts.get(altIndex).split("\\|")[1]);
                return;
            }

            if (mouseX >= loginX && mouseX <= loginX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                loginWithAlt(alts.get(altIndex));
                return;
            }

            if (mouseX >= delX && mouseX <= delX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                alts.remove(altIndex);
                saveAltsToFile();
                return;
            }
        }

        int scrollbarX = BOX_X + BOX_WIDTH - SCROLLBAR_WIDTH - 2;
        int scrollbarY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int scrollbarHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
        }
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    private void loginWithAlt(String alt) {
        if (alt.startsWith("cracked|")) {
            String username = alt.split("\\|")[1];
            SessionChanger.getInstance().setUserOffline(username);
        } else if (alt.startsWith("microsoft|")) {
            String[] parts = alt.split("\\|");
            if (parts.length >= 3) {
                String email = parts[2];
                String pass = parts[3];
                SessionChanger.getInstance().setUserMicrosoft(email, pass);
            }
        } else if (alt.startsWith("microsoftOAuth|")) {
            String username = alt.split("\\|")[1];
            String refreshToken = loadRefreshToken(username);
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
            }
        }
    }


    private String loadRefreshToken(String username) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "simp");
        File file = new File(dir, "tokens.txt");

        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScrollbar) {
            int scrollbarHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;
            int deltaY = mouseY - dragStartY;

            int visibleEntries = scrollbarHeight / ENTRY_HEIGHT;
            int maxScrollLocal = Math.max(0, alts.size() - visibleEntries);

            if (maxScrollLocal > 0) {
                int scrollRange = scrollbarHeight
                        - Math.max(scrollbarHeight * visibleEntries / (alts.size() == 0 ? 1 : alts.size()), 20);
                int scrollDelta = deltaY * maxScrollLocal / scrollRange;
                scrollOffset = Math.min(maxScrollLocal, Math.max(0, scrollStart + scrollDelta));
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int visibleEntries = (BOX_HEIGHT - TOP_BOX_HEIGHT - 10) / ENTRY_HEIGHT;
            int maxScrollLocal = Math.max(0, alts.size() - visibleEntries);
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (wheel < 0) {
                scrollOffset = Math.min(maxScrollLocal, scrollOffset + 1);
            }
        }
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
