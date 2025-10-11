package cc.simp.interfaces.click;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.processes.FontProcess;
import net.minecraft.client.gui.GuiScreen;
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

    // GameSense color scheme
    private static final Color BG_COLOR = new Color(20, 20, 20, 200);
    private static final Color PANEL_BG = new Color(25, 25, 25, 220);
    private static final Color ACCENT_COLOR = new Color(150, 150, 255);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color HOVER_COLOR = new Color(35, 35, 35);
    private static final Color DISABLED_COLOR = new Color(100, 100, 100);

    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_SPACING = 10;
    private static final int TOP_MARGIN = 50;

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

        // Update dragging slider if active
        if (draggingSlider != null) {
            draggingSlider.updateDrag(mouseX);
        }

        for (CategoryPanel panel : panels) {
            panel.render(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (CategoryPanel panel : panels) {
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

            // Check if mouse is over any panel content area for scrolling
            for (CategoryPanel panel : panels) {
                if (mouseX >= panel.x && mouseX <= panel.x + panel.width &&
                        mouseY >= panel.y + panel.headerHeight) {
                    panel.handleScroll(mouseX, mouseY, wheel);
                    handledByPanel = true;
                    break;
                }
            }

            // If not over panel content, move panels horizontally
            if (!handledByPanel && wheel != 0) {
                int scrollAmount = wheel > 0 ? 15 : -15;
                for (CategoryPanel panel : panels) {
                    panel.x += scrollAmount;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
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
        private int headerHeight = 16;
        private boolean dragging = false;
        private int dragX, dragY;
        private float scrollOffset = 0;
        private float targetScroll = 0;

        private final List<ModuleButton> modules = new ArrayList<>();

        public CategoryPanel(ModuleCategory category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;

            for (Module module : Simp.INSTANCE.getModuleManager().getModulesForCategory(category)) {
                modules.add(new ModuleButton(module, this));
            }
        }

        public void render(int mouseX, int mouseY) {
            // Handle dragging
            if (dragging) {
                x = mouseX - dragX;
                y = mouseY - dragY;
            }

            // Smooth scroll interpolation
            scrollOffset += (targetScroll - scrollOffset) * 0.2f;

            // Calculate total height and max scroll
            int totalHeight = 0;
            for (ModuleButton moduleButton : modules) {
                totalHeight += moduleButton.getTotalHeight();
            }

            int maxVisibleHeight = height - y - headerHeight - 20;
            int maxScroll = Math.max(0, totalHeight - maxVisibleHeight);
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));

            // Header background
            drawRect(x, y, x + width, y + headerHeight, PANEL_BG.getRGB());

            // Category name
            font.drawString(category.name(), x + 4, y + 5, TEXT_COLOR.getRGB());

            // Panel body background
            int bodyHeight = Math.min(totalHeight, maxVisibleHeight);
            drawRect(x, y + headerHeight, x + width, y + headerHeight + bodyHeight, BG_COLOR.getRGB());

            // Render modules
            int moduleY = y + headerHeight - (int)scrollOffset;

            for (ModuleButton moduleButton : modules) {
                int buttonHeight = moduleButton.getTotalHeight();

                // Only render if visible in viewport
                if (moduleY + buttonHeight > y + headerHeight &&
                        moduleY < y + headerHeight + bodyHeight) {
                    moduleButton.render(x, moduleY, width, mouseX, mouseY);
                }

                moduleY += buttonHeight;
            }

            // Draw scrollbar if needed
            if (totalHeight > maxVisibleHeight) {
                drawScrollbar(y + headerHeight, bodyHeight, totalHeight, maxScroll);
            }
        }

        private void drawScrollbar(int startY, int visibleHeight, int totalHeight, int maxScroll) {
            int scrollbarX = x + width - 2;
            int scrollbarWidth = 2;

            // Background track
            drawRect(scrollbarX, startY, scrollbarX + scrollbarWidth,
                    startY + visibleHeight, new Color(40, 40, 40, 180).getRGB());

            // Calculate thumb size and position
            float thumbSize = Math.max(20, (float)visibleHeight / totalHeight * visibleHeight);
            float thumbPos = maxScroll > 0 ? (scrollOffset / maxScroll) * (visibleHeight - thumbSize) : 0;

            // Thumb
            drawRect(scrollbarX, (int)(startY + thumbPos),
                    scrollbarX + scrollbarWidth, (int)(startY + thumbPos + thumbSize),
                    ACCENT_COLOR.getRGB());
        }

        public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            // Header dragging
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (mouseButton == 0) {
                    dragging = true;
                    dragX = mouseX - x;
                    dragY = mouseY - y;
                    return true;
                }
            }

            // Module clicks
            int moduleY = y + headerHeight - (int)scrollOffset;
            int maxY = y + headerHeight + (height - y - headerHeight - 20);

            for (ModuleButton moduleButton : modules) {
                // Only process clicks for visible modules
                if (moduleY + 16 > y + headerHeight && moduleY < maxY) {
                    if (moduleButton.mouseClicked(x, moduleY, width, mouseX, mouseY, mouseButton)) {
                        return true;
                    }
                }
                moduleY += moduleButton.getTotalHeight();
            }

            return false;
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            dragging = false;

            int moduleY = y + headerHeight - (int)scrollOffset;
            for (ModuleButton moduleButton : modules) {
                moduleButton.mouseReleased(mouseX, mouseY, state);
                moduleY += moduleButton.getTotalHeight();
            }
        }

        public void handleScroll(int mouseX, int mouseY, int wheel) {
            targetScroll -= wheel / 120f * 20;
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
            int height = 16;
            if (expanded) {
                for (SettingComponent setting : settings) {
                    if (setting.property.isAvailable()) {
                        height += setting.getHeight();
                    }
                }
            }
            return height;
        }

        public void render(int x, int y, int width, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;

            // Module button background
            Color bgColor = module.isEnabled() ? ACCENT_COLOR : (hovered ? HOVER_COLOR : BG_COLOR);
            drawRect(x, y, x + width, y + 16, bgColor.getRGB());

            // Module name
            String name = module == listeningModule ? "Listening..." : module.getLabel();
            Color textColor = module.isEnabled() ? Color.WHITE : TEXT_COLOR;
            font.drawString(name, x + 4, y + 5, textColor.getRGB());

            // Keybind indicator
            if (module.getKey() != 0 && module != listeningModule) {
                String keyName = Keyboard.getKeyName(module.getKey());
                int keyWidth = font.getStringWidth(keyName);
                int textX = x + width - keyWidth - (settings.isEmpty() ? 4 : 14);
                font.drawString(keyName, textX, y + 5,
                        new Color(150, 150, 150, 180).getRGB());
            }

            // Expand indicator
            if (!settings.isEmpty()) {
                String arrow = expanded ? "▼" : "▶";
                font.drawString(arrow, x + width - 10, y + 5,
                        new Color(150, 150, 150).getRGB());
            }

            // Settings
            if (expanded) {
                int settingY = y + 16;
                for (SettingComponent setting : settings) {
                    if (setting.property.isAvailable()) {
                        setting.render(x, settingY, width, mouseX, mouseY);
                        settingY += setting.getHeight();
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
                    if (!settings.isEmpty()) {
                        expanded = !expanded;
                    }
                } else if (mouseButton == 2) {
                    listeningModule = module;
                }
                return true;
            }

            if (expanded) {
                int settingY = y + 16;
                for (SettingComponent setting : settings) {
                    if (setting.property.isAvailable()) {
                        if (setting.mouseClicked(x, settingY, width, mouseX, mouseY, mouseButton)) {
                            return true;
                        }
                        settingY += setting.getHeight();
                    }
                }
            }

            return false;
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            if (expanded) {
                for (SettingComponent setting : settings) {
                    setting.mouseReleased(mouseX, mouseY);
                }
            }
        }
    }

    private class SettingComponent {
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
            boolean hovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= y && mouseY <= y + 16;

            // Background
            Color bgColor = hovered ? new Color(30, 30, 30, 220) : PANEL_BG;
            drawRect(x, y, x + width, y + 16, bgColor.getRGB());

            if (property.getType() == Boolean.class) {
                renderBooleanSetting(x, y, width);
            } else if (property instanceof NumberProperty) {
                renderNumberSetting(x, y, width, mouseX);
            } else if (property instanceof ModeProperty) {
                renderModeSetting(x, y, width, mouseX, mouseY);
            }
        }

        private void renderBooleanSetting(int x, int y, int width) {
            boolean value = (Boolean) property.getValue();

            // Toggle switch
            int switchWidth = 18;
            int switchHeight = 8;
            int switchX = x + width - switchWidth - 4;
            int switchY = y + 4;

            // Background with smooth color transition
            Color bgColor = value ? ACCENT_COLOR.darker() : new Color(60, 60, 60);
            drawRect(switchX, switchY, switchX + switchWidth, switchY + switchHeight,
                    bgColor.getRGB());

            // Knob
            int knobSize = 6;
            int knobX = value ? switchX + switchWidth - knobSize - 1 : switchX + 1;
            Color knobColor = value ? ACCENT_COLOR.brighter() : new Color(120, 120, 120);
            drawRect(knobX, switchY + 1, knobX + knobSize, switchY + switchHeight - 1,
                    knobColor.getRGB());

            // Label
            font.drawString(property.getLabel(), x + 4, y + 4, TEXT_COLOR.getRGB());
        }

        private void renderNumberSetting(int x, int y, int width, int mouseX) {
            NumberProperty numProp = (NumberProperty) property;
            double value = numProp.getValue();
            double min = numProp.getMin();
            double max = numProp.getMax();
            double percent = (value - min) / (max - min);

            // Format value cleanly
            String valueStr = formatNumber(value);

            // Label
            font.drawString(property.getLabel(), x + 4, y + 4, TEXT_COLOR.getRGB());

            // Value display
            int valueWidth = font.getStringWidth(valueStr);
            font.drawString(valueStr, x + width - valueWidth - 4, y + 4,
                    ACCENT_COLOR.getRGB());

            // Slider area
            int sliderY = y + 13;
            int sliderHeight = 2;
            int sliderPadding = 4;

            // Check if hovering over slider
            boolean sliderHovered = mouseX >= x + sliderPadding &&
                    mouseX <= x + width - sliderPadding &&
                    mouseX >= x && mouseX <= x + width;

            // Slider background track
            drawRect(x + sliderPadding, sliderY, x + width - sliderPadding, sliderY + sliderHeight,
                    new Color(60, 60, 60).getRGB());

            // Slider filled portion
            int filledWidth = (int)((width - sliderPadding * 2) * percent);
            drawRect(x + sliderPadding, sliderY, x + sliderPadding + filledWidth, sliderY + sliderHeight,
                    ACCENT_COLOR.getRGB());

            // Slider thumb
            if (draggingSlider == this || sliderHovered) {
                int thumbX = x + sliderPadding + filledWidth;
                int thumbSize = 4;
                Color thumbColor = draggingSlider == this ? ACCENT_COLOR.brighter() : ACCENT_COLOR;
                drawRect(thumbX - thumbSize / 2, sliderY - 2,
                        thumbX + thumbSize / 2, sliderY + sliderHeight + 2,
                        thumbColor.getRGB());
            }
        }

        private void renderModeSetting(int x, int y, int width, int mouseX, int mouseY) {
            ModeProperty<?> modeProp = (ModeProperty<?>) property;

            // Label with current value
            String displayText = property.getLabel() + ": " + modeProp.getValue();
            font.drawString(displayText, x + 4, y + 4, TEXT_COLOR.getRGB());

            // Dropdown arrow
            String arrow = dropdownOpen ? "▲" : "▼";
            font.drawString(arrow, x + width - 10, y + 4, new Color(150, 150, 150).getRGB());

            // Dropdown options
            if (dropdownOpen) {
                int optionY = y + 16;
                for (Object value : modeProp.getValues()) {
                    boolean selected = value.equals(modeProp.getValue());
                    boolean hovered = mouseX >= x + 2 && mouseX <= x + width - 2 &&
                            mouseY >= optionY && mouseY <= optionY + 12;

                    Color color;
                    if (selected) {
                        color = ACCENT_COLOR;
                    } else if (hovered) {
                        color = HOVER_COLOR.brighter();
                    } else {
                        color = new Color(30, 30, 30, 220);
                    }

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
            } else if (property instanceof NumberProperty) {
                int sliderY = y + 11;
                if (mouseX >= x + 4 && mouseX <= x + width - 4 &&
                        mouseY >= sliderY && mouseY <= sliderY + 6) {
                    draggingSlider = this;
                    dragStartX = mouseX;
                    componentX = x;
                    componentWidth = width;
                    updateSlider(x, width, mouseX);
                    return true;
                }
            } else if (property instanceof ModeProperty) {
                if (dropdownOpen) {
                    ModeProperty<?> modeProp = (ModeProperty<?>) property;
                    int optionY = y + 16;
                    for (Object value : modeProp.getValues()) {
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 &&
                                mouseY >= optionY && mouseY <= optionY + 12) {
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
            // Slider dragging is handled globally in ClickInterface
        }

        public void updateDrag(int mouseX) {
            if (property instanceof NumberProperty && draggingSlider == this) {
                updateSlider(componentX, componentWidth, mouseX);
            }
        }

        private void updateSlider(int x, int width, int mouseX) {
            if (property instanceof NumberProperty) {
                NumberProperty numProp = (NumberProperty) property;
                double percent = Math.max(0, Math.min(1, (mouseX - x - 4.0) / (width - 8.0)));

                double range = numProp.getMax() - numProp.getMin();
                double rawValue = numProp.getMin() + range * percent;

                // Apply increment steps
                double increment = numProp.getIncrement();
                if (increment > 0) {
                    rawValue = Math.round(rawValue / increment) * increment;
                }

                // Clamp to bounds
                double finalValue = Math.max(numProp.getMin(), Math.min(numProp.getMax(), rawValue));
                numProp.setValue(finalValue);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
