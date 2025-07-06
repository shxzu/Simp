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

import java.util.*;

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
        FontRenderer fontRenderer = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        List<Module> modules = new ArrayList<>();
        for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
            if (module.isEnabled()) {
                modules.add(module);
            }
        }
        modules.sort(Comparator.comparingDouble(module1 -> mc.fontRendererObj.getStringWidth(((Module) module1).getLabel())).reversed());

        int posY = 2;
        int screenWidth = sr.getScaledWidth();
        for (Module module : modules) {
            String displayName = module.getLabel();
            int stringWidth = fontRenderer.getStringWidth(displayName);
            fontRenderer.drawStringWithShadow(displayName, screenWidth - stringWidth - 2, posY, 0xFFFFFFFF);
            posY += 12;
        }
    };

}
