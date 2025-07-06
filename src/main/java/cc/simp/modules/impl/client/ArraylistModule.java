package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.event.impl.render.overlay.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Arraylist", category = ModuleCategory.CLIENT)
public final class ArraylistModule extends Module {

    private static final Map<Module, String> displayLabelCache = new HashMap<>();
    private static List<Module> moduleCache;

    public ArraylistModule() {
        toggle();
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        if (mc.gameSettings.showDebugInfo) return;

        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        float hue = (System.currentTimeMillis() % 3000) / 3000f;
        boolean left = false; // Can be made configurable
        int y = left ? 12 : 1;

        List<Module> modules = new CopyOnWriteArrayList<>();

        // Collect enabled modules
        for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
            if (module.isEnabled()) {
                modules.add(module);
            }
        }

        // Sort modules by width
        modules.sort(Comparator.comparingDouble(m ->
                -fontRenderer.getStringWidth(m.getUpdatedSuffix() != null ?
                        m.getLabel() + " " + m.getUpdatedSuffix() :
                        m.getLabel())
        ));

        // Render modules
        for (Module module : modules) {
            if (hue > 1.0f) hue = 0.0f;

            String text = module.getLabel() + (module.getUpdatedSuffix() != null ? " §7" + module.getUpdatedSuffix() : "");
            float x = left ? 2.0f : sr.getScaledWidth() - fontRenderer.getStringWidth(text) - 1.0f;

            Color rainbow = Color.getHSBColor(hue, 0.55f, 0.9f);
            fontRenderer.drawStringWithShadow(text, x, y, rainbow.getRGB());

            y += 10;
            hue += 0.035f;
        }
    };

}
