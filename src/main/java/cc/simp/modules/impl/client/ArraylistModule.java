package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.utils.font.FontManager;
import cc.simp.utils.font.TrueTypeFontRenderer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Arraylist", category = ModuleCategory.CLIENT)
public final class ArraylistModule extends Module {

    private final Property<Boolean> leftProperty = new Property<>("Align Left", false);
    private final Property<Boolean> importantProperty = new Property<>("Only Important", false);

    public ArraylistModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        if (mc.gameSettings.showDebugInfo) return;

        MinecraftFontRenderer minecraftFontRenderer = mc.minecraftFontRendererObj;
        TrueTypeFontRenderer CFont = FontManager.TAHOMA;
        ScaledResolution sr = new ScaledResolution(mc);

        float hue = (System.currentTimeMillis() % 3000) / 3000f;
        boolean left = leftProperty.getValue(); // Can be made configurable
        int y = left ? 12 : 1;

        List<Module> modules = new CopyOnWriteArrayList<>();

        // Collect enabled modules
        for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
            if (module.isEnabled()) {
                if (!importantProperty.getValue() || (module.getCategory() != ModuleCategory.CLIENT && module.getCategory() != ModuleCategory.RENDER))
                    modules.add(module);
            }
        }

        // Sort modules by width
        if (FontManagerModule.fontTypeProperty.getValue() == FontManagerModule.FontType.TAHOMA) {
            modules.sort(Comparator.comparingDouble(m ->
                    -CFont.getWidth(m.getUpdatedSuffix() != null ?
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

            String text = module.getLabel() + (module.getUpdatedSuffix() != null ? " §7" + module.getUpdatedSuffix() : "");
            float x = left ? 2.0f : sr.getScaledWidth() - minecraftFontRenderer.getStringWidth(text) - 1.0f;
            if (FontManagerModule.fontTypeProperty.getValue() == FontManagerModule.FontType.TAHOMA)
                x = left ? 2.0f : sr.getScaledWidth() - CFont.getWidth(text) - 1.0f;

            Color rainbow = Color.getHSBColor(hue, 0.55f, 0.9f);
            if (FontManagerModule.fontTypeProperty.getValue() == FontManagerModule.FontType.TAHOMA) {
                CFont.drawStringWithShadow(text, x, y, rainbow.getRGB());
            } else {
                minecraftFontRenderer.drawStringWithShadow(text, x, y, rainbow.getRGB());
            }

            y += 10;
            hue += 0.035f;
        }
    };

}
