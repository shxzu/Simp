package cc.simp.ui.alt;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import cc.simp.ui.alt.microsoft.GuiLoginMicrosoft;
import cc.simp.utils.font.FontManager;
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
    private static final int SCROLLBAR_WIDTH = 2;

    private ArrayList<String> alts = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;

    private static final int BUTTON_COPY_CURRENT = 9000;

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        buttonList.clear();

        int visibleEntries = (BOX_HEIGHT - TOP_BOX_HEIGHT) / ENTRY_HEIGHT;
        maxScroll = Math.max(0, alts.size() - visibleEntries);

        buttonList.add(new GuiButton(BUTTON_COPY_CURRENT, BOX_X + BOX_WIDTH - 80, BOX_Y + 10, 70, 20, "Copy User"));

        int y = (this.height / 4 + 48) + (this.height / 8);
        this.buttonList.add(new GuiButton(0, (this.width / 2 - 100), y + 20 * 3, "Cancel"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, y + 20 * 1, "Use Cracked"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, y, "Use Microsoft"));

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

        drawRect(BOX_X, BOX_Y, BOX_X + BOX_WIDTH, BOX_Y + TOP_BOX_HEIGHT, new Color(30, 30, 30, 180).getRGB());
        FontManager.TAHOMA.drawString("Current User:", BOX_X + 5, BOX_Y + 5, Color.WHITE.getRGB());
        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        FontManager.TAHOMA.drawString(currentUser, BOX_X + 5, BOX_Y + 20, Color.LIGHT_GRAY.getRGB());

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
            FontManager.TAHOMA.drawString(altName, listX + 5, entryY + 8, Color.WHITE.getRGB());
            if (hovered) {
                drawRect(listX, entryY, listX + listWidth, entryY + ENTRY_HEIGHT, new Color(0, 0, 0, 200).getRGB());

                drawHoverButton(listX + listWidth - 180, entryY + 5, "Copy");
                drawHoverButton(listX + listWidth - 120, entryY + 5, "Login");
                drawHoverButton(listX + listWidth - 60, entryY + 5, "Delete");
            }

            
            
            
        }

        disableScissor();

        int scrollbarX = BOX_X + BOX_WIDTH - SCROLLBAR_WIDTH - 2;
        int scrollbarY = BOX_Y + TOP_BOX_HEIGHT;
        int scrollbarHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;

        drawRect(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight,
                new Color(50, 50, 50, 180).getRGB());

        int thumbHeight = Math.max(scrollbarHeight * visibleEntries / (alts.size() == 0 ? 1 : alts.size()), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / (maxScroll == 0 ? 1 : maxScroll);

        drawRect(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                new Color(100, 100, 100, 220).getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHoverButton(int x, int y, String label) {
        int w = 50, h = 20;
        drawRect(x, y, x + w, y + h, new Color(80, 80, 80, 220).getRGB());
        FontManager.TAHOMA.drawString(label, x + 8, y + 6, Color.WHITE.getRGB());
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
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_COPY_CURRENT) {
            String currentUser = Minecraft.getMinecraft().getSession().getUsername();
            copyToClipboard(currentUser);
        }
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
        if (button.id == 1) {
            mc.displayGuiScreen(new GuiLogin());
        }
        if (button.id == 2) {
            mc.displayGuiScreen(new GuiLoginMicrosoft());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int listX = BOX_X + 5;
        int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;
        int listHeight = BOX_HEIGHT - TOP_BOX_HEIGHT - 10;
        int visibleEntries = listHeight / ENTRY_HEIGHT;

        for (int i = 0; i < visibleEntries; i++) {
            int altIndex = i + scrollOffset;
            if (altIndex >= alts.size()) break;

            int entryY = listY + i * ENTRY_HEIGHT;

            int copyX = listX + listWidth - 180;
            int loginX = listX + listWidth - 120;
            int delX = listX + listWidth - 60;
            int btnY = entryY + 5;
            int btnW = 50, btnH = 20;

            if (mouseX >= copyX && mouseX <= copyX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                copyToClipboard(alts.get(altIndex));
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
            String[] parts = alt.split("\\|");
            if (parts.length >= 3) {
                String username = parts[1];
                String uuid = parts[2];
                String token = parts[3];
                Session auth = GuiLoginMicrosoft.createMsSession();
                if (auth != null) {
                    mc.session = auth;
                }
            }
        }
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
