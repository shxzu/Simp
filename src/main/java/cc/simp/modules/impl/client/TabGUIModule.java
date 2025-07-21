package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.KeyPressEvent;
import cc.simp.event.impl.render.Render2DEvent;
import cc.simp.font.FontManager;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;
import static net.minecraft.client.gui.Gui.drawRect;

@ModuleInfo(label = "Tab GUI", category = ModuleCategory.CLIENT)
public final class TabGUIModule extends Module {

    private final Property<Boolean> rainbowProperty = new Property<>("Rainbow", true);
    private final EnumProperty<Position> position = new EnumProperty<>("Position", Position.LEFT);

    private boolean inModules = false;
    private boolean inSettings = false;
    private int currentCategory = 0;
    private int moduleIndex = 0;
    private int settingIndex = 0;
    private float targetY = 12;
    private float currentY = 12;
    private float hue = 0.0f;

    public enum Position {
        LEFT, RIGHT
    }

    public TabGUIModule() {
        toggle();
    }

    @Override
    public void onEnable() {
        targetY = 12;
        currentCategory = 0;
        moduleIndex = 0;
        settingIndex = 0;
        inModules = false;
        inSettings = false;
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        if (mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = new ScaledResolution(mc);
        MinecraftFontRenderer fr = mc.minecraftFontRendererObj;

        // Update rainbow - use the same calculation as in ArraylistModule
        float hue = (System.currentTimeMillis() % 3000) / 3000f;

        // Smooth animation
        currentY += (targetY - currentY) * 0.2f;

        int startX = position.getValue() == Position.LEFT ? 2 : sr.getScaledWidth() - 67;
        int startY = 12;
        int width = 65;
        int height = ModuleCategory.values().length * 12 + 2;

        // Background
        drawRect(startX, startY, startX + width, startY + height,
                new Color(0, 0, 0, 160).getRGB());

        // Category rendering
        int y = startY + 2;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = currentCategory == category.ordinal();
            Color color;

            if (rainbowProperty.getValue()) {
                color = Color.getHSBColor(hue, 0.55f, 0.9f);
            } else {
                color = selected ? Color.WHITE : Color.GRAY;
            }

            if (selected) {
                drawRect(startX, y - 2, startX + width, y + 10,
                        new Color(255, 255, 255, 40).getRGB());
            }

            if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                FontManager.getCurrentFont().drawStringWithShadow(category.name(), startX + 4, y, color.getRGB());
            } else {
                fr.drawStringWithShadow(category.name(), startX + 4, y, color.getRGB());
            }
            y += 12;
            hue += 0.035f;
            if (hue > 1.0f) hue = 0.0f;
        }

        // Module rendering
        if (inModules) {
            List<Module> modules = getModulesInCategory(ModuleCategory.values()[currentCategory]);
            int moduleStartX = position.getValue() == Position.LEFT ?
                    startX + width + 2 : startX - 72;
            renderModuleList(fr, moduleStartX, startY, modules);
        }

        // Settings rendering
        if (inSettings) {
            List<Module> modules = getModulesInCategory(ModuleCategory.values()[currentCategory]);
            Module selectedModule = modules.get(moduleIndex);
            int settingsStartX = position.getValue() == Position.LEFT ?
                    startX + width + 74 : startX - 144;
            renderSettings(fr, settingsStartX, startY, selectedModule);
        }
    };

    @EventLink
    public final Listener<KeyPressEvent> keyPressEventListener = event -> {
        int key = event.getKey();

        if (!inModules) {
            handleCategoryNavigation(key);
        } else if (!inSettings) {
            handleModuleNavigation(key);
        } else {
            handleSettingsNavigation(key);
        }
    };

    private void handleCategoryNavigation(int key) {
        if (key == Keyboard.KEY_UP) {
            if (currentCategory > 0) {
                currentCategory--;
                targetY -= 12;
            }
        } else if (key == Keyboard.KEY_DOWN) {
            if (currentCategory < ModuleCategory.values().length - 1) {
                currentCategory++;
                targetY += 12;
            }
        } else if (key == Keyboard.KEY_RIGHT) {
            inModules = true;
            moduleIndex = 0;
        }
    }

    private void handleModuleNavigation(int key) {
        List<Module> modules = getModulesInCategory(ModuleCategory.values()[currentCategory]);

        if (key == Keyboard.KEY_UP && moduleIndex > 0) {
            moduleIndex--;
        } else if (key == Keyboard.KEY_DOWN && moduleIndex < modules.size() - 1) {
            moduleIndex++;
        } else if (key == Keyboard.KEY_LEFT) {
            inModules = false;
        } else if (key == Keyboard.KEY_RIGHT && !modules.get(moduleIndex).getElements().isEmpty()) {
            inSettings = true;
            settingIndex = 0;
        } else if (key == Keyboard.KEY_RETURN) {
            modules.get(moduleIndex).toggle();
        }
    }

    private void handleSettingsNavigation(int key) {
        Module module = getModulesInCategory(ModuleCategory.values()[currentCategory]).get(moduleIndex);
        List<Property<?>> properties = module.getElements();

        if (key == Keyboard.KEY_UP && settingIndex > 0) {
            settingIndex--;
        } else if (key == Keyboard.KEY_DOWN && settingIndex < properties.size() - 1) {
            settingIndex++;
        } else if (key == Keyboard.KEY_LEFT) {
            inSettings = false;
        } else if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_RETURN) {
            updateProperty(properties.get(settingIndex));
        }
    }

    private void updateProperty(Property<?> property) {
        if (property.getType() == Boolean.class) {
            property.setValueObj(!(boolean) property.getValueObj());
        } else if (property instanceof EnumProperty<?>) {
            EnumProperty<?> enumProp = (EnumProperty<?>) property;
            Enum<?>[] values = enumProp.getValues();
            int currentIndex = -1;

            // Find current value index
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(enumProp.getValue())) {
                    currentIndex = i;
                    break;
                }
            }

            // Set next value
            if (currentIndex != -1) {
                int nextIndex = (currentIndex + 1) % values.length;
                enumProp.setValue(nextIndex);
            }
        } else if (property instanceof DoubleProperty) {
            DoubleProperty prop = (DoubleProperty) property;
            double newValue = prop.getValue() + prop.getIncrement();
            if (newValue <= prop.getMax())
                prop.setValue(newValue);
        }
    }

    private void renderModuleList(MinecraftFontRenderer fr, int x, int y, List<Module> modules) {
        int width = 70;
        int height = modules.size() * 12 + 2;
        float hue = (System.currentTimeMillis() % 3000) / 3000f;

        // Background
        drawRect(x, y, x + width, y + height, new Color(0, 0, 0, 160).getRGB());

        // Render modules
        int currentY = y + 2;
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean selected = i == moduleIndex;

            // Selection highlight
            if (selected) {
                drawRect(x, currentY - 2, x + width, currentY + 10,
                        new Color(255, 255, 255, 40).getRGB());
            }

            // Module text
            String text = module.getLabel() + (module.getSuffix() != null ? " §7" + module.getSuffix() : "");
            Color rainbow = Color.getHSBColor(hue, 0.55f, 0.9f);
            if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                FontManager.getCurrentFont().drawStringWithShadow(text, x + 4, currentY,
                        rainbowProperty.getValue() ? rainbow.getRGB() :
                                module.isEnabled() ? -1 : Color.GRAY.getRGB());
            } else {
                fr.drawStringWithShadow(text, x + 4, currentY,
                        rainbowProperty.getValue() ? rainbow.getRGB() :
                                module.isEnabled() ? -1 : Color.GRAY.getRGB());
            }

            // Settings indicator
            if (!module.getElements().isEmpty()) {
                if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                    FontManager.getCurrentFont().drawStringWithShadow("...", x + width - 10, currentY, Color.GRAY.getRGB());
                } else {
                    fr.drawStringWithShadow("...", x + width - 10, currentY, Color.GRAY.getRGB());
                }
            }

            currentY += 12;
            hue += 0.035f;
            if (hue > 1.0f) hue = 0.0f;
        }
    }

    private void renderSettings(MinecraftFontRenderer fr, int x, int y, Module module) {
        List<Property<?>> properties = module.getElements();
        int width = 90;
        int height = properties.size() * 12 + 2;
        float hue = (System.currentTimeMillis() % 3000) / 3000f;

        // Background
        drawRect(x, y, x + width, y + height, new Color(0, 0, 0, 160).getRGB());

        // Render settings
        int currentY = y + 2;
        for (int i = 0; i < properties.size(); i++) {
            Property<?> property = properties.get(i);
            boolean selected = i == settingIndex;

            // Selection highlight
            if (selected) {
                drawRect(x, currentY - 2, x + width, currentY + 10,
                        new Color(255, 255, 255, 40).getRGB());
            }

            // Setting name
            String name = property.getLabel();
            Color rainbow = Color.getHSBColor(hue, 0.55f, 0.9f);

            // Setting value
            String value = getPropertyValue(property);
            String text = name + ": §7" + value;

            if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                FontManager.getCurrentFont().drawStringWithShadow(text, x + 4, currentY,
                        rainbowProperty.getValue() ? rainbow.getRGB() : -1);
            } else {
                fr.drawStringWithShadow(text, x + 4, currentY,
                        rainbowProperty.getValue() ? rainbow.getRGB() : -1);
            }

            currentY += 12;
            hue += 0.035f;
            if (hue > 1.0f) hue = 0.0f;
        }
    }

    private String getPropertyValue(Property<?> property) {
        if (property.getType() == Boolean.class) {
            return (boolean) property.getValueObj() ? "On" : "Off";
        } else if (property instanceof EnumProperty) {
            return String.valueOf(((EnumProperty<?>) property).getValue());
        } else if (property instanceof DoubleProperty) {
            return String.format("%.2f", ((DoubleProperty) property).getValue());
        }
        return String.valueOf(property.getValue());
    }

    private List<Module> getModulesInCategory(ModuleCategory category) {
        return Simp.INSTANCE.getModuleManager().getModules().stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }
}