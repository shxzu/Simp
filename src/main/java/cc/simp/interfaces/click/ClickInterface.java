package cc.simp.interfaces.click;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.processes.FontProcess;
import cc.simp.utils.client.Logger;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClickInterface extends GuiScreen {

    // Core components
    private final CustomFontRenderer fontRenderer = FontProcess.getFont("simp");
    private ModuleCategory currentCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;

    // UI state management
    private ModeProperty<?> openDropdown = null;
    private boolean draggingSlider = false;
    private Property<?> draggedProperty = null;
    private Module listeningModule = null;

    // Window positioning and dragging
    private static double windowX, windowY;
    private static boolean hasInitializedPosition = false;
    private double windowWidth, windowHeight;
    private boolean isDraggingWindow = false;
    private double dragStartX, dragStartY;

    // Scrolling system
    private double propertyScrollOffset = 0;
    private double targetPropertyScrollOffset = 0;
    private double maxPropertyScroll = 0;
    private boolean isScrolling = false;
    private double scrollStartY = 0;
    private double scrollStartOffset = 0;

    // Modern color scheme - Deep purple with gradients
    private final Color BACKGROUND_PRIMARY = new Color(18, 18, 24, 250);
    private final Color BACKGROUND_SECONDARY = new Color(24, 24, 32, 245);
    private final Color BACKGROUND_TERTIARY = new Color(28, 28, 38, 240);
    private final Color CARD_BACKGROUND = new Color(32, 32, 44, 240);
    private final Color ACCENT_PRIMARY = new Color(139, 92, 246);
    private final Color ACCENT_HOVER = new Color(167, 139, 250);
    private final Color ACCENT_ACTIVE = new Color(124, 58, 237);
    private final Color HOVER_OVERLAY = new Color(255, 255, 255, 12);
    private final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private final Color TEXT_MUTED = new Color(100, 116, 139);
    private final Color BORDER_COLOR = new Color(71, 85, 105, 40);
    private final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private final Color DIVIDER_COLOR = new Color(51, 65, 85, 80);

    // Bounds tracking
    private final Map<ModuleCategory, double[]> categoryBounds = new HashMap<>();
    private final Map<Module, double[]> moduleBounds = new HashMap<>();
    private final Map<Property<?>, double[]> propertyBounds = new HashMap<>();
    private final Map<Property<?>, double[]> sliderBounds = new HashMap<>();
    private final Map<ModeProperty<?>, Map<Object, double[]>> enumOptionBounds = new HashMap<>();
    private double[] scrollBarHitbox = null;

    // Layout constants - More spacious modern design
    private static final double PADDING = 12.0;
    private static final double BORDER_RADIUS = 10.0;
    private static final double CATEGORY_HEIGHT = 40.0;
    private static final double MODULE_HEIGHT = 36.0;
    private static final double PROPERTY_HEIGHT = 44.0;
    private static final double SLIDER_PROPERTY_HEIGHT = 56.0;
    private static final double HEADER_HEIGHT = 56.0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        initializeWindow();
        clearBounds();
        updateScrolling();

        drawBackground();
        drawMainWindow();
        drawContent(mouseX, mouseY);
    }

    private void initializeWindow() {
        this.windowWidth = this.width / 2.0;
        this.windowHeight = this.height / 1.4;

        if (!hasInitializedPosition) {
            windowX = (this.width - this.windowWidth) / 2;
            windowY = (this.height - this.windowHeight) / 2;
            hasInitializedPosition = true;
        }
    }

    private void updateScrolling() {
        propertyScrollOffset = RenderUtils.lerp((float) propertyScrollOffset, (float) targetPropertyScrollOffset, 0.2f);
    }

    private void drawBackground() {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 140).getRGB());
    }

    private void drawMainWindow() {
        // Main window with subtle shadow effect
        drawRoundedRect(windowX + 2, windowY + 2, windowX + windowWidth + 2, windowY + windowHeight + 2,
                BORDER_RADIUS, new Color(0, 0, 0, 60));
        drawRoundedRect(windowX, windowY, windowX + windowWidth, windowY + windowHeight,
                BORDER_RADIUS, BACKGROUND_PRIMARY);
    }

    private void drawContent(int mouseX, int mouseY) {
        drawHeader();

        double contentY = windowY + HEADER_HEIGHT;
        double contentHeight = windowHeight - HEADER_HEIGHT;

        drawCategoriesSection(mouseX, mouseY, windowX, contentY, windowWidth);

        double mainContentY = contentY + CATEGORY_HEIGHT + PADDING;
        double mainContentHeight = contentHeight - CATEGORY_HEIGHT - PADDING;
        double moduleListWidth = windowWidth * 0.35;

        drawModulesSection(mouseX, mouseY, windowX + PADDING, mainContentY, moduleListWidth, mainContentHeight);

        if (selectedModule != null) {
            drawSettingsSection(mouseX, mouseY, windowX + moduleListWidth + PADDING * 2, mainContentY,
                    windowWidth - moduleListWidth - PADDING * 3, mainContentHeight);
        }
    }

    private void drawHeader() {
        // Title
        String title = Simp.NAME;
        float titleScale = 1.5f;
        fontRenderer.drawStringWithShadow(title, (float) (windowX + PADDING * 1.5),
                (float) (windowY + (HEADER_HEIGHT - fontRenderer.FONT_HEIGHT * titleScale) / 2),
                TEXT_PRIMARY.getRGB());

        // Version badge
        String version = "v" + Simp.VERSION;
        double badgeWidth = fontRenderer.getStringWidth(version) + 12;
        double badgeX = windowX + PADDING + fontRenderer.getStringWidth(title) * titleScale - 3;
        double badgeY = windowY + (HEADER_HEIGHT - 25) / 2;

        drawRoundedRect(badgeX, badgeY, badgeX + badgeWidth, badgeY + 20, 10,
                new Color(139, 92, 246, 30));
        fontRenderer.drawStringWithShadow(version, (float) (badgeX + 6), (float) (badgeY + 6),
                ACCENT_PRIMARY.getRGB());
    }

    private void drawCategoriesSection(int mouseX, int mouseY, double x, double y, double width) {
        double categoryX = x + PADDING;
        double categoryY = y + PADDING;

        for (ModuleCategory category : ModuleCategory.values()) {
            drawCategoryTab(category, mouseX, mouseY, categoryX, categoryY);
            categoryX += getCategoryTabWidth(category) + PADDING / 2;
        }
    }

    private void drawCategoryTab(ModuleCategory category, int mouseX, int mouseY, double x, double y) {
        String name = category.name();
        double tabWidth = getCategoryTabWidth(category);
        double tabHeight = CATEGORY_HEIGHT - PADDING;

        categoryBounds.put(category, new double[]{x, y, x + tabWidth, y + tabHeight});

        boolean isSelected = currentCategory == category;
        boolean isHovered = inBounds(mouseX, mouseY, categoryBounds.get(category));

        // Background
        if (isSelected) {
            drawRoundedRect(x, y, x + tabWidth, y + tabHeight, BORDER_RADIUS * 0.7, ACCENT_PRIMARY);
        } else if (isHovered) {
            drawRoundedRect(x, y, x + tabWidth, y + tabHeight, BORDER_RADIUS * 0.7,
                    new Color(255, 255, 255, 15));
        }

        // Text
        Color textColor = isSelected ? TEXT_PRIMARY : TEXT_MUTED;
        fontRenderer.drawStringWithShadow(name, (float) (x + PADDING),
                (float) (y + (tabHeight - fontRenderer.FONT_HEIGHT) / 2),
                textColor.getRGB());
    }

    private double getCategoryTabWidth(ModuleCategory category) {
        return fontRenderer.getStringWidth(category.name()) + PADDING * 2;
    }

    private void drawModulesSection(int mouseX, int mouseY, double x, double y, double width, double height) {
        drawRoundedRect(x, y, x + width, y + height - 20, BORDER_RADIUS * 0.8, BACKGROUND_SECONDARY);

        double moduleY = y + PADDING;
        for (Module module : Simp.INSTANCE.getModuleManager().getModulesForCategory(currentCategory)) {
            if (moduleY + MODULE_HEIGHT > y + height) break;
            drawModuleCard(module, mouseX, mouseY, x, moduleY, width);
            moduleY += MODULE_HEIGHT + PADDING / 2;
        }
    }

    private void drawModuleCard(Module module, int mouseX, int mouseY, double x, double y, double width) {
        String displayName = (module == listeningModule) ? "Press any key..." : module.getLabel();

        moduleBounds.put(module, new double[]{x + PADDING / 2, y, x + width - PADDING / 2, y + MODULE_HEIGHT});

        boolean isSelected = selectedModule == module;
        boolean isEnabled = module.isEnabled();
        boolean isHovered = inBounds(mouseX, mouseY, moduleBounds.get(module));

        double cardX = x + PADDING / 2;
        double cardWidth = width - PADDING;

        // Card background with gradient effect
        Color bgColor;
        if (isEnabled) {
            bgColor = ACCENT_PRIMARY;
        } else if (isSelected) {
            bgColor = CARD_BACKGROUND;
        } else if (isHovered) {
            bgColor = new Color(CARD_BACKGROUND.getRed() + 8,
                    CARD_BACKGROUND.getGreen() + 8,
                    CARD_BACKGROUND.getBlue() + 8,
                    CARD_BACKGROUND.getAlpha());
        } else {
            bgColor = new Color(0, 0, 0, 0);
        }

        drawRoundedRect(cardX, y, cardX + cardWidth, y + MODULE_HEIGHT, BORDER_RADIUS * 0.6, bgColor);

        // Indicator bar for enabled modules
        if (isEnabled) {
            drawRoundedRect(cardX, y, cardX + 3, y + MODULE_HEIGHT, 1.5, SUCCESS_COLOR);
        }

        // Module name
        Color textColor = isEnabled ? TEXT_PRIMARY : (isSelected ? TEXT_PRIMARY : TEXT_SECONDARY);
        fontRenderer.drawStringWithShadow(displayName, (float) (cardX + (isEnabled ? 10 : 8)),
                (float) (y + (MODULE_HEIGHT - fontRenderer.FONT_HEIGHT) / 2),
                textColor.getRGB());

        // Keybind badge
        drawModuleKeybind(module, cardX, y, cardWidth);
    }

    private void drawModuleKeybind(Module module, double x, double y, double width) {
        if (module != listeningModule && module.getKey() != 0) {
            String keyName = Keyboard.getKeyName(module.getKey());
            double badgeWidth = fontRenderer.getStringWidth(keyName) + 8;
            double badgeX = x + width - badgeWidth - 4;
            double badgeY = y + (MODULE_HEIGHT - 16) / 2;

            drawRoundedRect(badgeX, badgeY, badgeX + badgeWidth, badgeY + 16, 8,
                    new Color(255, 255, 255, 10));
            fontRenderer.drawStringWithShadow(keyName, (float) (badgeX + 4),
                    (float) (badgeY + 4), TEXT_MUTED.getRGB());
        }
    }

    private void drawSettingsSection(int mouseX, int mouseY, double x, double y, double width, double height) {
        drawRoundedRect(x, y, x + width, y + height - 20, BORDER_RADIUS * 0.8, BACKGROUND_SECONDARY);

        // Settings header
        String title = selectedModule.getLabel();
        fontRenderer.drawStringWithShadow(title, (float) (x + PADDING),
                (float) (y + PADDING), TEXT_PRIMARY.getRGB());

        String description = selectedModule.getDescription();
        fontRenderer.drawStringWithShadow(description, (float) (x + PADDING),
                (float) (y + PADDING + fontRenderer.FONT_HEIGHT + 4), TEXT_MUTED.getRGB());

        // Divider
        double dividerY = y + PADDING + fontRenderer.FONT_HEIGHT + 10;
        drawRect((int) (x + PADDING), (int) dividerY, (int) (x + width - PADDING),
                (int) (dividerY + 1), DIVIDER_COLOR.getRGB());

        double propertiesY = dividerY + PADDING;
        double propertiesHeight = height - (propertiesY - y) - PADDING;

        drawPropertiesPanel(mouseX, mouseY, x, propertiesY, width, propertiesHeight);

        if (openDropdown != null) {
            drawEnumDropdown(mouseX, mouseY);
        }
    }

    private void drawPropertiesPanel(int mouseX, int mouseY, double x, double y, double width, double height) {
        RenderUtils.startScissor((float) x, (float) y, (float) width, (float) height);

        double currentY = y;
        double totalHeight = 0;

        for (Property<?> property : selectedModule.getElements()) {
            if (!property.isAvailable()) continue;

            double propertyHeight = getPropertyHeight(property);
            double drawnY = currentY - propertyScrollOffset;

            propertyBounds.put(property, new double[]{x, currentY, x + width, currentY + propertyHeight});

            if (drawnY + propertyHeight >= y && drawnY <= y + height) {
                drawProperty(property, mouseX, mouseY, x, drawnY, width, propertyHeight);
            }

            currentY += propertyHeight + PADDING / 2;
            totalHeight += propertyHeight + PADDING / 2;
        }

        RenderUtils.endScissor();

        updateScrollBounds(totalHeight, height);

        if (maxPropertyScroll > 0) {
            drawScrollBar(x, y, width, height, totalHeight);
        }
    }

    private void drawProperty(Property<?> property, int mouseX, int mouseY, double x, double y, double width, double height) {
        boolean isHovered = inBounds(mouseX, mouseY, new double[]{x + PADDING / 2, y, x + width - PADDING / 2, y + height});

        Color cardBg = isHovered ? new Color(CARD_BACKGROUND.getRed() + 6,
                CARD_BACKGROUND.getGreen() + 6,
                CARD_BACKGROUND.getBlue() + 6,
                CARD_BACKGROUND.getAlpha()) : CARD_BACKGROUND;

        drawRoundedRect(x + PADDING / 2, y, x + width - PADDING / 2, y + height - 4,
                BORDER_RADIUS * 0.6, cardBg);

        fontRenderer.drawStringWithShadow(property.getLabel(), (float) (x + PADDING + 10),
                (float) (y + PADDING + 4), TEXT_PRIMARY.getRGB());

        if (property.getType() == Boolean.class) {
            drawToggleSwitch(property, x + width - PADDING - 36, y + PADDING - 2);
        } else if (property instanceof ModeProperty) {
            drawDropdownButton((ModeProperty<?>) property, x + width - PADDING - 140, y + 4, 130);
        } else if (property instanceof NumberProperty) {
            drawModernSlider((NumberProperty) property, x + PADDING, y + PROPERTY_HEIGHT - 16, width - PADDING * 2);
        }
    }

    private void drawToggleSwitch(Property<?> property, double x, double y) {
        double switchWidth = 36;
        double switchHeight = 20;
        boolean enabled = (boolean) property.getValue();

        // Switch track
        Color trackColor = enabled ? new Color(ACCENT_PRIMARY.getRed(), ACCENT_PRIMARY.getGreen(),
                ACCENT_PRIMARY.getBlue(), 150) : new Color(71, 85, 105, 150);
        drawRoundedRect(x, y, x + switchWidth, y + switchHeight, switchHeight / 2, trackColor);

        // Switch thumb
        double thumbX = enabled ? x + switchWidth - switchHeight + 2 : x + 2;
        Color thumbColor = enabled ? ACCENT_PRIMARY : new Color(148, 163, 184);
        drawCircle((float) (thumbX + switchHeight / 2 - 2), (float) (y + switchHeight / 2),
                (float) (switchHeight / 2 - 3), thumbColor);
    }

    private void drawDropdownButton(ModeProperty<?> property, double x, double y, double width) {
        double height = 28;
        boolean isOpen = openDropdown == property;

        drawRoundedRect(x, y, x + width, y + height, BORDER_RADIUS * 0.5, BACKGROUND_TERTIARY);

        fontRenderer.drawStringWithShadow(property.getValue().toString(), (float) (x + 8),
                (float) (y + (height - fontRenderer.FONT_HEIGHT) / 2), TEXT_PRIMARY.getRGB());

        RenderUtils.ArrowDirection direction = isOpen ? RenderUtils.ArrowDirection.UP : RenderUtils.ArrowDirection.DOWN;
        RenderUtils.drawArrow((float) (x + width - 14), (float) (y + height / 2 - 2),
                4, direction, TEXT_SECONDARY.getRGB());
    }

    private void drawModernSlider(NumberProperty property, double x, double y, double width) {
        double min = property.getMin();
        double max = property.getMax();
        double value = property.getValue();
        double percentage = (value - min) / (max - min);

        double sliderHeight = 8;
        double trackY = y + 4;

        // Track background
        drawRoundedRect(x, trackY, x + width, trackY + sliderHeight, sliderHeight / 2,
                new Color(71, 85, 105, 100));

        // Active track
        drawRoundedRect(x, trackY, x + (width * percentage), trackY + sliderHeight,
                sliderHeight / 2, ACCENT_PRIMARY);

        // Thumb
        double thumbX = x + (width * percentage);
        drawCircle((float) thumbX, (float) (trackY + sliderHeight / 2), 10, ACCENT_HOVER);
        drawCircle((float) thumbX, (float) (trackY + sliderHeight / 2), 6, ACCENT_PRIMARY);

        // Value display
        String valueText = String.format("%.2f", value);
        fontRenderer.drawStringWithShadow(valueText, (float) (x),
                (float) (y - 14), TEXT_SECONDARY.getRGB());

        sliderBounds.put(property, new double[]{x, trackY, x + width, trackY + sliderHeight});
    }

    private void drawEnumDropdown(int mouseX, int mouseY) {
        double[] mainBounds = propertyBounds.get(openDropdown);
        if (mainBounds == null) return;

        double x = mainBounds[2] - PADDING - 145;
        double y = (mainBounds[1] - propertyScrollOffset) + 34;
        double width = 130;

        // Dropdown shadow
        drawRoundedRect(x + 2, y + 2, x + width + 2, y + 4, BORDER_RADIUS * 0.6,
                new Color(0, 0, 0, 80));

        enumOptionBounds.putIfAbsent(openDropdown, new HashMap<>());
        Map<Object, double[]> optionBounds = enumOptionBounds.get(openDropdown);
        optionBounds.clear();

        double optionY = y;
        int index = 0;
        Object[] values = openDropdown.getValues();

        for (Object enumValue : values) {
            double optionHeight = 32;
            optionBounds.put(enumValue, new double[]{x, optionY, x + width, optionY + optionHeight});

            boolean isHovered = inBounds(mouseX, mouseY, optionBounds.get(enumValue));
            boolean isSelected = enumValue.equals(openDropdown.getValue());

            Color bgColor;
            if (isSelected) {
                bgColor = ACCENT_PRIMARY;
            } else if (isHovered) {
                bgColor = HOVER_OVERLAY;
            } else {
                bgColor = BACKGROUND_TERTIARY;
            }

            double radius = index == 0 ? BORDER_RADIUS * 0.6 : (index == values.length - 1 ? BORDER_RADIUS * 0.6 : 0);
            drawRoundedRect(x, optionY, x + width, optionY + optionHeight, radius, bgColor);

            Color textColor = isSelected ? TEXT_PRIMARY : TEXT_SECONDARY;
            fontRenderer.drawStringWithShadow(enumValue.toString(), (float) (x + 8),
                    (float) (optionY + (optionHeight - fontRenderer.FONT_HEIGHT) / 2), textColor.getRGB());

            optionY += optionHeight;
            index++;
        }
    }

    private void drawScrollBar(double x, double propertiesY, double width, double propertiesHeight, double totalHeight) {
        double scrollBarWidth = 4;
        double scrollBarX = x + width - scrollBarWidth - 4;
        double visibleRatio = propertiesHeight / totalHeight;
        double handleHeight = Math.max(30, propertiesHeight * visibleRatio);
        double handleY = propertiesY + (propertyScrollOffset / maxPropertyScroll) * (propertiesHeight - handleHeight);

        // Scrollbar handle
        drawRoundedRect(scrollBarX, handleY, scrollBarX + scrollBarWidth, handleY + handleHeight,
                scrollBarWidth / 2, new Color(ACCENT_PRIMARY.getRed(), ACCENT_PRIMARY.getGreen(),
                        ACCENT_PRIMARY.getBlue(), 150));

        scrollBarHitbox = new double[]{scrollBarX - 6, propertiesY, scrollBarX + scrollBarWidth + 6, propertiesY + propertiesHeight};
    }

    private double getPropertyHeight(Property<?> property) {
        if (property instanceof NumberProperty) return SLIDER_PROPERTY_HEIGHT;
        if (property instanceof ModeProperty && property == openDropdown) {
            ModeProperty<?> enumProp = (ModeProperty<?>) property;
            return PROPERTY_HEIGHT + (enumProp.getValues().length * 32);
        }
        return PROPERTY_HEIGHT;
    }

    private void updateScrollBounds(double totalHeight, double visibleHeight) {
        maxPropertyScroll = Math.max(0, totalHeight - visibleHeight);
        targetPropertyScrollOffset = Math.max(0, Math.min(targetPropertyScrollOffset, maxPropertyScroll));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        AtomicBoolean consumedClick = new AtomicBoolean(false);

        if (mouseButton == 2) {
            for (Map.Entry<Module, double[]> entry : moduleBounds.entrySet()) {
                if (inBounds(mouseX, mouseY, entry.getValue())) {
                    listeningModule = entry.getKey();
                    consumedClick.set(true);
                    break;
                }
            }
            if (consumedClick.get()) return;
        }

        double dragHandleHeight = HEADER_HEIGHT;
        if (inBounds(mouseX, mouseY, new double[]{windowX, windowY, windowX + windowWidth, windowY + dragHandleHeight}) && openDropdown == null) {
            isDraggingWindow = true;
            dragStartX = mouseX - windowX;
            dragStartY = mouseY - windowY;
        }

        if (openDropdown != null && enumOptionBounds.containsKey(openDropdown)) {
            for (Map.Entry<Object, double[]> optionEntry : enumOptionBounds.get(openDropdown).entrySet()) {
                if (inBounds(mouseX, mouseY, optionEntry.getValue())) {
                    openDropdown.setValueObj(optionEntry.getKey());
                    openDropdown = null;
                    consumedClick.set(true);
                    break;
                }
            }
        }
        if (consumedClick.get()) return;

        for (Map.Entry<ModuleCategory, double[]> entry : categoryBounds.entrySet()) {
            if (inBounds(mouseX, mouseY, entry.getValue())) {
                currentCategory = entry.getKey();
                if (selectedModule != null) openDropdown = null;
                selectedModule = null;
                targetPropertyScrollOffset = propertyScrollOffset = 0;
                consumedClick.set(true);
                break;
            }
        }
        if (consumedClick.get()) return;

        for (Map.Entry<Module, double[]> entry : moduleBounds.entrySet()) {
            if (inBounds(mouseX, mouseY, entry.getValue())) {
                if (mouseButton == 0) entry.getKey().toggle();
                else if (mouseButton == 1) {
                    if (selectedModule != entry.getKey()) {
                        openDropdown = null;
                        selectedModule = entry.getKey();
                        targetPropertyScrollOffset = propertyScrollOffset = 0;
                    }
                }
                consumedClick.set(true);
                break;
            }
        }
        if (consumedClick.get()) return;

        if (selectedModule != null) {
            handleSettingsClick(mouseX, mouseY, consumedClick);
        }

        if (!consumedClick.get() && openDropdown != null) {
            openDropdown = null;
        }

        if (!consumedClick.get() && !isDraggingWindow) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void handleSettingsClick(int mouseX, int mouseY, AtomicBoolean consumedClick) {
        if (scrollBarHitbox != null && inBounds(mouseX, mouseY, scrollBarHitbox)) {
            isScrolling = true;
            scrollStartY = mouseY;
            scrollStartOffset = targetPropertyScrollOffset;
            consumedClick.set(true);
            return;
        }

        for (Map.Entry<Property<?>, double[]> entry : sliderBounds.entrySet()) {
            double[] bounds = entry.getValue();
            if (inBounds(mouseX, mouseY, new double[]{bounds[0], bounds[1] - 4, bounds[2], bounds[1] + 12})) {
                draggingSlider = true;
                draggedProperty = entry.getKey();
                updateSliderValue((NumberProperty) draggedProperty, mouseX);
                consumedClick.set(true);
                return;
            }
        }

        double propertiesAreaY = windowY + HEADER_HEIGHT + CATEGORY_HEIGHT + PADDING * 2 + fontRenderer.FONT_HEIGHT * 2 + 8;
        double propertiesAreaHeight = windowHeight - (propertiesAreaY - windowY) - PADDING;

        if (inBounds(mouseX, mouseY, new double[]{windowX, propertiesAreaY, windowX + windowWidth, propertiesAreaY + propertiesAreaHeight})) {
            for (Map.Entry<Property<?>, double[]> entry : propertyBounds.entrySet()) {
                Property<?> property = entry.getKey();
                double[] bounds = entry.getValue();
                double visibleY = bounds[1] - propertyScrollOffset;
                double componentHeight = getPropertyHeight(property);

                if (mouseY >= visibleY && mouseY <= visibleY + componentHeight) {
                    handlePropertyClick(property);
                    consumedClick.set(true);
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = false;
        draggedProperty = null;
        isScrolling = false;
        isDraggingWindow = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingWindow) {
            windowX = mouseX - dragStartX;
            windowY = mouseY - dragStartY;
            return;
        }

        if (isScrolling) {
            double deltaY = mouseY - scrollStartY;
            targetPropertyScrollOffset = Math.max(0, Math.min(maxPropertyScroll, scrollStartOffset + deltaY));
            return;
        }

        if (draggingSlider && draggedProperty != null) {
            updateSliderValue((NumberProperty) draggedProperty, mouseX);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && selectedModule != null) {
            targetPropertyScrollOffset = Math.max(0, Math.min(maxPropertyScroll, targetPropertyScrollOffset - wheel / 3.0f));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                listeningModule.setKey(Keyboard.KEY_NONE);
                Logger.chatPrint("Removed keybind for " + listeningModule.getLabel());
            } else {
                listeningModule.setKey(keyCode);
                Logger.chatPrint("Set keybind for " + listeningModule.getLabel() + " to " + Keyboard.getKeyName(keyCode));
            }
            listeningModule = null;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void handlePropertyClick(Property<?> property) {
        if (property.getType() == Boolean.class) {
            property.setValueObj(!(boolean) property.getValueObj());
        } else if (property instanceof ModeProperty) {
            openDropdown = (openDropdown == property) ? null : (ModeProperty<?>) property;
        }
    }

    private void updateSliderValue(NumberProperty property, int mouseX) {
        double[] bounds = sliderBounds.get(property);
        if (bounds == null) return;

        double sliderWidth = bounds[2] - bounds[0];
        double clickX = Math.max(0, Math.min(mouseX - bounds[0], sliderWidth));
        double percentage = clickX / sliderWidth;
        property.setValue(property.getMin() + (property.getMax() - property.getMin()) * percentage);
    }

    private void clearBounds() {
        categoryBounds.clear();
        moduleBounds.clear();
        propertyBounds.clear();
        sliderBounds.clear();
        if (openDropdown == null || !enumOptionBounds.containsKey(openDropdown)) {
            enumOptionBounds.values().forEach(Map::clear);
        }
        scrollBarHitbox = null;
    }

    private boolean inBounds(int mouseX, int mouseY, double[] bounds) {
        return bounds != null && mouseX >= bounds[0] && mouseX <= bounds[2] &&
                mouseY >= bounds[1] && mouseY <= bounds[3];
    }

    private void drawRoundedRect(double x, double y, double x2, double y2, double radius, Color color) {
        float fx = (float) x;
        float fy = (float) y;
        float fwidth = (float) (x2 - x);
        float fheight = (float) (y2 - y);
        float fradius = (float) radius;
        RenderUtils.drawRoundedRectNoShaders(fx, fy, fwidth, fheight, fradius, color.getRGB());
    }

    private void drawCircle(float x, float y, float radius, Color color) {
        RenderUtils.drawCircle(x, y, radius, color.getRGB());
    }
}