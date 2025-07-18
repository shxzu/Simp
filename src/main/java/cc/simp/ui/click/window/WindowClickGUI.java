package cc.simp.ui.click.window;

import cc.simp.Simp;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.Logger;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowClickGUI extends GuiScreen {

    private final MinecraftFontRenderer minecraftFontRenderer = Minecraft.getMinecraft().minecraftFontRendererObj;
    private ModuleCategory currentCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;

    // UI States
    private EnumProperty<?> openDropdown = null;
    private boolean draggingSlider = false;
    private Property<?> draggedProperty = null;
    private boolean isScrolling = false;
    private Module listeningModule = null;

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

    // Custom Color Palette (Based on the provided image aesthetic)
    float hue = (System.currentTimeMillis() % 3000) / 3000f;
    private final Color primaryBgColor = new Color(30, 30, 30, 230); // Main window background, slightly transparent
    private final Color secondaryBgColor = new Color(40, 40, 40, 230); // Section backgrounds
    private final Color hoverColor = new Color(60, 60, 60, 230); // Hover state for clickable elements
    private final Color textColor = new Color(220, 220, 220); // General text color
    private final Color mutedTextColor = new Color(150, 150, 150); // Muted text for keybinds
    private final Color borderColor = new Color(50, 50, 50, 230); // Subtle borders

    // UI Element Bounds
    private final Map<ModuleCategory, double[]> categoryBounds = new HashMap<>();
    private final Map<Module, double[]> moduleBounds = new HashMap<>();
    private final Map<Property<?>, double[]> propertyBounds = new HashMap<>();
    private final Map<Property<?>, double[]> sliderBounds = new HashMap<>();
    private final Map<EnumProperty<?>, Map<Object, double[]>> enumOptionBounds = new HashMap<>();
    private double[] scrollBarHitbox = null;

    // Layout constants
    private final double padding = 8.0;
    private final double borderRadius = 5.0; // For rounded corners

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

        // Draw dim background
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 140).getRGB());

        // Draw main window with rounded corners
        drawRoundedRect((float) windowX, (float) windowY, (float) (windowX + windowWidth), (float) (windowY + windowHeight), (float) borderRadius, primaryBgColor.getRGB());
        drawRoundedRectOutline((float) windowX, (float) windowY, (float) (windowX + windowWidth), (float) (windowY + windowHeight), (float) borderRadius, 1.0f, borderColor.getRGB());


        double innerX = windowX + padding;
        double innerY = windowY + padding;
        double innerWidth = windowWidth - padding * 2;
        double innerHeight = windowHeight - padding * 2;

        // Category section
        double categorySectionHeight = 30;
        drawRoundedRect((float) innerX, (float) innerY, (float) (innerX + innerWidth), (float) (innerY + categorySectionHeight), (float) (borderRadius / 2), secondaryBgColor.getRGB());
        drawCategoryButtons(mouseX, mouseY, innerX, innerY, categorySectionHeight);

        double contentY = innerY + categorySectionHeight + padding;
        double contentHeight = innerHeight - categorySectionHeight - padding;
        double moduleListWidth = innerWidth / 3;

        // Module list background
        drawRoundedRect((float) innerX, (float) contentY, (float) (innerX + moduleListWidth), (float) (contentY + contentHeight), (float) (borderRadius / 2), secondaryBgColor.getRGB());
        drawModuleList(innerX, contentY, moduleListWidth, mouseX, mouseY);

        if (selectedModule != null) {
            drawSettingsPanel(mouseX, mouseY, innerX + moduleListWidth + padding, contentY, innerWidth - moduleListWidth - padding, contentHeight);
        }

        // Draw SIMP version text
        minecraftFontRenderer.drawStringWithShadow(Simp.NAME + " " + Simp.BUILD, (float) (windowX + padding), (float) (windowY + windowHeight - minecraftFontRenderer.FONT_HEIGHT - 5), mutedTextColor.getRGB());
    }

    private void drawCategoryButtons(int mouseX, int mouseY, double x, double y, double height) {
        double categoryX = x + padding;
        for (ModuleCategory category : ModuleCategory.values()) {
            String name = category.name();
            double textWidth = minecraftFontRenderer.getStringWidth(name);
            double btnWidth = textWidth + padding * 2;
            double btnHeight = height - padding;
            double btnY = y + (height - btnHeight) / 2;

            categoryBounds.put(category, new double[]{categoryX, btnY, categoryX + btnWidth, btnY + btnHeight});
            boolean isHovered = inBounds(mouseX, mouseY, categoryBounds.get(category));

            int bgColor = currentCategory == category ? Color.getHSBColor(hue, 0.55f, 0.9f).getRGB() : (isHovered ? hoverColor.getRGB() : secondaryBgColor.getRGB());
            drawRoundedRect((float) categoryX, (float) btnY, (float) (categoryX + btnWidth), (float) (btnY + btnHeight), (float) (borderRadius / 2), bgColor);

            int color = textColor.getRGB();
            minecraftFontRenderer.drawStringWithShadow(name, (float) (categoryX + padding), (float) (btnY + (btnHeight - minecraftFontRenderer.FONT_HEIGHT) / 2), color);
            categoryX += btnWidth + padding;
        }
    }

    private void drawModuleList(double x, double y, double width, int mouseX, int mouseY) {
        double moduleY = y + padding;
        for (Module module : Simp.INSTANCE.getModuleManager().getModulesForCategory(currentCategory)) {
            String name = (module == listeningModule) ? "Listening..." : module.getLabel();
            double moduleHeight = minecraftFontRenderer.FONT_HEIGHT + padding;

            moduleBounds.put(module, new double[]{x + padding, moduleY, x + width - padding, moduleY + moduleHeight});
            boolean isHovered = inBounds(mouseX, mouseY, moduleBounds.get(module));

            int bgColor = secondaryBgColor.getRGB();
            if (module.isEnabled()) {
                bgColor = Color.getHSBColor(hue, 0.55f, 0.9f).getRGB(); // Active module background
            } else if (selectedModule == module) {
                bgColor = hoverColor.getRGB(); // Selected but not active module
            } else if (isHovered) {
                bgColor = hoverColor.getRGB(); // Hovered module
            }

            drawRoundedRect((float) (x + padding), (float) moduleY, (float) (x + width - padding), (float) (moduleY + moduleHeight), (float) (borderRadius / 2), bgColor);

            int color = textColor.getRGB();
            minecraftFontRenderer.drawStringWithShadow(name, (float) (x + padding * 2), (float) (moduleY + padding / 2), color);

            // Draw key bind
            if (module != listeningModule && module.getKey() != 0) {
                String keyName = Keyboard.getKeyName(module.getKey());
                float keyX = (float) (x + width - padding * 2 - minecraftFontRenderer.getStringWidth("§7[" + keyName + "]"));
                minecraftFontRenderer.drawStringWithShadow("§7[" + keyName + "]", keyX, (float) (moduleY + padding / 2), mutedTextColor.getRGB());
            }

            moduleY += moduleHeight + padding;
        }
    }

    private void drawSettingsPanel(int mouseX, int mouseY, double x, double y, double width, double height) {
        drawRoundedRect((float) x, (float) y, (float) (x + width), (float) (y + height), (float) (borderRadius / 2), secondaryBgColor.getRGB());
        minecraftFontRenderer.drawStringWithShadow(selectedModule.getDescription() + " Settings", (float) (x + padding), (float) (y + padding), textColor.getRGB());

        double propertiesAreaY = y + minecraftFontRenderer.FONT_HEIGHT + padding * 2;
        double propertiesAreaHeight = height - (minecraftFontRenderer.FONT_HEIGHT + padding * 3);

        RenderUtils.startScissor((float) x, (float) propertiesAreaY, (float) width, (float) propertiesAreaHeight);
        double logicalPropertyY = propertiesAreaY;
        double totalPropertiesHeight = 0;
        for (Property<?> property : selectedModule.getElements()) {
            if (!property.isAvailable()) continue;
            double propertyHeight = getPropertyHeight(property);
            propertyBounds.put(property, new double[]{x, logicalPropertyY, x + width, logicalPropertyY + propertyHeight});
            double drawnY = logicalPropertyY - propertyScrollOffset;

            // Draw property background
            drawRoundedRect((float) (x + padding), (float) (drawnY + 2), (float) (x + width - padding), (float) (drawnY + propertyHeight - 2), (float) (borderRadius / 2), primaryBgColor.getRGB());

            minecraftFontRenderer.drawStringWithShadow(property.getLabel(), (float) (x + padding * 2), (float) (drawnY + 4), textColor.getRGB());
            if (property.getType() == Boolean.class) {
                drawBooleanProperty(property, x + width - padding - 20, drawnY + 4);
            } else if (property instanceof EnumProperty) {
                drawEnumProperty((EnumProperty<?>) property, x + width - padding - 110, drawnY + 2, 100);
            } else if (property instanceof DoubleProperty) {
                drawDoubleProperty((DoubleProperty) property, x + padding * 2, drawnY + 18, width - padding * 4);
            }
            logicalPropertyY += propertyHeight;
            totalPropertiesHeight += propertyHeight;
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
        double scrollBarWidth = 6;
        double scrollBarX = x + width - scrollBarWidth - 2;
        double visibleRatio = propertiesAreaHeight / totalPropertiesHeight;
        double handleHeight = Math.max(20, propertiesAreaHeight * visibleRatio);
        double handleY = propertiesAreaY + (propertyScrollOffset / maxPropertyScroll) * (propertiesAreaHeight - handleHeight);

        // Scrollbar track
        drawRoundedRect((float) scrollBarX, (float) propertiesAreaY, (float) (scrollBarX + scrollBarWidth), (float) (propertiesAreaY + propertiesAreaHeight), (float) (scrollBarWidth / 2), primaryBgColor.getRGB());
        // Scrollbar handle
        drawRoundedRect((float) scrollBarX, (float) handleY, (float) (scrollBarX + scrollBarWidth), (float) (handleY + handleHeight), (float) (scrollBarWidth / 2), Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());

        scrollBarHitbox = new double[]{scrollBarX, propertiesAreaY, scrollBarX + scrollBarWidth, propertiesAreaY + propertiesAreaHeight};
    }

    private double getPropertyHeight(Property<?> property) {
        if (property instanceof DoubleProperty) return 40;
        if (property instanceof EnumProperty && property == openDropdown) {
            EnumProperty<?> enumProp = (EnumProperty<?>) property;
            return 25 + (enumProp.getValues().length * (minecraftFontRenderer.FONT_HEIGHT + 8)) + 5;
        }
        return 25;
    }

    private void drawBooleanProperty(Property property, double x, double y) {
        double boxSize = 14;
        drawRoundedRect((float) x, (float) y, (float) (x + boxSize), (float) (y + boxSize), (float) (borderRadius / 2), borderColor.getRGB());
        if ((boolean) property.getValue()) {
            drawRoundedRect((float) (x + 2), (float) (y + 2), (float) (x + boxSize - 2), (float) (y + boxSize - 2), (float) (borderRadius / 3), Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        } else {
            drawRoundedRect((float) (x + 2), (float) (y + 2), (float) (x + boxSize - 2), (float) (y + boxSize - 2), (float) (borderRadius / 3), hoverColor.darker().getRGB());
        }
    }

    private void drawEnumProperty(EnumProperty<?> property, double x, double y, double width) {
        double height = minecraftFontRenderer.FONT_HEIGHT + 8;
        drawRoundedRect((float) x, (float) y, (float) (x + width), (float) (y + height), (float) (borderRadius / 2), hoverColor.getRGB());
        minecraftFontRenderer.drawStringWithShadow(property.getValue().toString(), (float) (x + 5), (float) (y + 4), textColor.getRGB());
        RenderUtils.drawArrow((float) (x + width - 12), (float) (y + height / 2 - 2), 4, openDropdown == property ? RenderUtils.ArrowDirection.UP : RenderUtils.ArrowDirection.DOWN, textColor.getRGB());
    }

    private void drawEnumDropdown(int mouseX, int mouseY) {
        double[] mainBounds = propertyBounds.get(openDropdown);
        if (mainBounds == null) return;
        double x = mainBounds[2] - padding - 110;
        double y = (mainBounds[1] - propertyScrollOffset) + 25;
        double width = 100;
        enumOptionBounds.putIfAbsent(openDropdown, new HashMap<>());
        Map<Object, double[]> optionBounds = enumOptionBounds.get(openDropdown);
        optionBounds.clear();
        double optionY = y;
        for (Object enumValue : openDropdown.getValues()) {
            double optionHeight = minecraftFontRenderer.FONT_HEIGHT + 8;
            optionBounds.put(enumValue, new double[]{x, optionY, x + width, optionY + optionHeight});
            boolean isHovered = inBounds(mouseX, mouseY, optionBounds.get(enumValue));
            drawRoundedRect((float)x, (float)optionY, (float)(x+width), (float)(optionY + optionHeight), (float) (borderRadius / 3), isHovered ? Color.getHSBColor(hue, 0.55f, 0.9f).darker().getRGB() : secondaryBgColor.getRGB());
            minecraftFontRenderer.drawStringWithShadow(enumValue.toString(), (float) (x + 5), (float) (optionY + 4), textColor.getRGB());
            optionY += optionHeight;
        }
    }

    private void drawDoubleProperty(DoubleProperty property, double x, double y, double width) {
        double min = property.getMin();
        double max = property.getMax();
        double value = property.getValue();
        double percentage = (value - min) / (max - min);

        double sliderY = y;
        double sliderHeight = 6;

        // Slider track
        drawRoundedRect((float) x, (float) sliderY, (float) (x + width), (float) (sliderY + sliderHeight), (float) (sliderHeight / 2), primaryBgColor.darker().getRGB());
        // Slider fill
        drawRoundedRect((float) x, (float) sliderY, (float) (x + (width * percentage)), (float) (sliderY + sliderHeight), (float) (sliderHeight / 2), Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());

        // Slider thumb (circle)
        drawCircle((float) (x + (width * percentage)), (float) (sliderY + sliderHeight / 2), (float) (sliderHeight * 0.8), Color.getHSBColor(hue, 0.55f, 0.9f).getRGB());
        drawCircle((float) (x + (width * percentage)), (float) (sliderY + sliderHeight / 2), (float) (sliderHeight * 0.5), primaryBgColor.getRGB());


        String valueText = String.format("%.2f", value);
        minecraftFontRenderer.drawStringWithShadow(valueText, (float) (x + width - minecraftFontRenderer.getStringWidth(valueText)), (float) (sliderY - 12), textColor.getRGB());

        sliderBounds.put(property, new double[]{x, sliderY, x + width, sliderY + sliderHeight});
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        AtomicBoolean consumedClick = new AtomicBoolean(false);

        // Handle key binding when middle clicking a module
        if (mouseButton == 2) { // Middle click
            for (Map.Entry<Module, double[]> entry : moduleBounds.entrySet()) {
                if (inBounds(mouseX, mouseY, entry.getValue())) {
                    listeningModule = entry.getKey();
                    consumedClick.set(true);
                    break;
                }
            }
            if (consumedClick.get()) return;
        }

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
                    double componentHeight = getPropertyHeight(property); // Use actual property height
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

    /**
     * Placeholder for drawing a rounded rectangle.
     * You will need to implement the actual OpenGL rendering for this.
     * This typically involves GL11.glBegin(GL11.GL_POLYGON) and calculating vertices.
     */
    private void drawRoundedRect(float x, float y, float x2, float y2, float radius, int color) {
        // TODO: Implement actual OpenGL rendering for rounded rectangles
        // For now, this will draw a regular rectangle.
        Gui.drawRect((int)x, (int)y, (int)x2, (int)y2, color);
    }

    /**
     * Placeholder for drawing a rounded rectangle outline.
     * You will need to implement the actual OpenGL rendering for this.
     */
    private void drawRoundedRectOutline(float x, float y, float x2, float y2, float radius, float lineWidth, int color) {
        // TODO: Implement actual OpenGL rendering for rounded rectangle outlines
        // For now, this will draw a regular rectangle outline.
        // This is a simplified representation. Actual implementation would involve GL11.glLineWidth, etc.
        Gui.drawRect((int)x, (int)y, (int)x2, (int)(y + lineWidth), color); // Top
        Gui.drawRect((int)x, (int)(y2 - lineWidth), (int)x2, (int)y2, color); // Bottom
        Gui.drawRect((int)x, (int)y, (int)(x + lineWidth), (int)y2, color); // Left
        Gui.drawRect((int)(x2 - lineWidth), (int)y, (int)x2, (int)y2, color); // Right
    }

    /**
     * Placeholder for drawing a circle.
     * You will need to implement the actual OpenGL rendering for this.
     */
    private void drawCircle(float x, float y, float radius, int color) {
        // TODO: Implement actual OpenGL rendering for circles
        // For now, this is just a placeholder.
        RenderUtils.drawCircle(x, y, radius, color); // Assuming RenderUtils has a working circle drawing method
    }
}