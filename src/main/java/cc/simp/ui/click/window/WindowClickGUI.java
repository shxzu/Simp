package cc.simp.ui.click.window;

import cc.simp.Simp;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.property.Property;
import cc.simp.property.impl.BooleanProperty;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowClickGUI extends GuiScreen {

    private final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
    private ModuleCategory currentCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;

    // UI States
    private EnumProperty<?> openDropdown = null;
    private boolean draggingSlider = false;
    private Property<?> draggedProperty = null;
    private boolean isScrolling = false;

    // --- Window Position and Dragging ---
    private static double windowX, windowY;
    private static boolean hasInitialisedPosition = false;
    private double windowWidth, windowHeight;

    private boolean isDraggingWindow = false;
    private double dragStartX, dragStartY;

    // Animation & Scrolling
    private double propertyScrollOffset = 0;
    private double targetPropertyScrollOffset = 0;
    private double maxPropertyScroll = 0;
    private double scrollStartY = 0;
    private double scrollStartOffset = 0;

    // Color Palette (Skeet.cc inspired)
    private final Color bgColor = new Color(25, 25, 25);
    private final Color headerColor = new Color(35, 35, 35);
    private final Color textColor = new Color(200, 200, 200);
    private final int accentColor = getRainbowColor(0); // Rainbow accent
    private final Color componentBgColor = new Color(45, 45, 45);
    private final Color dropdownHoverColor = new Color(55, 55, 55);
    private final Color outlineColor = new Color(60, 60, 60);

    // UI Element Bounds
    private final Map<ModuleCategory, double[]> categoryBounds = new HashMap<>();
    private final Map<Module, double[]> moduleBounds = new HashMap<>();
    private final Map<Property<?>, double[]> propertyBounds = new HashMap<>();
    private final Map<Property<?>, double[]> sliderBounds = new HashMap<>();
    private final Map<EnumProperty<?>, Map<Object, double[]>> enumOptionBounds = new HashMap<>();
    private double[] scrollBarHitbox = null;

    // Layout constants
    private final double padding = 8.0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.windowWidth = this.width / 2.2;
        this.windowHeight = this.height / 1.5;
        if (!hasInitialisedPosition) {
            windowX = (this.width - this.windowWidth) / 2;
            windowY = (this.height - this.windowHeight) / 2;
            hasInitialisedPosition = true;
        }

        clearBounds();

        propertyScrollOffset = RenderUtils.lerp((float)propertyScrollOffset, (float)targetPropertyScrollOffset, 0.15f);

        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 140).getRGB());
        drawRect((float) windowX, (float) windowY, (float) (windowX + windowWidth), (float) (windowY + windowHeight), bgColor.getRGB());

        double innerX = windowX + padding;
        double innerY = windowY + padding;
        double innerWidth = windowWidth - padding * 2;
        double innerHeight = windowHeight - padding * 2;

        double categorySectionHeight = 30;
        Gui.drawRect((int) innerX, (int) innerY, (int) (innerX + innerWidth), (int) (innerY + categorySectionHeight), headerColor.getRGB());
        drawCategoryButtons(mouseX, mouseY, innerX, innerY, categorySectionHeight);

        double contentY = innerY + categorySectionHeight + padding;
        double contentHeight = innerHeight - categorySectionHeight - padding;
        double moduleListWidth = innerWidth / 3;

        Gui.drawRect((int) innerX, (int) contentY, (int) (innerX + moduleListWidth), (int) (contentY + contentHeight), headerColor.getRGB());
        drawModuleList(innerX, contentY, moduleListWidth, mouseX, mouseY);

        if (selectedModule != null) {
            drawSettingsPanel(mouseX, mouseY, innerX + moduleListWidth + padding, contentY, innerWidth - moduleListWidth - padding, contentHeight);
        }

        fontRenderer.drawStringWithShadow(Simp.NAME + " " + Simp.BUILD, (float) (windowX + padding), (float) (windowY + windowHeight - fontRenderer.FONT_HEIGHT - 5), textColor.getRGB());
    }

    private void drawCategoryButtons(int mouseX, int mouseY, double x, double y, double height) {
        double categoryX = x + padding;
        int i = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            String name = category.name();
            double textWidth = fontRenderer.getStringWidth(name);
            categoryBounds.put(category, new double[]{categoryX, y, categoryX + textWidth, y + height});
            boolean isHovered = inBounds(mouseX, mouseY, categoryBounds.get(category));
            int color = (currentCategory == category) ? getRainbowColor(i * 120) : (isHovered ? Color.WHITE.getRGB() : textColor.getRGB());
            fontRenderer.drawStringWithShadow(name, (float) categoryX, (float) (y + (height - fontRenderer.FONT_HEIGHT) / 2), color);
            categoryX += textWidth + padding * 2;
            i++;
        }
    }

    private void drawModuleList(double x, double y, double width, int mouseX, int mouseY) {
        double moduleY = y + padding;
        int i = 0;
        for (Module module : Simp.INSTANCE.getModuleManager().getModulesForCategory(currentCategory)) {
            String name = module.getLabel();
            moduleBounds.put(module, new double[]{x + padding, moduleY, x + width - padding, moduleY + fontRenderer.FONT_HEIGHT});
            boolean isHovered = inBounds(mouseX, mouseY, moduleBounds.get(module));
            int color = module.isEnabled() ? getRainbowColor(i * 80) : (selectedModule == module ? Color.WHITE.getRGB() : textColor.getRGB());
            fontRenderer.drawStringWithShadow(name, (float) (x + padding), (float) moduleY, color);
            moduleY += fontRenderer.FONT_HEIGHT + padding;
            i++;
        }
    }

    private void drawSettingsPanel(int mouseX, int mouseY, double x, double y, double width, double height) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), headerColor.getRGB());
        fontRenderer.drawStringWithShadow(selectedModule.getDescription() + " Settings", (float) (x + padding), (float) (y + padding), textColor.getRGB());
        double propertiesAreaY = y + fontRenderer.FONT_HEIGHT + padding * 2;
        double propertiesAreaHeight = height - (fontRenderer.FONT_HEIGHT + padding * 3);

        RenderUtils.startScissor((float) x, (float) propertiesAreaY, (float) width, (float) propertiesAreaHeight);
        double logicalPropertyY = propertiesAreaY;
        double totalPropertiesHeight = 0;
        int i = 0;
        for (Property<?> property : selectedModule.getElements()) {
            if (!property.isAvailable()) continue;
            double propertyHeight = getPropertyHeight(property);
            propertyBounds.put(property, new double[]{x, logicalPropertyY, x + width, logicalPropertyY + propertyHeight});
            double drawnY = logicalPropertyY - propertyScrollOffset;
            fontRenderer.drawStringWithShadow(property.getLabel(), (float) (x + padding), (float) (drawnY + 4), textColor.getRGB());
            if (property.getType() == Boolean.class) {
                drawBooleanProperty(property, x + width - padding - 15, drawnY + 2, i);
            } else if (property instanceof EnumProperty) {
                drawEnumProperty((EnumProperty<?>) property, x + width - padding - 100, drawnY, 90);
            } else if (property instanceof DoubleProperty) {
                drawDoubleProperty((DoubleProperty) property, x + padding, drawnY + 18, width - padding * 2, i);
            }
            logicalPropertyY += propertyHeight;
            totalPropertiesHeight += propertyHeight;
            i++;
        }
        RenderUtils.endScissor();

        if (openDropdown != null) {
            drawEnumDropdown(mouseX, mouseY);
        }

        maxPropertyScroll = Math.max(0, totalPropertiesHeight - propertiesAreaHeight);
        targetPropertyScrollOffset = Math.max(0, Math.min(targetPropertyScrollOffset, maxPropertyScroll));

        if (maxPropertyScroll > 0) {
            drawScrollBar(x, propertiesAreaY, width, propertiesAreaHeight, totalPropertiesHeight);
        }
    }

    private void drawScrollBar(double x, double propertiesAreaY, double width, double propertiesAreaHeight, double totalPropertiesHeight) {
        double scrollBarWidth = 4;
        double scrollBarX = x + width - scrollBarWidth - 2;
        double visibleRatio = propertiesAreaHeight / totalPropertiesHeight;
        double handleHeight = Math.max(20, propertiesAreaHeight * visibleRatio);
        double handleY = propertiesAreaY + (propertyScrollOffset / maxPropertyScroll) * (propertiesAreaHeight - handleHeight);

        Gui.drawRect((int) scrollBarX, (int) propertiesAreaY, (int) (scrollBarX + scrollBarWidth), (int) (propertiesAreaY + propertiesAreaHeight), componentBgColor.getRGB());
        Gui.drawRect((int) scrollBarX, (int) handleY, (int) (scrollBarX + scrollBarWidth), (int) (handleY + handleHeight), accentColor);

        scrollBarHitbox = new double[]{scrollBarX, propertiesAreaY, scrollBarX + scrollBarWidth, propertiesAreaY + propertiesAreaHeight};
    }

    private double getPropertyHeight(Property<?> property) {
        if (property instanceof DoubleProperty) return 40;
        if (property instanceof EnumProperty && property == openDropdown) {
            EnumProperty<?> enumProp = (EnumProperty<?>) property;
            return 25 + (enumProp.getValues().length * (fontRenderer.FONT_HEIGHT + 8)) + 5;
        }
        return 25;
    }

    private void drawBooleanProperty(Property property, double x, double y, int rainbowIndex) {
        Gui.drawRect((int) x, (int) y, (int) (x + 12), (int) (y + 12), outlineColor.getRGB());
        Gui.drawRect((int) x + 1, (int) y + 1, (int) (x + 11), (int) (y + 11), componentBgColor.getRGB());
        if ((boolean) property.getValue()) {
            Gui.drawRect((int) x + 3, (int) y + 3, (int) (x + 9), (int) (y + 9), accentColor);
        }
    }

    private void drawEnumProperty(EnumProperty<?> property, double x, double y, double width) {
        double height = fontRenderer.FONT_HEIGHT + 8;
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), componentBgColor.getRGB());
        fontRenderer.drawStringWithShadow(property.getValue().toString(), (float) (x + 5), (float) (y + 4), textColor.getRGB());
        RenderUtils.drawArrow((float) (x + width - 12), (float) (y + height / 2 - 2), 4, openDropdown == property ? RenderUtils.ArrowDirection.UP : RenderUtils.ArrowDirection.DOWN, textColor.getRGB());
    }

    private void drawEnumDropdown(int mouseX, int mouseY) {
        double[] mainBounds = propertyBounds.get(openDropdown);
        if (mainBounds == null) return;
        double x = mainBounds[2] - padding - 100;
        double y = (mainBounds[1] - propertyScrollOffset) + 25;
        double width = 90;
        enumOptionBounds.putIfAbsent(openDropdown, new HashMap<>());
        Map<Object, double[]> optionBounds = enumOptionBounds.get(openDropdown);
        optionBounds.clear();
        double optionY = y;
        for (Object enumValue : openDropdown.getValues()) {
            double optionHeight = fontRenderer.FONT_HEIGHT + 8;
            optionBounds.put(enumValue, new double[]{x, optionY, x + width, optionY + optionHeight});
            boolean isHovered = inBounds(mouseX, mouseY, optionBounds.get(enumValue));
            Gui.drawRect((int)x, (int)optionY, (int)(x+width), (int)(optionY + optionHeight), isHovered ? dropdownHoverColor.getRGB() : componentBgColor.getRGB());
            fontRenderer.drawStringWithShadow(enumValue.toString(), (float) (x + 5), (float) (optionY + 4), textColor.getRGB());
            optionY += optionHeight;
        }
    }

    private void drawDoubleProperty(DoubleProperty property, double x, double y, double width, int rainbowIndex) {
        double min = property.getMin();
        double max = property.getMax();
        double value = property.getValue();
        double percentage = (value - min) / (max - min);

        // Adjust y position for scrolling
        double sliderY = y;

        Gui.drawRect((int) x, (int) sliderY, (int) (x + width), (int) (sliderY + 6), componentBgColor.getRGB());
        Gui.drawRect((int) x, (int) sliderY, (int) (x + (width * percentage)), (int) (sliderY + 6), accentColor);
        RenderUtils.drawCircle(x + (width * percentage), sliderY + 3, 5, accentColor);
        RenderUtils.drawCircle(x + (width * percentage), sliderY + 3, 4, bgColor.getRGB());

        String valueText = String.format("%.2f", value);
        fontRenderer.drawStringWithShadow(valueText, (float) (x + width - fontRenderer.getStringWidth(valueText)), (float) (sliderY - 12), textColor.getRGB());

        sliderBounds.put(property, new double[]{x, sliderY, x + width, sliderY + 6});
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        AtomicBoolean consumedClick = new AtomicBoolean(false);

        double dragHandleHeight = padding + 30;
        if (inBounds(mouseX, mouseY, new double[]{windowX, windowY, windowX + windowWidth, windowY + dragHandleHeight}) && openDropdown == null) {
            isDraggingWindow = true;
            dragStartX = mouseX - windowX;
            dragStartY = mouseY - windowY;
        }

        if (openDropdown != null) {
            if (enumOptionBounds.containsKey(openDropdown)) {
                for (Map.Entry<Object, double[]> optionEntry : enumOptionBounds.get(openDropdown).entrySet()) {
                    if (inBounds(mouseX, mouseY, optionEntry.getValue())) {
                        openDropdown.setValueObj(optionEntry.getKey());
                        openDropdown = null;
                        consumedClick.set(true);
                        break;
                    }
                }
            }
        }
        if (consumedClick.get()) return;

        for (Map.Entry<ModuleCategory, double[]> entry : categoryBounds.entrySet()) {
            if (inBounds(mouseX, mouseY, entry.getValue())) {
                currentCategory = entry.getKey();
                if(selectedModule != null) openDropdown = null;
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
            if (scrollBarHitbox != null && inBounds(mouseX, mouseY, scrollBarHitbox)) {
                isScrolling = true;
                scrollStartY = mouseY;
                scrollStartOffset = targetPropertyScrollOffset;
                consumedClick.set(true);
            }
            if (consumedClick.get()) return;

            for (Map.Entry<Property<?>, double[]> entry : sliderBounds.entrySet()) {
                DoubleProperty doubleProperty = (DoubleProperty) entry.getKey();
                double[] bounds = entry.getValue();
                // Adjust the y coordinate of the slider bounds by adding propertyScrollOffset
                double sliderY = bounds[1];
                if (inBounds(mouseX, mouseY, new double[]{bounds[0], sliderY - 2, bounds[2], sliderY + 8})) {
                    draggingSlider = true;
                    draggedProperty = entry.getKey();
                    updateSliderValue((DoubleProperty) draggedProperty, mouseX);
                    consumedClick.set(true);
                    break;
                }
            }
            if (consumedClick.get()) return;

            double propertiesAreaY = windowY + padding + 30 + padding;
            double propertiesAreaHeight = windowHeight - (padding * 3) - 30;
            if (inBounds(mouseX, mouseY, new double[]{windowX, propertiesAreaY, windowX + windowWidth, propertiesAreaY + propertiesAreaHeight})) {
                for (Map.Entry<Property<?>, double[]> entry : propertyBounds.entrySet()) {
                    Property<?> property = entry.getKey();
                    double[] bounds = entry.getValue();
                    double visibleY = bounds[1] - propertyScrollOffset;
                    double componentHeight = 25;
                    if (mouseY >= visibleY && mouseY <= visibleY + componentHeight) {
                        handlePropertyClick(property);
                        consumedClick.set(true);
                        break;
                    }
                }
            }
        }

        if (!consumedClick.get() && openDropdown != null) {
            openDropdown = null;
        }

        if (!consumedClick.get() && !isDraggingWindow) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void handlePropertyClick(Property<?> property) {
        if (property.getType() == Boolean.class) {
            property.setValueObj(!(boolean) property.getValueObj());
        } else if (property instanceof EnumProperty) {
            openDropdown = (openDropdown == property) ? null : (EnumProperty<?>) property;
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
        if(isDraggingWindow) {
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
            updateSliderValue((DoubleProperty) draggedProperty, mouseX);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && selectedModule != null) {
            targetPropertyScrollOffset = Math.max(0, Math.min(maxPropertyScroll, targetPropertyScrollOffset - wheel / 4.0f));
        }
    }

    private void updateSliderValue(DoubleProperty property, int mouseX) {
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
        if(openDropdown == null || !enumOptionBounds.containsKey(openDropdown)) {
            enumOptionBounds.values().forEach(Map::clear);
        }
        scrollBarHitbox = null;
    }

    private boolean inBounds(int mouseX, int mouseY, double[] bounds) {
        if (bounds == null) return false;
        return mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3];
    }

    // Gamesense-style rainbow accent
    private int getRainbowColor(int offset) {
        float speed = 2000f;
        float hue = ((System.currentTimeMillis() + offset) % (int)speed) / speed;
        return Color.HSBtoRGB(hue, 0.7f, 1.0f);
    }
}