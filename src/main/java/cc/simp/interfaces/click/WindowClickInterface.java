package cc.simp.interfaces.click;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.impl.client.ClientSettingsModule;
import cc.simp.utils.client.Logger;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.FontUtils;
import cc.simp.utils.misc.GitHubConfigFetcher;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowClickInterface extends GuiScreen {

    private final CustomFontRenderer font = FontUtils.getFont("bold");
    private final CustomFontRenderer titleFont = FontUtils.getFont("big");

    // Window properties
    private int windowX = 100;
    private int windowY = 100;
    private final int windowWidth = 480;
    private final int windowHeight = 300;
    private boolean dragging = false;
    private int dragX, dragY;

    // Layout
    private static final int SIDEBAR_WIDTH = 130;
    private static final int HEADER_HEIGHT = 35;
    private static final int CONTENT_PADDING = 12;
    private static final int MODULE_ITEM_HEIGHT = 32;
    private static final int CONFIG_ITEM_HEIGHT = 28;

    // Colors
    private static final Color WINDOW_BG = new Color(17, 17, 17, 250);
    private static final Color HEADER_BG = new Color(20, 20, 20, 255);
    private static final Color SIDEBAR_BG = new Color(19, 19, 19, 245);
    private static final Color CONTENT_BG = new Color(24, 24, 24, 240);
    private static Color ACCENT_COLOR = new Color(120, 145, 255);
    private static final Color TEXT_COLOR = new Color(210, 210, 210);
    private static final Color SECONDARY_TEXT = new Color(140, 140, 140);
    private static final Color SEPARATOR_COLOR = new Color(35, 35, 35);
    private static final Color HOVER_COLOR = new Color(32, 32, 32);

    // State
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;
    private Module listeningModule = null;
    private SettingComponent draggingSlider = null;
    private SettingComponent editingString = null;
    private String editingBuffer = "";

    // Config state
    private volatile List<ConfigItem> configList = new ArrayList<>();
    private volatile boolean configsLoading = true;
    private boolean configLoadStarted = false;

    // Scrolling
    private float moduleScrollOffset = 0f;
    private float moduleTargetScroll = 0f;
    private float settingScrollOffset = 0f;
    private float settingTargetScroll = 0f;
    private float configScrollOffset = 0f;
    private float configTargetScroll = 0f;

    // Component cache
    private final Map<Property<?>, SettingComponent> componentCache = new HashMap<>();

    @Override
    public void initGui() {
        if (selectedModule == null) {
            List<Module> modules = Simp.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
            if (!modules.isEmpty()) {
                selectedModule = modules.getFirst();
            }
        }

        // Load configs asynchronously once.
        if (!configLoadStarted) {
            configLoadStarted = true;
            configsLoading = true;
            new Thread(() -> {
                try {
                    List<String> configs = GitHubConfigFetcher.fetchConfigList();
                    List<ConfigItem> loadedConfigs = new ArrayList<>(configs.size());
                    for (String configName : configs) {
                        loadedConfigs.add(new ConfigItem(configName));
                    }
                    configList = loadedConfigs;
                    configsLoading = false;
                } catch (Exception e) {
                    Logger.chatError("Failed to load configs: " + e.getMessage());
                    configsLoading = false;
                }
            }, "Simp-Config-Loader").start();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ACCENT_COLOR = ColorProcess.getColor();

        ClientSettingsModule.renderAnimeImage(width, height);

        // Update dragging
        if (dragging) {
            windowX = clamp(mouseX - dragX, 0, Math.max(0, width - windowWidth));
            windowY = clamp(mouseY - dragY, 0, Math.max(0, height - windowHeight));
        }

        // Update slider drag
        if (draggingSlider != null) {
            draggingSlider.updateDrag(mouseX);
        }

        // Smooth scrolling
        moduleScrollOffset = RenderUtils.lerp(moduleScrollOffset, moduleTargetScroll, 0.18f);
        settingScrollOffset = RenderUtils.lerp(settingScrollOffset, settingTargetScroll, 0.18f);
        configScrollOffset = RenderUtils.lerp(configScrollOffset, configTargetScroll, 0.18f);

        drawWindow(mouseX, mouseY);
    }

    private void drawWindow(int mouseX, int mouseY) {
        // Window shadow
        RenderUtils.drawRoundedRect(windowX - 3, windowY - 3, windowWidth + 6, windowHeight + 6, 6, new Color(0, 0, 0, 80));

        // Main window background
        RenderUtils.drawRoundedRect(windowX, windowY, windowWidth, windowHeight, 5, WINDOW_BG);

        // Header
        RenderUtils.drawRoundedRect(windowX, windowY, windowWidth, HEADER_HEIGHT, 5, HEADER_BG);
        drawRect(windowX, windowY + HEADER_HEIGHT - 5, windowX + windowWidth, windowY + HEADER_HEIGHT, HEADER_BG.getRGB());

        titleFont.drawString("Simp Client", windowX + 12, windowY + 11, ACCENT_COLOR.getRGB());

        // Close button
        int closeX = windowX + windowWidth - 25;
        int closeY = windowY + 8;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 18 && mouseY >= closeY && mouseY <= closeY + 18;
        RenderUtils.drawRoundedRect(closeX, closeY, 18, 18, 3, closeHovered ? new Color(220, 60, 60) : new Color(60, 60, 60));
        font.drawString("×", closeX + 5, closeY + 3, Color.WHITE.getRGB());

        // Separator line
        drawRect(windowX, windowY + HEADER_HEIGHT, windowX + windowWidth, windowY + HEADER_HEIGHT + 1, SEPARATOR_COLOR.getRGB());

        // Draw sidebar
        drawSidebar(mouseX, mouseY);

        // Sidebar separator
        int sidebarX = windowX + SIDEBAR_WIDTH;
        drawRect(sidebarX, windowY + HEADER_HEIGHT, sidebarX + 1, windowY + windowHeight, SEPARATOR_COLOR.getRGB());

        // Content area
        int contentX = sidebarX + 1;
        int contentY = windowY + HEADER_HEIGHT + 1;
        int contentWidth = windowWidth - SIDEBAR_WIDTH - 1;
        int contentHeight = windowHeight - HEADER_HEIGHT - 1;

        if (selectedCategory == ModuleCategory.CONFIGS) {
            drawConfigList(contentX, contentY, contentWidth, contentHeight, mouseX, mouseY);
        } else {
            int moduleListWidth = (int) (contentWidth * 0.35);
            drawModuleList(contentX, contentY, moduleListWidth, contentHeight, mouseX, mouseY);

            int settingsX = contentX + moduleListWidth + 1;
            drawRect(settingsX - 1, contentY, settingsX, contentY + contentHeight, SEPARATOR_COLOR.getRGB());
            drawSettings(settingsX, contentY, contentWidth - moduleListWidth - 1, contentHeight, mouseX, mouseY);
        }
    }

    private void drawSidebar(int mouseX, int mouseY) {
        int sidebarX = windowX;
        int sidebarY = windowY + HEADER_HEIGHT + 1;
        int sidebarHeight = windowHeight - HEADER_HEIGHT - 1;

        RenderUtils.drawRoundedRect(sidebarX, sidebarY, SIDEBAR_WIDTH, sidebarHeight, 0, SIDEBAR_BG);

        int categoryY = sidebarY + CONTENT_PADDING;

        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = selectedCategory == category;
            boolean hovered = mouseX >= sidebarX && mouseX <= sidebarX + SIDEBAR_WIDTH &&
                    mouseY >= categoryY && mouseY <= categoryY + 28;

            Color bgColor = selected ? new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), 40) :
                    hovered ? HOVER_COLOR : new Color(0, 0, 0, 0);

            if (selected || hovered) {
                RenderUtils.drawRoundedRect(sidebarX + 6, categoryY, SIDEBAR_WIDTH - 12, 28, 3, bgColor);
            }

            if (selected) {
                drawRect(sidebarX + 4, categoryY + 8, sidebarX + 6, categoryY + 20, ACCENT_COLOR.getRGB());
            }

            Color textColor = selected ? ACCENT_COLOR : hovered ? TEXT_COLOR : SECONDARY_TEXT;
            font.drawString(category.name(), sidebarX + 14, categoryY + 9, textColor.getRGB());

            categoryY += 32;
        }
    }

    private void drawModuleList(int x, int y, int width, int height, int mouseX, int mouseY) {
        drawRect(x, y, x + width, y + height, CONTENT_BG.getRGB());

        List<Module> modules = Simp.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
        int totalHeight = modules.size() * MODULE_ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - (height - CONTENT_PADDING * 2));
        moduleTargetScroll = clamp(moduleTargetScroll, 0f, maxScroll);

        RenderUtils.startScissor(x, y + CONTENT_PADDING, width, height - CONTENT_PADDING * 2);

        int moduleY = y + CONTENT_PADDING - (int) moduleScrollOffset;

        for (Module module : modules) {
            boolean selected = module == selectedModule;
            boolean hovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= moduleY && mouseY <= moduleY + MODULE_ITEM_HEIGHT;

            Color bgColor = selected ? new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), 30) :
                    hovered ? HOVER_COLOR : new Color(0, 0, 0, 0);

            if (selected || hovered) {
                RenderUtils.drawRoundedRect(x + 8, moduleY, width - 16, MODULE_ITEM_HEIGHT, 4, bgColor);
            }

            // Module status indicator
            int indicatorSize = 8;
            int indicatorX = x + 16;
            int indicatorY = moduleY + (MODULE_ITEM_HEIGHT - indicatorSize) / 2;
            Color indicatorColor = module.isEnabled() ? ACCENT_COLOR : new Color(60, 60, 60);
            RenderUtils.drawCircle(indicatorX + indicatorSize / 2.0, indicatorY + indicatorSize / 2.0, indicatorSize / 2.0, indicatorColor.getRGB());

            // Module name
            String name = module == listeningModule ? "Press a key..." : module.getLabel();
            Color textColor = module.isEnabled() ? TEXT_COLOR.brighter() : SECONDARY_TEXT;
            font.drawString(name, x + 30, moduleY + 11, textColor.getRGB());

            // Keybind
            if (module.getKey() != 0 && module != listeningModule) {
                String keyName = Keyboard.getKeyName(module.getKey());
                int keyWidth = font.getStringWidth(keyName);
                RenderUtils.drawRoundedRect(x + width - keyWidth - 20, moduleY + 8, keyWidth + 10, 16, 3, new Color(40, 40, 40));
                font.drawString(keyName, x + width - keyWidth - 15, moduleY + 10, SECONDARY_TEXT.getRGB());
            }

            moduleY += MODULE_ITEM_HEIGHT;
        }

        RenderUtils.endScissor();

        // Scrollbar
        if (totalHeight > height - CONTENT_PADDING * 2) {
            drawScrollbar(x + width - 8, y + CONTENT_PADDING, height - CONTENT_PADDING * 2, totalHeight, moduleScrollOffset);
        }
    }

    private void drawConfigList(int x, int y, int width, int height, int mouseX, int mouseY) {
        drawRect(x, y, x + width, y + height, CONTENT_BG.getRGB());

        if (configsLoading) {
            String loadingText = "Loading configs...";
            int textWidth = font.getStringWidth(loadingText);
            font.drawString(loadingText, x + (width - textWidth) / 2.0f, y + height / 2.0f - 5, SECONDARY_TEXT.getRGB());
            return;
        }

        if (configList.isEmpty()) {
            String emptyText = "No configs available";
            int textWidth = font.getStringWidth(emptyText);
            font.drawString(emptyText, x + (width - textWidth) / 2.0f, y + height / 2.0f - 5, SECONDARY_TEXT.getRGB());
            return;
        }

        int totalHeight = configList.size() * CONFIG_ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - (height - CONTENT_PADDING * 2));
        configTargetScroll = clamp(configTargetScroll, 0f, maxScroll);

        RenderUtils.startScissor(x, y + CONTENT_PADDING, width, height - CONTENT_PADDING * 2);

        int configY = y + CONTENT_PADDING - (int) configScrollOffset;

        for (ConfigItem config : configList) {
            boolean hovered = mouseX >= x + CONTENT_PADDING && mouseX <= x + width - CONTENT_PADDING &&
                    mouseY >= configY && mouseY <= configY + CONFIG_ITEM_HEIGHT;

            if (hovered) {
                RenderUtils.drawRoundedRect(x + CONTENT_PADDING, configY, width - CONTENT_PADDING * 2, CONFIG_ITEM_HEIGHT, 4, HOVER_COLOR);
            }

            // Config icon
            RenderUtils.drawRoundedRect(x + CONTENT_PADDING + 8, configY + 6, 16, 16, 3, new Color(40, 40, 40));
            font.drawString("C", x + CONTENT_PADDING + 13, configY + 9, ACCENT_COLOR.getRGB());

            // Config name
            font.drawString(config.name, x + CONTENT_PADDING + 32, configY + 9, TEXT_COLOR.getRGB());

            // Download button
            int buttonX = x + width - CONTENT_PADDING - 80;
            int buttonY = configY + 6;
            boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + 75 &&
                    mouseY >= buttonY && mouseY <= buttonY + 16;

            Color buttonColor = config.downloading ? new Color(80, 80, 80) :
                    buttonHovered ? ACCENT_COLOR.brighter() : ACCENT_COLOR;
            RenderUtils.drawRoundedRect(buttonX, buttonY, 75, 16, 3, buttonColor);

            String buttonText = config.downloading ? "Loading..." : "Download";
            int textWidth = font.getStringWidth(buttonText);
            font.drawString(buttonText, buttonX + (75 - textWidth) / 2.0f, buttonY + 4, Color.WHITE.getRGB());

            configY += CONFIG_ITEM_HEIGHT;
        }

        RenderUtils.endScissor();

        // Scrollbar
        if (totalHeight > height - CONTENT_PADDING * 2) {
            drawScrollbar(x + width - 8, y + CONTENT_PADDING, height - CONTENT_PADDING * 2, totalHeight, configScrollOffset);
        }
    }

    private void drawSettings(int x, int y, int width, int height, int mouseX, int mouseY) {
        drawRect(x, y, x + width, y + height, CONTENT_BG.getRGB());

        if (selectedModule == null) {
            String text = "Select a module";
            int textWidth = font.getStringWidth(text);
            font.drawString(text, x + (width - textWidth) / 2.0f, y + height / 2.0f - 5, SECONDARY_TEXT.getRGB());
            return;
        }

        // Module header
        int headerY = y + CONTENT_PADDING;
        titleFont.drawString(selectedModule.getLabel(), x + CONTENT_PADDING, headerY, TEXT_COLOR.getRGB());

        if (selectedModule.getDescription() != null && !selectedModule.getDescription().isEmpty()) {
            font.drawString(selectedModule.getDescription(), x + CONTENT_PADDING, headerY + 16, SECONDARY_TEXT.getRGB());
        }

        drawRect(x + CONTENT_PADDING, headerY + 32, x + width - CONTENT_PADDING, headerY + 33, SEPARATOR_COLOR.getRGB());

        int settingsY = headerY + 40;
        int availableHeight = height - settingsY + y - CONTENT_PADDING;

        List<Property<?>> properties = selectedModule.getElements();
        int totalHeight = 0;
        for (Property<?> property : properties) {
            if (property.isAvailable()) {
                totalHeight += getPropertyHeight(property) + 8;
            }
        }

        int maxScroll = Math.max(0, totalHeight - availableHeight);
        settingTargetScroll = clamp(settingTargetScroll, 0f, maxScroll);

        RenderUtils.startScissor(x, settingsY, width, availableHeight);

        int currentY = settingsY - (int) settingScrollOffset;

        for (Property<?> property : properties) {
            if (property.isAvailable()) {
                int propHeight = getPropertyHeight(property);
                drawProperty(property, x + CONTENT_PADDING, currentY, width - CONTENT_PADDING * 2, mouseX, mouseY);
                currentY += propHeight + 8;
            }
        }

        RenderUtils.endScissor();

        // Scrollbar
        if (totalHeight > availableHeight) {
            drawScrollbar(x + width - 8, settingsY, availableHeight, totalHeight, settingScrollOffset);
        }
    }

    private void drawScrollbar(int x, int y, int height, int totalHeight, float scrollOffset) {
        int scrollbarWidth = 6;
        RenderUtils.drawRoundedRect(x, y, scrollbarWidth, height, 3, new Color(30, 30, 30, 100));

        float trackHeight = Math.max(1, height);
        float thumbSize = Math.max(20f, trackHeight / totalHeight * trackHeight);
        float maxScroll = Math.max(0, totalHeight - height);
        float thumbPos = maxScroll > 0 ? (scrollOffset / maxScroll) * (height - thumbSize) : 0f;

        RenderUtils.drawRoundedRect(x + 1, y + thumbPos, scrollbarWidth - 2, thumbSize, 2, ACCENT_COLOR);
    }

    private int getPropertyHeight(Property<?> property) {
        SettingComponent component = findOrCreateComponent(property);
        if (property instanceof ModeProperty && component.dropdownOpen) {
            return 30 + ((ModeProperty<?>) property).getValues().length * 20;
        }
        if (property instanceof NumberProperty) {
            return 36;
        }
        return 26;
    }

    private void drawProperty(Property<?> property, int x, int y, int width, int mouseX, int mouseY) {
        SettingComponent component = findOrCreateComponent(property);

        font.drawString(property.getLabel(), x, y + 2, TEXT_COLOR.getRGB());

        if (property.getType() == Boolean.class) {
            drawBooleanProperty(property, x, y, width);
        } else if (property instanceof NumberProperty) {
            drawNumberProperty((NumberProperty) property, component, x, y, width, mouseX, mouseY);
        } else if (property instanceof ModeProperty) {
            drawModeProperty((ModeProperty<?>) property, component, x, y, width, mouseX, mouseY);
        } else if (property.getType() == String.class) {
            drawStringProperty(property, component, x, y, width);
        }
    }

    private void drawBooleanProperty(Property<?> property, int x, int y, int width) {
        boolean value = (Boolean) property.getValue();
        int switchWidth = 32;
        int switchHeight = 16;
        int switchX = x + width - switchWidth;
        int switchY = y + 3;

        Color bgColor = value ? ACCENT_COLOR.darker() : new Color(50, 50, 50);
        RenderUtils.drawRoundedRect(switchX, switchY, switchWidth, switchHeight, 8, bgColor);

        int knobSize = 12;
        int knobX = value ? switchX + switchWidth - knobSize - 2 : switchX + 2;
        Color knobColor = value ? Color.WHITE : new Color(100, 100, 100);
        RenderUtils.drawCircle(knobX + knobSize / 2.0, switchY + switchHeight / 2.0, knobSize / 2.0, knobColor.getRGB());
    }

    private void drawNumberProperty(NumberProperty property, SettingComponent component, int x, int y, int width, int mouseX, int mouseY) {
        String valueStr = formatNumber(property.getValue());
        int valueWidth = font.getStringWidth(valueStr);
        font.drawString(valueStr, x + width - valueWidth, y + 2, ACCENT_COLOR.getRGB());

        int sliderY = y + 20;
        int sliderHeight = 6;
        double percent = (property.getValue() - property.getMin()) / (property.getMax() - property.getMin());

        RenderUtils.drawRoundedRect(x, sliderY, width, sliderHeight, 3, new Color(40, 40, 40));
        RenderUtils.drawRoundedRect(x, sliderY, (float) (width * percent), sliderHeight, 3, ACCENT_COLOR);

        // Thumb
        int thumbSize = 12;
        int thumbX = (int) (x + width * percent - thumbSize / 2.0);
        boolean thumbHovered = mouseX >= thumbX && mouseX <= thumbX + thumbSize &&
                mouseY >= sliderY - 3 && mouseY <= sliderY + sliderHeight + 3;
        Color thumbColor = draggingSlider == component || thumbHovered ? ACCENT_COLOR.brighter() : ACCENT_COLOR;
        RenderUtils.drawCircle(thumbX + thumbSize / 2.0, sliderY + sliderHeight / 2.0, thumbSize / 2.0, thumbColor.getRGB());

        component.componentX = x;
        component.componentWidth = width;
    }

    private void drawModeProperty(ModeProperty<?> property, SettingComponent component, int x, int y, int width, int mouseX, int mouseY) {
        String value = property.getValue().toString();
        int valueWidth = font.getStringWidth(value);

        String arrow = component.dropdownOpen ? "▲" : "▼";
        int arrowWidth = font.getStringWidth(arrow);
        font.drawString(arrow, x + width - arrowWidth, y + 2, SECONDARY_TEXT.getRGB());
        font.drawString(value, x + width - valueWidth - arrowWidth - 8, y + 2, ACCENT_COLOR.getRGB());

        if (component.dropdownOpen) {
            int dropdownY = y + 22;
            Object[] values = property.getValues();

            for (Object val : values) {
                boolean selected = val.equals(property.getValue());
                boolean hovered = mouseX >= x && mouseX <= x + width &&
                        mouseY >= dropdownY && mouseY <= dropdownY + 20;

                Color bgColor = selected ? new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), 50) :
                        hovered ? new Color(40, 40, 40) : new Color(30, 30, 30);

                RenderUtils.drawRoundedRect(x, dropdownY, width, 20, 3, bgColor);
                font.drawString(val.toString(), x + 8, dropdownY + 6, selected ? ACCENT_COLOR.getRGB() : TEXT_COLOR.getRGB());

                dropdownY += 20;
            }
        }
    }

    private void drawStringProperty(Property<?> property, SettingComponent component, int x, int y, int width) {
        String displayValue;
        Color valueColor;

        if (editingString == component) {
            displayValue = editingBuffer + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
            valueColor = ACCENT_COLOR.brighter();
        } else {
            String value = (String) property.getValue();
            displayValue = value.isEmpty() ? "Click to edit" : value;
            valueColor = value.isEmpty() ? SECONDARY_TEXT : ACCENT_COLOR;
        }

        int boxWidth = width - font.getStringWidth(property.getLabel()) - 8;
        int boxX = x + width - boxWidth;

        Color boxColor = editingString == component ? new Color(40, 40, 40) : new Color(35, 35, 35);
        RenderUtils.drawRoundedRect(boxX, y, boxWidth, 18, 3, boxColor);

        int textWidth = font.getStringWidth(displayValue);
        font.drawString(displayValue, boxX + (boxWidth - textWidth) / 2.0f, y + 5, valueColor.getRGB());
    }

    private String formatNumber(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(value % 1 == 0 ? 0 : 2, RoundingMode.HALF_UP);
        return bd.stripTrailingZeros().toPlainString();
    }

    private SettingComponent findOrCreateComponent(Property<?> property) {
        return componentCache.computeIfAbsent(property, SettingComponent::new);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        clearTransientState();

        // Close button
        int closeX = windowX + windowWidth - 25;
        int closeY = windowY + 8;
        if (mouseX >= closeX && mouseX <= closeX + 18 && mouseY >= closeY && mouseY <= closeY + 18) {
            mc.displayGuiScreen(null);
            return;
        }

        // Window dragging
        if (mouseX >= windowX && mouseX <= windowX + windowWidth &&
                mouseY >= windowY && mouseY <= windowY + HEADER_HEIGHT) {
            dragging = true;
            dragX = mouseX - windowX;
            dragY = mouseY - windowY;
            return;
        }

        // Sidebar category selection
        int sidebarY = windowY + HEADER_HEIGHT + 1 + CONTENT_PADDING;
        for (ModuleCategory category : ModuleCategory.values()) {
            if (mouseX >= windowX && mouseX <= windowX + SIDEBAR_WIDTH &&
                    mouseY >= sidebarY && mouseY <= sidebarY + 28) {
                selectedCategory = category;
                selectedModule = null;
                listeningModule = null;
                moduleTargetScroll = 0;
                settingTargetScroll = 0;
                configTargetScroll = 0;
                if (selectedCategory != ModuleCategory.CONFIGS) {
                    List<Module> modules = Simp.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
                    if (!modules.isEmpty()) {
                        selectedModule = modules.getFirst();
                    }
                }
                return;
            }
            sidebarY += 32;
        }

        int contentX = windowX + SIDEBAR_WIDTH + 1;
        int contentY = windowY + HEADER_HEIGHT + 1;

        if (selectedCategory == ModuleCategory.CONFIGS) {
            handleConfigClick(contentX, contentY, mouseX, mouseY, mouseButton);
        } else {
            handleModuleClick(contentX, contentY, mouseX, mouseY, mouseButton);
            handleSettingsClick(mouseX, mouseY);
        }
    }

    private void handleConfigClick(int contentX, int contentY, int mouseX, int mouseY, int mouseButton) {
        if (configsLoading || configList.isEmpty()) return;

        int configY = contentY + CONTENT_PADDING - (int) configScrollOffset;

        for (ConfigItem config : configList) {
            int buttonX = contentX + windowWidth - SIDEBAR_WIDTH - 1 - CONTENT_PADDING - 80;
            int buttonY = configY + 6;

            if (mouseX >= buttonX && mouseX <= buttonX + 75 &&
                    mouseY >= buttonY && mouseY <= buttonY + 16 &&
                    mouseButton == 0 && !config.downloading) {
                config.downloading = true;
                new Thread(() -> {
                    boolean success = GitHubConfigFetcher.downloadAndLoadConfig(config.name);
                    config.downloading = false;
                    if (success) {
                        cc.simp.utils.client.Logger.chatPrint("§aConfig '" + config.name + "' loaded successfully!");
                    } else {
                        cc.simp.utils.client.Logger.chatPrint("§cFailed to load config '" + config.name + "'.");
                    }
                }).start();
                return;
            }
            configY += CONFIG_ITEM_HEIGHT;
        }
    }

    private void handleModuleClick(int contentX, int contentY, int mouseX, int mouseY, int mouseButton) {
        int moduleListWidth = (int) ((windowWidth - SIDEBAR_WIDTH - 1) * 0.35);

        if (mouseX < contentX || mouseX > contentX + moduleListWidth) return;

        List<Module> modules = Simp.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
        int moduleY = contentY + CONTENT_PADDING - (int) moduleScrollOffset;

        for (Module module : modules) {
            if (mouseY >= moduleY && mouseY <= moduleY + MODULE_ITEM_HEIGHT) {
                if (mouseButton == 1) {
                    selectedModule = module;
                    settingTargetScroll = 0;
                } else if (mouseButton == 0) {
                    module.toggle();
                } else if (mouseButton == 2) {
                    listeningModule = module;
                }
                return;
            }
            moduleY += MODULE_ITEM_HEIGHT;
        }
    }

    private void handleSettingsClick(int mouseX, int mouseY) {
        if (selectedModule == null) return;

        int contentX = windowX + SIDEBAR_WIDTH + 1;
        int moduleListWidth = (int) ((windowWidth - SIDEBAR_WIDTH - 1) * 0.35);
        int settingsX = contentX + moduleListWidth + 1;
        int contentY = windowY + HEADER_HEIGHT + 1;
        int headerY = contentY + CONTENT_PADDING;
        int settingsY = headerY + 40;
        int settingsWidth = windowWidth - SIDEBAR_WIDTH - 1 - moduleListWidth - 1;

        int currentY = settingsY - (int) settingScrollOffset;

        for (Property<?> property : selectedModule.getElements()) {
            if (!property.isAvailable()) continue;

            SettingComponent component = findOrCreateComponent(property);
            int propHeight = getPropertyHeight(property);

            if (property.getType() == Boolean.class) {
                int switchX = settingsX + settingsWidth - CONTENT_PADDING - 32;
                int switchY = currentY + 3;
                if (mouseX >= switchX && mouseX <= switchX + 32 &&
                        mouseY >= switchY && mouseY <= switchY + 16) {
                    property.setValueObj(!(Boolean) property.getValue());
                    return;
                }
            } else if (property instanceof NumberProperty numProp) {
                int sliderY = currentY + 20;
                if (mouseX >= component.componentX && mouseX <= component.componentX + component.componentWidth &&
                        mouseY >= sliderY - 3 && mouseY <= sliderY + 9) {
                    draggingSlider = component;
                    updateSlider(numProp, component, mouseX);
                    return;
                }
            } else if (property instanceof ModeProperty<?> modeProp) {
                if (component.dropdownOpen) {
                    int dropdownY = currentY + 22;
                    for (Object value : modeProp.getValues()) {
                        if (mouseX >= settingsX + CONTENT_PADDING &&
                                mouseX <= settingsX + settingsWidth - CONTENT_PADDING &&
                                mouseY >= dropdownY && mouseY <= dropdownY + 20) {
                            modeProp.setValueObj(value);
                            component.dropdownOpen = false;
                            return;
                        }
                        dropdownY += 20;
                    }
                }
                if (mouseX >= settingsX + CONTENT_PADDING &&
                        mouseX <= settingsX + settingsWidth - CONTENT_PADDING &&
                        mouseY >= currentY && mouseY <= currentY + 18) {
                    component.dropdownOpen = !component.dropdownOpen;
                    return;
                }
            } else if (property.getType() == String.class) {
                int boxWidth = settingsWidth - CONTENT_PADDING * 2 - font.getStringWidth(property.getLabel()) - 8;
                int boxX = settingsX + settingsWidth - CONTENT_PADDING - boxWidth;
                if (mouseX >= boxX && mouseX <= boxX + boxWidth &&
                        mouseY >= currentY && mouseY <= currentY + 18) {
                    editingString = component;
                    editingBuffer = (String) property.getValue();
                    return;
                }
            }

            currentY += propHeight + 8;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        draggingSlider = null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * width / mc.displayWidth;

            int scrollAmount = wheel > 0 ? -20 : 20;

            int contentX = windowX + SIDEBAR_WIDTH + 1;

            if (selectedCategory == ModuleCategory.CONFIGS) {
                configTargetScroll += scrollAmount;
            } else {
                int moduleListWidth = (int) ((windowWidth - SIDEBAR_WIDTH - 1) * 0.35);
                if (mouseX >= contentX && mouseX <= contentX + moduleListWidth) {
                    moduleTargetScroll += scrollAmount;
                } else {
                    settingTargetScroll += scrollAmount;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (editingString != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                editingString = null;
                editingBuffer = "";
            } else if (keyCode == Keyboard.KEY_RETURN) {
                editingString.property.setValueObj(editingBuffer);
                editingString = null;
                editingBuffer = "";
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!editingBuffer.isEmpty()) {
                    editingBuffer = editingBuffer.substring(0, editingBuffer.length() - 1);
                }
            } else if (isCtrlKeyDown()) {
                if (keyCode == Keyboard.KEY_V) {
                    String clipboard = GuiScreen.getClipboardString();
                    if (!clipboard.isEmpty()) {
                        StringBuilder builder = new StringBuilder(editingBuffer.length() + clipboard.length());
                        builder.append(editingBuffer);
                        for (char c : clipboard.toCharArray()) {
                            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                                builder.append(c);
                            }
                        }
                        editingBuffer = builder.toString();
                    }
                } else if (keyCode == Keyboard.KEY_A) {
                    editingBuffer = "";
                } else if (keyCode == Keyboard.KEY_C) {
                    GuiScreen.setClipboardString(editingBuffer);
                }
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                editingBuffer += typedChar;
            }
            return;
        }

        if (listeningModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                listeningModule.setKey(Keyboard.KEY_NONE);
            } else {
                listeningModule.setKey(keyCode);
            }
            listeningModule = null;
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void updateSlider(NumberProperty property, SettingComponent component, int mouseX) {
        if (component.componentWidth <= 0) {
            return;
        }

        double percent = clamp((mouseX - component.componentX) / (double) component.componentWidth, 0.0, 1.0);
        double range = property.getMax() - property.getMin();
        double rawValue = range == 0 ? property.getMin() : property.getMin() + range * percent;
        double increment = property.getIncrement();
        if (increment > 0) rawValue = Math.round(rawValue / increment) * increment;
        double finalValue = clamp(rawValue, property.getMin(), property.getMax());
        property.setValue(finalValue);
    }

    private void clearTransientState() {
        dragging = false;
        draggingSlider = null;
        if (editingString != null) {
            editingString = null;
            editingBuffer = "";
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class SettingComponent {
        private final Property<?> property;
        private boolean dropdownOpen = false;
        private int componentX = 0;
        private int componentWidth = 0;

        public SettingComponent(Property<?> property) {
            this.property = property;
        }

        public void updateDrag(int mouseX) {
            if (property instanceof NumberProperty numProp) {
                if (componentWidth <= 0) {
                    return;
                }

                double percent = clamp((mouseX - componentX) / (double) componentWidth, 0.0, 1.0);
                double range = numProp.getMax() - numProp.getMin();
                double rawValue = range == 0 ? numProp.getMin() : numProp.getMin() + range * percent;
                double increment = numProp.getIncrement();
                if (increment > 0) rawValue = Math.round(rawValue / increment) * increment;
                double finalValue = clamp(rawValue, numProp.getMin(), numProp.getMax());
                numProp.setValue(finalValue);
            }
        }
    }

    private static class ConfigItem {
        private final String name;
        private boolean downloading = false;

        public ConfigItem(String name) {
            this.name = name;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}