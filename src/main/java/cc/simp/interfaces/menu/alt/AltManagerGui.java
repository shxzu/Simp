package cc.simp.interfaces.menu.alt;


import cc.simp.api.font.CustomFontRenderer;
import cc.simp.interfaces.menu.alt.microsoft.MicrosoftOAuthTranslation;
import cc.simp.utils.client.BgUtils;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.FontUtils;
import cc.simp.utils.render.GlUtils;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AltManagerGui extends GuiScreen {

    private int INFO_HEIGHT;
    private int LOGIN_WIDTH;
    private int LOGIN_HEIGHT;
    private int LOGIN_Y;
    private int BOX_X;
    private int BOX_Y;
    private int BOX_WIDTH;
    private static final int BOX_HEIGHT = 300;
    private static final int ENTRY_PADDING = 2;
    private int ENTRY_HEIGHT;
    private static final int TOP_BOX_HEIGHT = 40;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int PADDING = 6;
    private static int BOX_ACCENT_SIZE = 1;
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private CustomTextBox username, password;
    private ArrayList<String> alts = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;
    private String statusString;
    private boolean isLoggingIn = false;

    private final CustomFontRenderer titleFont;
    private final CustomFontRenderer buttonFont;
    private final CustomFontRenderer infoFont;
    private final CustomFontRenderer altFont;
    private final long startTime;

    private int buttonWidth;
    private int buttonHeight;
    private final int buttonSpacing = 8;

    public AltManagerGui() {
        startTime = System.currentTimeMillis();
        titleFont = FontUtils.getFont("semi-big");
        buttonFont = FontUtils.getFont("simp");
        altFont = FontUtils.getFont("simp");
        infoFont = FontUtils.getFont("small");
    }

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        selectedAlts.clear();
        buttonList.clear();

        username = new CustomTextBox(
                0,
                altFont,
                0,
                0,
                0,
                20
        );
        password = new CustomTextBox(
                0,
                altFont,
                0,
                0,
                0,
                20
        );


        super.initGui();
    }

    private void loadAltsFromFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
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
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
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
        BOX_WIDTH = (int) (this.width * 0.6);
        BOX_X = this.width - BOX_WIDTH - PADDING - BOX_ACCENT_SIZE;
        BOX_Y = PADDING + BOX_ACCENT_SIZE;
        LOGIN_HEIGHT = (int) (this.height * 0.4) - PADDING * 2;
        LOGIN_Y = (this.height / 2) - (LOGIN_HEIGHT / 2);
        LOGIN_WIDTH = this.width - BOX_WIDTH - (PADDING * 3);
        INFO_HEIGHT = (this.height - LOGIN_HEIGHT) / 2 - PADDING * 2;
        buttonWidth = (LOGIN_WIDTH - (PADDING * 2) - buttonSpacing * 2) / 3;
        buttonHeight = (int) (LOGIN_HEIGHT * 0.15);
        int titleBottom = LOGIN_Y + PADDING * 2 + titleFont.getHeight();
        int buttonsTop = LOGIN_Y + LOGIN_HEIGHT - buttonHeight - PADDING * 2;
        int textFeildHeight = (buttonsTop - titleBottom - PADDING) / 3;
        ENTRY_HEIGHT = ((BOX_HEIGHT - TOP_BOX_HEIGHT - 10) / 5) - PADDING * 3 + ENTRY_PADDING * 2;

        username.setPlaceholder("Username");
        username.xPosition = PADDING * 2;
        username.yPosition = LOGIN_Y + PADDING * 2 + titleFont.getHeight();
        username.setWidth(LOGIN_WIDTH - PADDING * 2);
        username.setHeight(textFeildHeight);

        password.setPlaceholder("Password");
        password.xPosition = PADDING * 2;
        password.yPosition = LOGIN_Y + PADDING * 3 + titleFont.getHeight() + textFeildHeight;
        password.setWidth(LOGIN_WIDTH - PADDING * 2);
        password.setHeight(textFeildHeight);

        RenderUtils.drawImage(BgUtils.getInstance().getCurrentBackground(), 0, 0, this.width, this.height);
        Gui.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 130).getRGB());

        // Draw current user box
        RenderUtils.drawRect(BOX_X + BOX_WIDTH, BOX_Y - BOX_ACCENT_SIZE, BOX_ACCENT_SIZE, TOP_BOX_HEIGHT / 3, getHueColorAt(BOX_X + BOX_WIDTH - (BOX_WIDTH / 3), BOX_Y - 2));
        RenderUtils.drawRect(BOX_X + BOX_WIDTH - (BOX_WIDTH / 3f), BOX_Y - BOX_ACCENT_SIZE, BOX_WIDTH / 3f, BOX_ACCENT_SIZE, getHueColorAt(BOX_X + BOX_WIDTH - (BOX_WIDTH / 3), BOX_Y - 2));
        RenderUtils.drawRect(BOX_X, BOX_Y, BOX_WIDTH, TOP_BOX_HEIGHT, new Color(22, 22, 22, 140));
        altFont.drawStringWithShadow("Current User:", BOX_X + 8, BOX_Y + 6, 0xFFFFFF);
        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        altFont.drawStringWithShadow(currentUser, BOX_X + 8, BOX_Y + 20, ColorProcess.getColor().getRGB());

        drawAltSwitcher(mouseX, mouseY);

        drawLoginBox(mouseX, mouseY);
        drawInfoBoxes(mouseX, mouseY);


        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawLoginBox(int mouseX, int mouseY) {
        RenderUtils.drawRect(PADDING, LOGIN_Y + (LOGIN_HEIGHT / 4f), BOX_ACCENT_SIZE, LOGIN_HEIGHT / 2f, getHueColorAt(PADDING - BOX_ACCENT_SIZE, LOGIN_Y + (LOGIN_HEIGHT / 2)));
        RenderUtils.drawRect(PADDING + BOX_ACCENT_SIZE, LOGIN_Y, LOGIN_WIDTH, LOGIN_HEIGHT, new Color(22, 22, 22, 140));
        titleFont.drawCenteredString("Login", PADDING + LOGIN_WIDTH / 2f, LOGIN_Y + PADDING, new Color(255,255,255).getRGB());

        username.drawTextBox();
        password.drawTextBox();

        drawActionButtons(mouseX, mouseY);

        altFont.drawCenteredString(statusString, PADDING + LOGIN_WIDTH / 2, LOGIN_Y + LOGIN_HEIGHT - PADDING - altFont.getHeight(), new Color(255, 255, 255).getRGB());
    }

    private void drawInfoBoxes(int mouseX, int mouseY) {

        RenderUtils.drawRect(PADDING, PADDING, BOX_ACCENT_SIZE, INFO_HEIGHT / 3f, getHueColorAt(PADDING - BOX_ACCENT_SIZE, PADDING + (INFO_HEIGHT / 3)));
        RenderUtils.drawRect(PADDING, PADDING, LOGIN_WIDTH / 3f, BOX_ACCENT_SIZE, getHueColorAt(PADDING - BOX_ACCENT_SIZE, PADDING + (INFO_HEIGHT / 3)));
        RenderUtils.drawRect(PADDING + BOX_ACCENT_SIZE, PADDING + BOX_ACCENT_SIZE, LOGIN_WIDTH, INFO_HEIGHT, new Color(22, 22, 22, 140));

        titleFont.drawCenteredString("Keybinds", PADDING + LOGIN_WIDTH / 2f,  PADDING * 2, new Color(255,255,255).getRGB());
        altFont.drawString("CLICK - Login to alt", PADDING * 2, PADDING * 3 + titleFont.getHeight(), new Color(255,255,255).getRGB());
        altFont.drawString("ALT+CLICK - Select alt", PADDING * 2, PADDING * 4 + titleFont.getHeight() + altFont.getHeight(), new Color(255,255,255).getRGB());
        altFont.drawString("ALT+BACKSPACE - Delete selected alts", PADDING * 2, PADDING * 5 + titleFont.getHeight() + altFont.getHeight() * 2, new Color(255,255,255).getRGB());
        altFont.drawString("ALT+A - Select all alts", PADDING * 2, PADDING * 6 + titleFont.getHeight() + altFont.getHeight() * 3, new Color(255,255,255).getRGB());

        RenderUtils.drawRect(PADDING, height - PADDING - LOGIN_HEIGHT / 3f, BOX_ACCENT_SIZE, LOGIN_HEIGHT / 3f, getHueColorAt(PADDING, LOGIN_Y + (LOGIN_HEIGHT * 2) + PADDING - (LOGIN_HEIGHT / 3)));
        RenderUtils.drawRect(PADDING, height - PADDING, LOGIN_WIDTH / 3f, BOX_ACCENT_SIZE, getHueColorAt(PADDING, LOGIN_Y + (LOGIN_HEIGHT * 2) + PADDING - (LOGIN_HEIGHT / 3)));
        RenderUtils.drawRect(PADDING + BOX_ACCENT_SIZE, LOGIN_Y + LOGIN_HEIGHT + PADDING - BOX_ACCENT_SIZE, LOGIN_WIDTH, INFO_HEIGHT, new Color(22, 22, 22, 140));

        titleFont.drawCenteredString("Info", PADDING + LOGIN_WIDTH / 2f,  LOGIN_Y + LOGIN_HEIGHT + PADDING * 2, new Color(255,255,255).getRGB());
        altFont.drawString("user:pass currently broken use oauth.", PADDING * 2, LOGIN_Y + LOGIN_HEIGHT + titleFont.getHeight() + PADDING * 3, new Color(255,255,255).getRGB());
        altFont.drawString("logging in with username only", PADDING * 2, LOGIN_Y + LOGIN_HEIGHT + titleFont.getHeight() + PADDING * 5, new Color(255,255,255).getRGB());
        altFont.drawString("creates cracked. use oauth for", PADDING * 2, LOGIN_Y + LOGIN_HEIGHT + titleFont.getHeight() + PADDING * 7, new Color(255,255,255).getRGB());
        altFont.drawString("token/cookie alts", PADDING * 2, LOGIN_Y + LOGIN_HEIGHT + titleFont.getHeight() + PADDING * 9, new Color(255,255,255).getRGB());
    }

    private void drawAltSwitcher(int mouseX, int mouseY) {
        int listX = BOX_X + 5;
        int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
        int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;
        int listHeight = this.height - listY - 5;

        final int COLUMNS = 3;

        int cellWidth = listWidth / COLUMNS;
        int cellHeight = ENTRY_HEIGHT;
        int rowStride = cellHeight + ENTRY_PADDING;

        int visibleRows = listHeight / rowStride;
        int startIndex = scrollOffset * COLUMNS;

        enableScissor(listX, listY, listWidth, listHeight);

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {

                int altIndex = startIndex + row * COLUMNS + col;
                if (altIndex >= alts.size()) break;

                int x = listX + col * cellWidth;
                int y = listY + row * rowStride;

                String[] parts = alts.get(altIndex).split("\\|", 2);
                String altType = parts[0];
                String altName = parts[1];

                String uuid = (altType.equals("microsoftOAuth") || altType.equals("microsoft"))
                        ? altName
                        : "";

                drawCustomCell(x, y, cellWidth - 4, cellHeight, altName, uuid, altIndex, mouseX, mouseY);
            }
        }

        disableScissor();

        int scrollbarX = BOX_X + BOX_WIDTH - SCROLLBAR_WIDTH - 2;
        int scrollbarY = listY;
        int scrollbarHeight = listHeight;

        RenderUtils.drawRoundedRect(
                scrollbarX,
                scrollbarY,
                SCROLLBAR_WIDTH,
                scrollbarHeight,
                2,
                true,
                new Color(50, 50, 50, 180)
        );

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        int thumbHeight = Math.max(scrollbarHeight * visibleRows / Math.max(1, totalRows), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);

        RenderUtils.drawRoundedRect(
                scrollbarX,
                thumbY,
                SCROLLBAR_WIDTH,
                thumbHeight,
                2,
                true,
                new Color(100, 100, 100, 220)
        );
    }



    private Color getHueColorAt(int x, int y) {
        long time = System.currentTimeMillis() - startTime;
        long weightX = 100L, weightY = 40L, speed = 3000L;
        float normal = 3000f;

        float hue = ((time + (x*weightX) + (y * weightY)) % speed) / normal;
        return Color.getHSBColor(hue, 0.5f, 0.95f);
    }

    private void drawActionButtons(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int startY = LOGIN_Y + LOGIN_HEIGHT - (buttonHeight * 2) - PADDING;

        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startX = PADDING * 2;

        drawCustomButton(startX, startY, buttonWidth, buttonHeight, "login", mouseX, mouseY);
        drawCustomButton(startX + (buttonWidth + buttonSpacing), startY, buttonWidth, buttonHeight, "oauth", mouseX, mouseY);
        drawCustomButton(startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight, "gen cracked", mouseX, mouseY);
    }

    private void drawCustomButton(int x, int y, int width, int height, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Color bgColor = hovered ? new Color(60, 60, 60) : new Color(40, 40, 40);

        RenderUtils.drawRect(x, y - 1, width, 1, getHueColorAt(x, y));
        RenderUtils.drawRect(x, y, width, height, bgColor);

        int textX = x + (width - buttonFont.getStringWidth(text)) / 2;
        int textY = y + (height - buttonFont.getHeight()) / 2;
        buttonFont.drawString(text, textX, textY, hovered ? ColorProcess.getColor().getRGB() : 0xFFFFFF);
    }


    private void drawCustomCell(int x, int y, int width, int height,
                                String text, String uuid, int index,
                                int mouseX, int mouseY) {

        boolean selected = selectedAlts.contains(index);

        Color bgColor = selected ? new Color(55, 55, 70) : new Color(22, 22, 22);

        RenderUtils.drawRect(x, y, width, height, bgColor);

        altFont.drawString(text, x + height, y + ENTRY_PADDING, 0xFFFFFF);

        loadHead(uuid);
        drawHead(x, y, uuid, height);
    }



    private final Map<String, ResourceLocation> headCache = new HashMap<>();
    private final Map<String, Boolean> headLoading = new HashMap<>();
    private final Map<String, Integer> headTries = new HashMap<>();

    private final ResourceLocation placeholderHead = new ResourceLocation("simp/images/Steve.png");

    public void loadHead(String uuid) {
        if (uuid == null || uuid.isEmpty()) return;
        if (headCache.containsKey(uuid)) return;
        if (headLoading.getOrDefault(uuid, false)) return;
        if (headTries.getOrDefault(uuid, 0) > 5) return;

        headLoading.put(uuid, true);
        headTries.put(uuid, headTries.getOrDefault(uuid, 0) + 1);

        headCache.put(uuid, placeholderHead);

        new Thread(() -> {
            try {

                URI uri;
                if (1 == 2) {
                    uri = URI.create("https://visage.surgeplay.com/bust/160/" + uuid + ".png");
                } else {
                    uri = URI.create("https://mc-heads.net/avatar/" + uuid);
                }

                URLConnection connection = uri.toURL().openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                connection.setRequestProperty("Accept", "image/png");

                BufferedImage image = ImageIO.read(connection.getInputStream());
                if (image == null) throw new IOException("Failed to read image");

                mc.addScheduledTask(() -> {
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation head = mc.getTextureManager()
                            .getDynamicTextureLocation("HEAD-" + uuid, texture);

                    headCache.put(uuid, head);
                    headLoading.put(uuid, false);
                });

            } catch (IOException e) {
                e.printStackTrace();
                headLoading.put(uuid, false);
            }
        }).start();
    }

    public void drawHead(int x, int y, String uuid, int cellHeight) {
        ResourceLocation head = uuid == null || uuid.isEmpty() ? placeholderHead : headCache.getOrDefault(uuid, placeholderHead);

        int size = cellHeight - (ENTRY_PADDING * 2);

        GlUtils.setup2DRendering();
        GlUtils.startBlend();
        GlStateManager.disableAlpha();

        mc.getTextureManager().bindTexture(head);
        Gui.drawModalRectWithCustomSizedTexture(x + ENTRY_PADDING, y + ENTRY_PADDING, 0, 0, size, size, size, size);

        GlUtils.endBlend();
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
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Action buttons
        int centerX = this.width / 2;
        int totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int startY = LOGIN_Y + LOGIN_HEIGHT - (buttonHeight * 2) - PADDING;
        int startX = PADDING * 2;

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, startX, startY, buttonWidth, buttonHeight)) {
            if (password.getText().isEmpty()) {
                handleCrackedLogin(username.getText());
            } else {
                //TODO: add microsoft user:pass login support(currently broken)
            }
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, startX + (buttonWidth + buttonSpacing), startY, buttonWidth, buttonHeight)) {
            handleOAuthLogin();
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, startX + (buttonWidth + buttonSpacing) * 2, startY, buttonWidth, buttonHeight)) {
            String username = generateRandomString();
            handleCrackedLogin(username);
            return;
        }

        if (GuiScreen.isAltKeyDown()) {
            int listX = BOX_X + 5;
            int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
            int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;

            final int COLUMNS = 3;
            int cellWidth = listWidth / COLUMNS;
            int rowStride = ENTRY_HEIGHT + ENTRY_PADDING;

            int col = (mouseX - listX) / cellWidth;
            int row = (mouseY - listY) / rowStride;

            if (col >= 0 && col < COLUMNS && row >= 0) {
                int index = (scrollOffset + row) * COLUMNS + col;
                if (index >= 0 && index < alts.size()) {
                    if (selectedAlts.contains(index)) {
                        selectedAlts.remove((Integer) index);
                    } else {
                        selectedAlts.add(index);
                    }
                    return;
                }
            }
        }

        if (!GuiScreen.isAltKeyDown()) {
            int listX = BOX_X + 5;
            int listY = BOX_Y + TOP_BOX_HEIGHT + 5;
            int listWidth = BOX_WIDTH - SCROLLBAR_WIDTH - 10;

            final int COLUMNS = 3;
            int cellWidth = listWidth / COLUMNS;
            int rowStride = ENTRY_HEIGHT + ENTRY_PADDING;

            int col = (mouseX - listX) / cellWidth;
            int row = (mouseY - listY) / rowStride;

            if (col >= 0 && col < COLUMNS && row >= 0) {
                int index = (scrollOffset + row) * COLUMNS + col;
                if (index >= 0 && index < alts.size()) {
                    loginWithAlt(alts.get(index));
                    return;
                }
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


    private void saveAlts() {
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
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
        File altsFile = new File(dir, "alts.txt");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(altsFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoftOAuth|" + username);
            alts.add("microsoftOAuth|" + username);
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

    private String loadRefreshToken(String username) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
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

    private void handleCrackedLogin(String username) {
        if (isLoggingIn) {
            return;
        }
        if (username.isEmpty()) {
            return;
        }

        isLoggingIn = true;

        mc.setSession(new Session(
                username, username, "0", "legacy"
        ));
        saveCrackedToFile(username);

        statusString = "Logged in with " + username;
        clearTextBoxes();
        isLoggingIn = false;
    }

    private void handleOAuthLogin() {
        if (isLoggingIn) {
            return;
        }

        isLoggingIn = true;
        statusString = "Awaiting response for Microsoft login...";

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            try {
                if (refreshToken != null) {
                    MicrosoftOAuthTranslation.LoginData login =
                            MicrosoftOAuthTranslation.login(refreshToken);

                    if (login.isGood()) {
                        mc.setSession(new Session(
                                login.username,
                                login.uuid,
                                login.mcToken,
                                "microsoft"
                        ));
                        saveOAuthAltToFile(login.username, login.newRefreshToken);
                        statusString = "Logged in with " + login.username;
                    } else {
                        statusString = "Failed to login with Microsoft OAuth";
                    }
                } else {
                    statusString = "Failed to get refresh token";
                }
            } finally {
                isLoggingIn = false;
            }
        });
    }

    private void saveCrackedToFile(String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Simp");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("cracked|" + sessionUsername);
            alts.add("cracked|" + sessionUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

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

    private void clearTextBoxes() {
        username.setText("");
        password.setText("");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        username.keyTyped(typedChar, keyCode);
        password.keyTyped(typedChar, keyCode);

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_A) {
            selectedAlts.clear();
            for (int i = 0; i < alts.size(); i++) {
                selectedAlts.add(i);
            }
            return;
        }


        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_BACK) {
            if (!selectedAlts.isEmpty()) {

                selectedAlts.sort((a, b) -> b - a);

                for (int index : selectedAlts) {
                    if (index >= 0 && index < alts.size()) {
                        alts.remove(index);

                    }
                }

                selectedAlts.clear();
                saveAltsToFile();
            }
            return;
        }



        super.keyTyped(typedChar, keyCode);
    }
}
