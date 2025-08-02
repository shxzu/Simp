package cc.simp.modules.impl.client;

import cc.simp.Simp;
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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Array List", category = ModuleCategory.CLIENT)
public final class ArraylistModule extends Module {

    private final Property<Boolean> leftProperty = new Property<>("Align Left", false);
    private final Property<Boolean> removeSpacesProperty = new Property<>("Remove Spaces", false);
    private final Property<Boolean> importantProperty = new Property<>("Only Important", false);
    public static EnumProperty<RectangleMode> rectangleProperty = new EnumProperty<>("Rectangle", RectangleMode.NONE);
    public DoubleProperty offsetProperty = new DoubleProperty("Offset", 0, () -> !leftProperty.getValue(), 0, 5, 1);

    public enum RectangleMode {
        NONE,
        BASIC
    }

    public ArraylistModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        if (mc.gameSettings.showDebugInfo) return;

        MinecraftFontRenderer minecraftFontRenderer = mc.minecraftFontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        float hue = (System.currentTimeMillis() % 3000) / 3000f;
        boolean left = leftProperty.getValue(); // Can be made configurable
        int y = left ? 12 : offsetProperty.getValue().intValue() + 1;
        if(Simp.INSTANCE.getModuleManager().getModule(TabGUIModule.class).isEnabled() && left) y = ModuleCategory.values().length * 12 + 20;

        List<Module> modules = new CopyOnWriteArrayList<>();

        // Collect enabled modules
        for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
            if (module.isEnabled()) {
                if (!importantProperty.getValue() || (module.getCategory() != ModuleCategory.CLIENT && module.getCategory() != ModuleCategory.RENDER))
                    modules.add(module);
            }
        }

        // Sort modules by width
        if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
            modules.sort(Comparator.comparingDouble(m ->
                    -FontManager.getCurrentFont().getStringWidth(m.getUpdatedSuffix() != null ?
                            m.getLabel() + " " + m.getUpdatedSuffix() :
                            m.getLabel())
            ));
        } else {
            modules.sort(Comparator.comparingDouble(m ->
                    -minecraftFontRenderer.getStringWidth(m.getUpdatedSuffix() != null ?
                            m.getLabel() + " " + m.getUpdatedSuffix() :
                            m.getLabel())
            ));
        }

        // Render modules
        for (Module module : modules) {

            if (hue > 1.0f) hue = 0.0f;

            StringBuilder moduleNameBuilder = new StringBuilder(module.getLabel());
            if(removeSpacesProperty.getValue() && module.getLabel().contains(" ")) {
                if(moduleNameBuilder.toString().contains(" ")) {
                    moduleNameBuilder.deleteCharAt(moduleNameBuilder.indexOf(" "));
                }
            }
            String text = moduleNameBuilder.toString() + (module.getUpdatedSuffix() != null ? " §7" + module.getUpdatedSuffix() : "");
            float x = left ? 2.0f : sr.getScaledWidth() - minecraftFontRenderer.getStringWidth(text) - offsetProperty.getValue().intValue() - 1.0f;
            if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC)
                x = left ? 2.0f : sr.getScaledWidth() - FontManager.getCurrentFont().getStringWidth(text) - offsetProperty.getValue().intValue() - 1.0f;

            Color rainbow = Color.getHSBColor(hue, 0.55f, 0.9f);

            switch (rectangleProperty.getValue()) {
                case NONE:
                    break;
                case BASIC:
                    if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                        Gui.drawRect2(x - 2, y - 1, FontManager.getCurrentFont().getStringWidth(text) + 4,
                                FontManager.getCurrentFont().FONT_HEIGHT + 1,
                                new Color(0, 0, 0, 130).getRGB());
                    } else {
                        Gui.drawRect2(x - 2, y - 1, minecraftFontRenderer.getStringWidth(text) + 4,
                                minecraftFontRenderer.FONT_HEIGHT + 1,
                                new Color(0, 0, 0, 130).getRGB());
                    }
            }

            if (FontManagerModule.fontTypeProperty.getValue() != FontManagerModule.FontType.MC) {
                FontManager.getCurrentFont().drawStringWithShadow(text, x, y, rainbow.getRGB());
            } else {
                minecraftFontRenderer.drawStringWithShadow(text, x, y, rainbow.getRGB());
            }

            y += 10;
            hue += 0.035f;
        }
    };
}
