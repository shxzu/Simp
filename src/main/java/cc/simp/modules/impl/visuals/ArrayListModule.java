package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.ClickInterfaceModule;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.Translate;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.*;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Array List", category = ModuleCategory.VISUALS)
public final class ArrayListModule extends Module {

    private final Property<Boolean> bg = new Property<>("Background", true);
    private final Property<Boolean> line = new Property<>("Line", false);
    private final Property<Boolean> outline = new Property<>("Outline", true);

    private static final Map<Module, String> displayLabelCache = new HashMap<>();
    private static List<Module> moduleCache;

    @EventLink
    public Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (moduleCache != null) {
            for (Module module : moduleCache)
                displayLabelCache.put(module, getDisplayLabel(module));

            moduleCache.sort(new LengthComparator());
        }
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {

        CustomFontRenderer fr = FontProcess.getCurrentFont();

        ScaledResolution sr = new ScaledResolution(mc);

        float screenX = sr.getScaledWidth();

        long currentMillis = System.currentTimeMillis();

        if (currentMillis == -1)
            currentMillis = System.currentTimeMillis();

        if (moduleCache == null)
            updateModulePositions(sr);

        int y = 2;

        float previousModuleWidth = -1;

        final int moduleCacheSize = moduleCache.size();
        int lastVisibleModuleIndex = moduleCacheSize - 1;

        for (; lastVisibleModuleIndex > 0; lastVisibleModuleIndex--) {
            if (moduleCache.get(lastVisibleModuleIndex).isVisible())
                break;
        }

        int firstVisibleModuleIndex = -1;

        for (int i = 0; i < moduleCacheSize; i++) {
            final Module module = moduleCache.get(i);
            final Translate translate = module.getTranslate();
            final String name = displayLabelCache.get(module);
            final float moduleWidth = fr.getStringWidth(name);
            final boolean visible = module.isVisible();
            int visibleModuleIndex = i * 20;
            if (visible) {
                if (firstVisibleModuleIndex == -1)
                    firstVisibleModuleIndex = i;
                translate.animate(screenX - moduleWidth - (line.getValue() ? 2 : 1), y);
                y += 12;
            } else {
                translate.animate(screenX, y);
            }

            double translateX = translate.getX();
            double translateY = translate.getY();

            if (visible || translateX < screenX) {

                int aColor = RenderUtils.interpolateColorsBackAndForth(15, visibleModuleIndex, ColorProcess.colors.getFirst(), ColorProcess.colors.getSecond(), false).getRGB();

                if (ClickInterfaceModule.color.getValue() == ClickInterfaceModule.Color.Astolfo) {
                    aColor = RenderUtils.astolfoColors(visibleModuleIndex / 5, visibleModuleIndex).getRGB();
                }

                if (ClickInterfaceModule.color.getValue() == ClickInterfaceModule.Color.Rainbow) {
                    aColor = RenderUtils.rainbowColors(visibleModuleIndex / 5, visibleModuleIndex).getRGB();
                }

                double top = translateY - 2;
                if (bg.getValue()) {
                    Gui.drawRect(translateX - 1,
                            top,
                            screenX,
                            translateY + 10,
                            0x780D0D0D);
                }
                fr.drawStringWithShadow(
                        name,
                        (float) translateX,
                        (float) translateY - (FontProcess.getCurrentFont() == FontProcess.getFont("mc") ? 0 : 1),
                        aColor);
                if (outline.getValue()) {
                    Gui.drawRect(translateX - 2,
                            translateY - 2,
                            translateX - 1,
                            translateY + 10,
                            aColor);

                    double outlineTop = top - 1;
                    double outlineBottom = translateY + 10;

                    if (i != firstVisibleModuleIndex && moduleWidth - previousModuleWidth > 0) {
                        Gui.drawRect(translateX - 2,
                                outlineTop,
                                screenX - previousModuleWidth - 3,
                                outlineTop + 1,
                                aColor);
                    }

                    if (i != lastVisibleModuleIndex) {
                        Module nextModule = null;
                        int indexOffset = 1;

                        while (i + indexOffset <= lastVisibleModuleIndex) {
                            nextModule = moduleCache.get(i + indexOffset);
                            if (nextModule.isVisible())
                                break;
                            nextModule = null;
                            indexOffset++;
                        }

                        if (nextModule != null) {
                            String nextModuleName = displayLabelCache.get(nextModule);
                            float nextModuleWidth = fr.getStringWidth(nextModuleName);

                            if (moduleWidth - nextModuleWidth > 0.5)
                                Gui.drawRect(translateX - 2,
                                        outlineBottom,
                                        screenX - nextModuleWidth - 3,
                                        outlineBottom + 1,
                                        aColor);
                        }
                    } else {
                        Gui.drawRect(translateX - 2,
                                outlineBottom,
                                screenX,
                                outlineBottom + 1,
                                aColor);
                    }
                }
                if (line.getValue()) {
                    Gui.drawRect(screenX - 1,
                            translateY - 2,
                            screenX,
                            translateY + (10 - 2),
                            aColor);
                }
                previousModuleWidth = moduleWidth;
            }
        }
    };

    private static String getDisplayLabel(Module m) {
        String label = m.getLabel();
        String suffix = m.getSuffix();
        if (suffix != null) {
            return label + " \2477" + suffix;
        } else
            return label;
    }

    private void updateModulePositions(ScaledResolution scaledResolution) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        if (moduleCache == null)
            moduleCache = new ArrayList<>(Simp.INSTANCE.getModuleManager().getModules());

        int y = 1;
        for (Module module : moduleCache) {
            if (module.isEnabled()) {
                module.getTranslate().setX(scaledResolution.getScaledWidth() -
                        fr.getStringWidth(getDisplayLabel(module)) - 2);
            } else
                module.getTranslate().setX(scaledResolution.getScaledWidth());
            module.getTranslate().setY(y);
            if (module.isEnabled())
                y += 12;
        }
    }

    private abstract static class ModuleComparator implements Comparator<Module> {
        protected CustomFontRenderer fontRenderer;

        @Override
        public abstract int compare(Module o1, Module o2);

        public CustomFontRenderer getFontRenderer() {
            return fontRenderer;
        }

        public void setFontRenderer(CustomFontRenderer fontRenderer) {
            this.fontRenderer = fontRenderer;
        }
    }

    private static class LengthComparator extends ModuleComparator {
        @Override
        public int compare(Module o1, Module o2) {
            CustomFontRenderer fr = FontProcess.getCurrentFont();
            return Float.compare(
                    fr.getStringWidth(displayLabelCache.get(o2)),
                    fr.getStringWidth(displayLabelCache.get(o1)));
        }
    }

}
