package cc.simp.interfaces.click;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.impl.client.ClientSettingsModule;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
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
import java.util.List;

public class ClickInterface extends GuiScreen {

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final CustomFontRenderer font = FontProcess.getFont("simp");
    private Module listeningModule = null;
    private SettingComponent draggingSlider = null;
    private SettingComponent editingString = null;
    private String editingBuffer = "";

    // Visual theme
    private static final Color BG_COLOR = new Color(22, 22, 22, 220);
    private static final Color PANEL_BG = new Color(28, 28, 28, 230);
    private static Color ACCENT_COLOR = new Color(120, 145, 255);
    private static final Color TEXT_COLOR = new Color(210, 210, 210);
    private static final Color HOVER_COLOR = new Color(45, 45, 45);

    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_SPACING = 12;
    private static final int TOP_MARGIN = 48;

    @Override
    public void initGui() {
        panels.clear();
        int x = 20;
        for (ModuleCategory category : ModuleCategory.values()) {
            CategoryPanel panel = new CategoryPanel(category, x, TOP_MARGIN);
            panels.add(panel);
            x += PANEL_WIDTH + PANEL_SPACING;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ACCENT_COLOR = ColorProcess.getColor();

        ClientSettingsModule.renderAnimeImage(width, height);

        // Update dragging slider if active
        if (draggingSlider != null) {
            draggingSlider.updateDrag(mouseX);
        }

        // Render panels and allow them to update interactive drag states
        for (CategoryPanel panel : panels) {
            panel.render(mouseX, mouseY);
            panel.updateDrag(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (CategoryPanel panel : panels) {
            // if a panel handled the click, stop propagation
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = null;
        for (CategoryPanel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, state);
            panel.clampToScreen();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;

            boolean handledByPanel = false;
            for (CategoryPanel panel : panels) {
                if (mouseX >= panel.x && mouseX <= panel.x + panel.width &&
                        mouseY >= panel.y + panel.headerHeight && mouseY <= panel.y + height) {
                    panel.handleScroll(mouseX, mouseY, wheel);
                    handledByPanel = true;
                    break;
                }
            }

            if (!handledByPanel) {
                int scrollAmount = wheel > 0 ? 18 : -18;
                for (CategoryPanel panel : panels) {
                    panel.x += scrollAmount;
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
                (editingString.property).setValueObj(editingBuffer);
                editingString = null;
                editingBuffer = "";
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!editingBuffer.isEmpty()) {
                    editingBuffer = editingBuffer.substring(0, editingBuffer.length() - 1);
                }
            } else if (isCtrlKeyDown()) {
                // Handle Ctrl+V (paste)
                if (keyCode == Keyboard.KEY_V) {
                    String clipboard = GuiScreen.getClipboardString();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        for (char c : clipboard.toCharArray()) {
                            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                                editingBuffer += c;
                            }
                        }
                    }
                }
                // Handle Ctrl+A (select all / clear for replacement)
                else if (keyCode == Keyboard.KEY_A) {
                    // In this context, we can clear the buffer so next input replaces it
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

    private class CategoryPanel {
        private final ModuleCategory category;
        private int x, y;
        private int width = PANEL_WIDTH;
        private int headerHeight = 18;
        private boolean dragging = false;
        private int dragX, dragY;
        private float scrollOffset = 0f;
        private float targetScroll = 0f;
        private boolean draggingScrollbar = false;
        private float scrollbarDragStartY;
        private float scrollbarStartScroll;

        private final List<ModuleButton> modules = new ArrayList<>();
        private final List<ConfigButton> configs = new ArrayList<>();

        public CategoryPanel(ModuleCategory category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;

            if (category == ModuleCategory.CONFIGS) {
                new Thread(() -> {
                    List<String> configList = GitHubConfigFetcher.fetchConfigList();
                    for (String configName : configList) {
                        configs.add(new ConfigButton(configName, this));
                    }
                }).start();
            } else {
                for (Module module : Simp.INSTANCE.getModuleManager().getModulesForCategory(category)) {
                    modules.add(new ModuleButton(module, this));
                }
            }
        }

        public void render(int mouseX, int mouseY) {
            if (dragging) {
                x = mouseX - dragX;
                y = mouseY - dragY;
            }

            scrollOffset = RenderUtils.lerp(scrollOffset, targetScroll, 0.18f);

            int totalHeight = 0;
            if (category == ModuleCategory.CONFIGS) {
                totalHeight = configs.size() * 16;
            } else {
                for (ModuleButton mb : modules) totalHeight += mb.getTotalHeight();
            }

            int maxVisibleHeight = height - y - headerHeight - 20;
            int maxScroll = Math.max(0, totalHeight - Math.max(0, maxVisibleHeight));
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            drawRect(x, y, x + width, y + headerHeight, PANEL_BG.getRGB());
            drawRect(x, y + headerHeight - 1, x + width, y + headerHeight, new Color(40, 40, 40).getRGB());
            font.drawString(category.name(), x + 6, y + 4, TEXT_COLOR.getRGB());

            int bodyHeight = Math.min(totalHeight, maxVisibleHeight);
            drawRect(x, y + headerHeight, x + width, y + headerHeight + bodyHeight, BG_COLOR.getRGB());

            RenderUtils.startScissor(x, y + headerHeight, width, bodyHeight);

            if (category == ModuleCategory.CONFIGS) {
                int configY = y + headerHeight - (int) scrollOffset;
                for (ConfigButton cb : configs) {
                    cb.render(x, configY, width, mouseX, mouseY);
                    configY += cb.getTotalHeight();
                }
            } else {
                int moduleY = y + headerHeight - (int) scrollOffset;
                for (ModuleButton mb : modules) {
                    mb.render(x, moduleY, width, mouseX, mouseY);
                    moduleY += mb.getTotalHeight();
                }
            }

            RenderUtils.endScissor();

            if (totalHeight > maxVisibleHeight) {
                drawScrollbar(y + headerHeight, bodyHeight, totalHeight, maxScroll);
            }
        }

        private void drawScrollbar(int startY, int visibleHeight, int totalHeight, int maxScroll) {
            int scrollbarX = x + width - 6;
            int scrollbarWidth = 6;

            drawRect(scrollbarX, startY, scrollbarX + scrollbarWidth, startY + visibleHeight, new Color(30, 30, 30, 180).getRGB());

            float thumbSize = Math.max(24f, (float) visibleHeight / (float) totalHeight * visibleHeight);
            float thumbPos = maxScroll > 0 ? (scrollOffset / maxScroll) * (visibleHeight - thumbSize) : 0f;

            int y1 = (int) (startY + thumbPos);
            int y2 = (int) (startY + thumbPos + thumbSize);

            drawRect(scrollbarX + 1, y1, scrollbarX + scrollbarWidth - 1, y2, ACCENT_COLOR.getRGB());

            drawRect(scrollbarX, y1, scrollbarX + 1, y2, new Color(0, 0, 0, 120).getRGB());
            drawRect(scrollbarX + scrollbarWidth - 1, y1, scrollbarX + scrollbarWidth, y2, new Color(0, 0, 0, 120).getRGB());
        }

        public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (mouseButton == 0) {
                    dragging = true;
                    dragX = mouseX - x;
                    dragY = mouseY - y;
                    return true;
                }
            }

            int totalHeight = 0;
            if (category == ModuleCategory.CONFIGS) {
                totalHeight = configs.size() * 16;
            } else {
                for (ModuleButton mb : modules) totalHeight += mb.getTotalHeight();
            }

            int maxVisibleHeight = height - y - headerHeight - 20;
            int maxScroll = Math.max(0, totalHeight - Math.max(0, maxVisibleHeight));

            if (totalHeight > maxVisibleHeight) {
                int scrollbarX = x + width - 6;
                int scrollbarWidth = 6;
                float thumbSize = Math.max(24f, (float) maxVisibleHeight / (float) totalHeight * maxVisibleHeight);
                float thumbPos = maxScroll > 0 ? (scrollOffset / maxScroll) * (maxVisibleHeight - thumbSize) : 0f;
                int y1 = (int) (y + headerHeight + thumbPos);
                int y2 = (int) (y + headerHeight + thumbPos + thumbSize);

                if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= y1 && mouseY <= y2) {
                    if (mouseButton == 0) {
                        draggingScrollbar = true;
                        scrollbarDragStartY = mouseY;
                        scrollbarStartScroll = scrollOffset;
                        return true;
                    }
                }
            }

            if (category == ModuleCategory.CONFIGS) {
                int configY = y + headerHeight - (int) scrollOffset;
                for (ConfigButton cb : configs) {
                    if (cb.mouseClicked(x, configY, width, mouseX, mouseY, mouseButton)) {
                        return true;
                    }
                    configY += cb.getTotalHeight();
                }
            } else {
                int moduleY = y + headerHeight - (int) scrollOffset;
                int maxY = y + headerHeight + (height - y - headerHeight - 20);
                for (ModuleButton mb : modules) {
                    int moduleHeight = mb.getTotalHeight();
                    if (moduleY + moduleHeight > y + headerHeight && moduleY < maxY) {
                        if (mb.mouseClicked(x, moduleY, width, mouseX, mouseY, mouseButton)) {
                            return true;
                        }
                    }
                    moduleY += moduleHeight;
                }
            }

            return false;
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            dragging = false;
            draggingScrollbar = false;
            for (ModuleButton mb : modules) {
                mb.mouseReleased(mouseX, mouseY, state);
            }
        }

        public void handleScroll(int mouseX, int mouseY, int wheel) {
            int scrollAmount = wheel > 0 ? 15 : -15;
            targetScroll -= scrollAmount;
        }

        public void updateDrag(int mouseX, int mouseY) {
            if (draggingScrollbar) {
                int totalHeight = 0;
                if (category == ModuleCategory.CONFIGS) {
                    totalHeight = configs.size() * 16;
                } else {
                    for (ModuleButton mb : modules) totalHeight += mb.getTotalHeight();
                }

                int maxVisibleHeight = height - y - headerHeight - 20;
                int maxScroll = Math.max(0, totalHeight - Math.max(0, maxVisibleHeight));

                if (maxScroll > 0) {
                    float thumbTrack = maxVisibleHeight - Math.max(24f, (float) maxVisibleHeight / (float) totalHeight * maxVisibleHeight);
                    if (thumbTrack <= 0) return;
                    float dy = mouseY - scrollbarDragStartY;
                    float scrollDelta = dy / thumbTrack * maxScroll;
                    targetScroll = Math.max(0f, Math.min(maxScroll, scrollbarStartScroll + scrollDelta));
                }
            }
        }

        public void clampToScreen() {
            int minX = -width + 30;
            int maxX = ClickInterface.this.width - 30;
            if (x < minX) x = minX;
            if (x > maxX) x = maxX;
        }
    }


    private class ModuleButton {
        private final Module module;
        private final CategoryPanel parent;
        private boolean expanded = false;
        private final List<SettingComponent> settings = new ArrayList<>();

        public ModuleButton(Module module, CategoryPanel parent) {
            this.module = module;
            this.parent = parent;
            for (Property<?> property : module.getElements()) {
                settings.add(new SettingComponent(property));
            }
        }

        public int getTotalHeight() {
            int h = 16;
            if (expanded) {
                for (SettingComponent s : settings) {
                    if (s.property.isAvailable()) h += s.getHeight();
                }
            }
            return h;
        }

        public void render(int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;

            Color bgColor = module.isEnabled() ? ACCENT_COLOR.darker() : (hovered ? HOVER_COLOR : BG_COLOR);
            drawRect(x, y, x + width, y + 16, bgColor.getRGB());

            // name
            String name = module == listeningModule ? "Listening..." : module.getLabel();
            Color textColor = module.isEnabled() ? Color.WHITE : TEXT_COLOR;
            font.drawString(name, x + 4, y + 4, textColor.getRGB());

            // keybind
            if (module.getKey() != 0 && module != listeningModule) {
                String keyName = Keyboard.getKeyName(module.getKey());
                int keyWidth = font.getStringWidth(keyName);
                int textX = x + width - keyWidth - (settings.isEmpty() ? 6 : 18);
                font.drawString(keyName, textX, y + 4, new Color(160, 160, 160, 200).getRGB());
            }

            // expand arrow
            if (!settings.isEmpty()) {
                String arrow = expanded ? "▼" : "▶";
                font.drawString(arrow, x + width - 10, y + 4, new Color(160, 160, 160).getRGB());
            }

            if (expanded) {
                int settingY = y + 16;
                for (SettingComponent s : settings) {
                    if (s.property.isAvailable()) {
                        s.render(x, settingY, width, mouseX, mouseY);
                        settingY += s.getHeight();
                    }
                }
            }
        }

        public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY, int mouseButton) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
            if (hovered) {
                if (mouseButton == 0) {
                    module.toggle();
                } else if (mouseButton == 1) {
                    if (!settings.isEmpty()) expanded = !expanded;
                } else if (mouseButton == 2) {
                    listeningModule = module;
                }
                return true;
            }

            if (expanded) {
                int settingY = y + 16;
                for (SettingComponent s : settings) {
                    if (s.property.isAvailable()) {
                        if (s.mouseClicked(x, settingY, width, mouseX, mouseY, mouseButton)) {
                            return true;
                        }
                        settingY += s.getHeight();
                    }
                }
            }
            return false;
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            if (expanded) {
                for (SettingComponent s : settings) s.mouseReleased(mouseX, mouseY);
            }
        }
    }

    public class SettingComponent {
        private final Property<?> property;
        private boolean dropdownOpen = false;
        private int dragStartX = 0;
        private int componentX = 0;
        private int componentWidth = 0;

        public SettingComponent(Property<?> property) {
            this.property = property;
        }

        public int getHeight() {
            if (property instanceof ModeProperty && dropdownOpen) {
                return 16 + ((ModeProperty<?>) property).getValues().length * 12;
            }
            return 17;
        }

        public void render(int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
            Color bgColor = hovered ? new Color(36, 36, 36, 230) : PANEL_BG;
            drawRect(x, y, x + width, y + 16, bgColor.getRGB());

            if (property.getType() == Boolean.class) {
                renderBooleanSetting(x, y, width);
            } else if (property instanceof NumberProperty) {
                renderNumberSetting(x, y, width, mouseX, mouseY);
            } else if (property instanceof ModeProperty) {
                renderModeSetting(x, y, width, mouseX, mouseY);
            } else if (property.getType() == String.class) {
                renderStringSetting(x, y, width, mouseX, mouseY);
            }
        }

        private void renderStringSetting(int x, int y, int width, int mouseX, int mouseY) {
            font.drawString(property.getLabel(), x + 4, y + 2, TEXT_COLOR.getRGB());

            String displayValue;
            Color valueColor;

            if (editingString == this) {
                displayValue = editingBuffer + (System.currentTimeMillis() % 1000 < 500 ? "_" : "");
                valueColor = ACCENT_COLOR.brighter();
            } else {
                String value = (String) property.getValue();
                displayValue = value.isEmpty() ? "..." : value;
                valueColor = ACCENT_COLOR;
            }

            int valueWidth = font.getStringWidth(displayValue);
            font.drawString(displayValue, x + width - valueWidth - 6, y + 2, valueColor.getRGB());

            int underlineY = y + 13;
            Color underlineColor = editingString == this ? ACCENT_COLOR : new Color(80, 80, 80);
            drawRect(x + 6, underlineY, x + width - 6, underlineY + 1, underlineColor.getRGB());
        }

        private void renderBooleanSetting(int x, int y, int width) {
            boolean value = (Boolean) property.getValue();
            int switchWidth = 18;
            int switchHeight = 8;
            int switchX = x + width - switchWidth - 6;
            int switchY = y + 4;

            Color bgColor = value ? ACCENT_COLOR.darker() : new Color(70, 70, 70);
            drawRect(switchX, switchY, switchX + switchWidth, switchY + switchHeight, bgColor.getRGB());

            int knobSize = 6;
            int knobX = value ? switchX + switchWidth - knobSize - 1 : switchX + 1;
            Color knobColor = value ? ACCENT_COLOR.brighter() : new Color(140, 140, 140);
            drawRect(knobX, switchY + 1, knobX + knobSize, switchY + switchHeight - 1, knobColor.getRGB());

            font.drawString(property.getLabel(), x + 4, y + 3, TEXT_COLOR.getRGB());
        }

        private void renderNumberSetting(int x, int y, int width, int mouseX, int mouseY) {
            NumberProperty numProp = (NumberProperty) property;
            double value = numProp.getValue();
            double min = numProp.getMin();
            double max = numProp.getMax();
            double percent = (max - min) > 0 ? (value - min) / (max - min) : 0;

            String valueStr = formatNumber(value);
            font.drawString(property.getLabel(), x + 4, y + 2, TEXT_COLOR.getRGB());
            int valueWidth = font.getStringWidth(valueStr);
            font.drawString(valueStr, x + width - valueWidth - 6, y + 2, ACCENT_COLOR.getRGB());

            // slider
            int sliderY = y + 12;
            int sliderHeight = 3;
            int sliderPadding = 6;
            int sliderLeft = x + sliderPadding;
            int sliderRight = x + width - sliderPadding;

            // hover detection for slider requires both X and Y
            boolean sliderHovered = mouseX >= sliderLeft && mouseX <= sliderRight && mouseY >= sliderY - 3 && mouseY <= sliderY + sliderHeight + 3;

            drawRect(sliderLeft, sliderY, sliderRight, sliderY + sliderHeight, new Color(60, 60, 60).getRGB());

            int filledWidth = (int) ((sliderRight - sliderLeft) * percent);
            drawRect(sliderLeft, sliderY, sliderLeft + filledWidth, sliderY + sliderHeight, ACCENT_COLOR.getRGB());

            if (draggingSlider == this || sliderHovered) {
                int thumbX = sliderLeft + filledWidth;
                int thumbSize = 6;
                Color thumbColor = draggingSlider == this ? ACCENT_COLOR.brighter() : ACCENT_COLOR;
                drawRect(thumbX - thumbSize / 2, sliderY - 3, thumbX + thumbSize / 2, sliderY + sliderHeight + 3, thumbColor.getRGB());
            }
        }

        private void renderModeSetting(int x, int y, int width, int mouseX, int mouseY) {
            ModeProperty<?> modeProp = (ModeProperty<?>) property;
            String displayText = property.getLabel() + ": " + modeProp.getValue();
            font.drawString(displayText, x + 4, y + 3, TEXT_COLOR.getRGB());

            String arrow = dropdownOpen ? "▲" : "▼";
            font.drawString(arrow, x + width - 10, y + 3, new Color(160, 160, 160).getRGB());

            if (dropdownOpen) {
                int optionY = y + 16;
                for (Object value : modeProp.getValues()) {
                    boolean selected = value.equals(modeProp.getValue());
                    boolean hovered = mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= optionY && mouseY <= optionY + 12;

                    Color color;
                    if (selected) color = ACCENT_COLOR;
                    else if (hovered) color = HOVER_COLOR.brighter();
                    else color = new Color(34, 34, 34, 220);

                    drawRect(x + 2, optionY, x + width - 2, optionY + 12, color.getRGB());
                    font.drawString(value.toString(), x + 6, optionY + 2, TEXT_COLOR.getRGB());
                    optionY += 12;
                }
            }
        }

        private String formatNumber(double value) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(value % 1 == 0 ? 0 : 2, RoundingMode.HALF_UP);
            return bd.stripTrailingZeros().toPlainString();
        }

        public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY, int mouseButton) {
            if (property.getType() == Boolean.class) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16) {
                    property.setValueObj(!(Boolean) property.getValue());
                    return true;
                }
            } else if (property.getType() == String.class) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16) {
                    editingString = this;
                    editingBuffer = (String) property.getValue();
                    return true;
                }
            } else if (property instanceof NumberProperty) {
                int sliderY = y + 12;
                int sliderPadding = 6;
                int sliderLeft = x + sliderPadding;
                int sliderRight = x + width - sliderPadding;
                int sliderTop = sliderY - 4;
                int sliderBottom = sliderY + 7;
                if (mouseX >= sliderLeft && mouseX <= sliderRight && mouseY >= sliderTop && mouseY <= sliderBottom) {
                    draggingSlider = this;
                    dragStartX = mouseX;
                    componentX = x;
                    componentWidth = width;
                    updateSlider(x, width, mouseX);
                    return true;
                }
            } else if (property instanceof ModeProperty) {
                ModeProperty<?> modeProp = (ModeProperty<?>) property;
                if (dropdownOpen) {
                    int optionY = y + 16;
                    for (Object value : modeProp.getValues()) {
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= optionY && mouseY <= optionY + 12) {
                            modeProp.setValueObj(value);
                            dropdownOpen = false;
                            return true;
                        }
                        optionY += 12;
                    }
                }
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16) {
                    dropdownOpen = !dropdownOpen;
                    return true;
                }
            }
            return false;
        }

        public void mouseReleased(int mouseX, int mouseY) {
            // handled globally
        }

        public void updateDrag(int mouseX) {
            if (property instanceof NumberProperty && draggingSlider == this) {
                updateSlider(componentX, componentWidth, mouseX);
            }
        }

        private void updateSlider(int x, int width, int mouseX) {
            if (property instanceof NumberProperty) {
                NumberProperty numProp = (NumberProperty) property;
                int sliderPadding = 6;
                double percent = Math.max(0, Math.min(1, (mouseX - (x + sliderPadding)) / (double) (width - sliderPadding * 2)));
                double range = numProp.getMax() - numProp.getMin();
                double rawValue = numProp.getMin() + range * percent;
                double increment = numProp.getIncrement();
                if (increment > 0) rawValue = Math.round(rawValue / increment) * increment;
                double finalValue = Math.max(numProp.getMin(), Math.min(numProp.getMax(), rawValue));
                numProp.setValue(finalValue);
            }
        }
    }

    private class ConfigButton {
        private final String configName;
        private final CategoryPanel parent;

        public ConfigButton(String configName, CategoryPanel parent) {
            this.configName = configName;
            this.parent = parent;
        }

        public int getTotalHeight() {
            return 16;
        }

        public void render(int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
            Color bgColor = hovered ? HOVER_COLOR : BG_COLOR;
            drawRect(x, y, x + width, y + 16, bgColor.getRGB());

            font.drawString(configName, x + 4, y + 4, TEXT_COLOR.getRGB());
            font.drawString("↓", x + width - 10, y + 4, ACCENT_COLOR.getRGB());
        }

        public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY, int mouseButton) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
            if (hovered && mouseButton == 0) {
                new Thread(() -> {
                    boolean success = GitHubConfigFetcher.downloadAndLoadConfig(configName);
                    if (success) {
                        cc.simp.utils.client.Logger.chatPrint("Config '" + configName + "' downloaded and loaded.");
                    } else {
                        cc.simp.utils.client.Logger.chatPrint("Failed to download config '" + configName + "'.");
                    }
                }).start();
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
