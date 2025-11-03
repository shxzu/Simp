package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
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
    private final ModeProperty<BGColorMode> bgColor = new ModeProperty<>("Background Color", BGColorMode.Normal, bg::getValue);
    private final NumberProperty bgAlpha = new NumberProperty("Background Opacity", 100, bg::getValue, 1, 255, 1);
    private final Property<Boolean> outline = new Property<>("Outline", true);
    private final ModeProperty<LineMode> line = new ModeProperty<>("Line", LineMode.Off, () -> !outline.getValue());
    private final Property<Boolean> hideVisuals = new Property<>("Hide Visuals", false);
    private final Property<Boolean> showSuffix = new Property<>("Show Suffix", true);
    private final Property<Boolean> lowercase = new Property<>("Lowercase", true);
    private final ModeProperty<ColorMode> colorMode = new ModeProperty<>("Color Mode", ColorMode.Fade);
    private final NumberProperty offsetX = new NumberProperty("Offset X", 0, -100, 100, 1);
    private final NumberProperty offsetY = new NumberProperty("Offset Y", 0, -100, 100, 1);
    private final Property<Boolean> roundedBg = new Property<>("Rounded Background", false);
    private final NumberProperty roundRadius = new NumberProperty("Round Radius", 3, roundedBg::getValue, 0, 10, 0.5);
    private final NumberProperty roundedSpacing = new NumberProperty("Rounded Spacing", 1, roundedBg::getValue, 0, 5, 0.5);

    private static final Map<Module, String> displayLabelCache = new HashMap<>();
    private static List<Module> moduleCache;

    public enum BGColorMode {
        Normal, Theme, White
    }

    public enum ColorMode {
        Static, Fade
    }

    private enum LineMode {
        Off("Off"),
        Rise("Rise"),
        Top("Top"),
        Right("Right"),
        RightTop("Right Top"),
        Left("Left"),
        Bottom("Bottom");

        public String name;

        LineMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventLink
    public Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (moduleCache != null) {
            for (Module module : moduleCache)
                displayLabelCache.put(module, getDisplayLabel(module));

            moduleCache.sort(new LengthComparator());
        }
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> renderArrayList();

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> renderArrayList();

    private void renderArrayList() {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);

        float screenX = sr.getScaledWidth() - offsetX.getValue().floatValue();
        float startY = 2 + offsetY.getValue().floatValue();

        if (moduleCache == null)
            updateModulePositions(sr);

        float y = startY;
        float previousModuleWidth = -1;

        // Build a filtered list of modules that should be rendered
        List<Module> filteredModules = new ArrayList<>();
        for (Module module : moduleCache) {
            if (hideVisuals.getValue() && module.getCategory() == ModuleCategory.VISUALS) {
                continue;
            }
            filteredModules.add(module);
        }

        final int moduleCacheSize = filteredModules.size();
        int lastVisibleModuleIndex = moduleCacheSize - 1;

        for (; lastVisibleModuleIndex > 0; lastVisibleModuleIndex--) {
            if (filteredModules.get(lastVisibleModuleIndex).isVisible())
                break;
        }

        int firstVisibleModuleIndex = -1;
        float spacing = roundedBg.getValue() ? roundedSpacing.getValue().floatValue() : 0;

        for (int i = 0; i < moduleCacheSize; i++) {
            final Module module = filteredModules.get(i);
            final Translate translate = module.getTranslate();
            final String name = displayLabelCache.get(module);
            final float moduleWidth = fr.getStringWidth(name);
            final boolean visible = module.isVisible();
            int visibleModuleIndex = i * 20;

            if (visible) {
                if (firstVisibleModuleIndex == -1)
                    firstVisibleModuleIndex = i;
                translate.animate(screenX - moduleWidth - (line.getValue() != LineMode.Off ? 2 : 1), y);
                y += 12 + spacing;
            } else {
                translate.animate(screenX, y);
            }

            double translateX = translate.getX();
            double translateY = translate.getY();

            if (visible || translateX < screenX) {
                int aColor = getColorForModule(visibleModuleIndex);
                double top = translateY - 2;

                if (bg.getValue()) {
                    if (roundedBg.getValue()) {
                        RenderUtils.drawRoundedRect(translateX - 1,
                                top,
                                moduleWidth + 2,
                                12,
                                roundRadius.getValue(),
                                new Color(getColorForBG()));
                    } else {
                        Gui.drawRect(translateX - 1,
                                translateY - 2,
                                screenX,
                                translateY + 10,
                                getColorForBG());
                    }
                }

                fr.drawStringWithShadow(
                        name,
                        (float) translateX,
                        (float) translateY - (FontProcess.getCurrentFont() == FontProcess.getFont("mc") ? 0 : 1),
                        aColor);

                if (outline.getValue() && !roundedBg.getValue()) {
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
                            nextModule = filteredModules.get(i + indexOffset);
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
                    if (i == firstVisibleModuleIndex) {
                        Gui.drawRect(screenX - 1,
                                startY - 2,
                                screenX,
                                translateY + 10 + spacing,
                                aColor);
                    } else {
                        Module prevModule = null;
                        for (int j = i - 1; j >= 0; j--) {
                            if (filteredModules.get(j).isVisible()) {
                                prevModule = filteredModules.get(j);
                                break;
                            }
                        }
                        if (prevModule != null) {
                            double prevY = prevModule.getTranslate().getY();
                            Gui.drawRect(screenX - 1,
                                    prevY + 10,
                                    screenX,
                                    translateY + 10 + spacing,
                                    aColor);
                        }
                    }
                    if (i == firstVisibleModuleIndex) {
                        Gui.drawRect(translateX - 2,
                                startY - 2,
                                screenX,
                                startY - 1,
                                aColor);
                    }
                }

                if (line.getValue() != LineMode.Off) {
                    if (line.getValue() == LineMode.Rise) {
                        Gui.drawRect(screenX - 1,
                                translateY - 2,
                                screenX,
                                translateY + 8,
                                aColor);
                    } else if (line.getValue() == LineMode.Right) {
                        if (i == firstVisibleModuleIndex) {
                            Gui.drawRect(screenX - 1,
                                    startY - 2,
                                    screenX,
                                    translateY + 10 + spacing,
                                    aColor);
                        } else {
                            Module prevModule = null;
                            for (int j = i - 1; j >= 0; j--) {
                                if (filteredModules.get(j).isVisible()) {
                                    prevModule = filteredModules.get(j);
                                    break;
                                }
                            }
                            if (prevModule != null) {
                                double prevY = prevModule.getTranslate().getY();
                                Gui.drawRect(screenX - 1,
                                        prevY + 10,
                                        screenX,
                                        translateY + 10 + spacing,
                                        aColor);
                            }
                        }
                    } else if (line.getValue() == LineMode.RightTop) {
                        if (i == firstVisibleModuleIndex) {
                            Gui.drawRect(screenX - 1,
                                    startY - 2,
                                    screenX,
                                    translateY + 10 + spacing,
                                    aColor);
                        } else {
                            Module prevModule = null;
                            for (int j = i - 1; j >= 0; j--) {
                                if (filteredModules.get(j).isVisible()) {
                                    prevModule = filteredModules.get(j);
                                    break;
                                }
                            }
                            if (prevModule != null) {
                                double prevY = prevModule.getTranslate().getY();
                                Gui.drawRect(screenX - 1,
                                        prevY + 10,
                                        screenX,
                                        translateY + 10 + spacing,
                                        aColor);
                            }
                        }
                        if (i == firstVisibleModuleIndex) {
                            Gui.drawRect(translateX - 2,
                                    startY - 2,
                                    screenX,
                                    startY - 1,
                                    aColor);
                        }
                    } else if (line.getValue() == LineMode.Left) {
                        Gui.drawRect(translateX - 2,
                                translateY - 2,
                                translateX - 1,
                                translateY + 10,
                                aColor);
                    } else if (line.getValue() == LineMode.Bottom) {
                        if (i == lastVisibleModuleIndex) {
                            Gui.drawRect(translateX - 1,
                                    translateY + 10,
                                    screenX,
                                    translateY + 11,
                                    aColor);
                        }
                    } else if (line.getValue() == LineMode.Top) {
                        if (i == firstVisibleModuleIndex) {
                            Gui.drawRect(translateX - 2,
                                    startY - 2,
                                    screenX,
                                    startY - 1,
                                    aColor);
                        }
                    }
                }
                previousModuleWidth = moduleWidth;
            }
        }
    }

    private int getColorForBG() {
        int alpha = bgAlpha.getValue().intValue();

        return switch (bgColor.getValue()) {
            case Normal -> new Color(0, 0, 0, alpha).getRGB();
            case Theme ->
                    new Color(ColorProcess.getColor().getRed(), ColorProcess.getColor().getGreen(), ColorProcess.getColor().getBlue(), alpha).getRGB();
            case White -> new Color(255, 255, 255, alpha).getRGB();
        };
    }

    private int getColorForModule(int visibleModuleIndex) {
        int offset = colorMode.getValue() == ColorMode.Fade ? visibleModuleIndex : 0;

        if (ClickInterfaceModule.color.getValue() == ClickInterfaceModule.Color.Astolfo) {
            return RenderUtils.astolfoColors(offset / 2, offset).getRGB();
        }

        if (ClickInterfaceModule.color.getValue() == ClickInterfaceModule.Color.Rainbow) {
            return RenderUtils.rainbowColors(offset / 2, offset).getRGB();
        }

        return RenderUtils.interpolateColorsBackAndForth(15, offset, ColorProcess.colors.getFirst(), ColorProcess.colors.getSecond(), false).getRGB();
    }

    private String getDisplayLabel(Module m) {
        String label = m.getLabel();
        String suffix = m.getSuffix();

        if (lowercase.getValue()) {
            label = label.toLowerCase();
            if (suffix != null) {
                suffix = suffix.toLowerCase();
            }
        }

        if (suffix != null && showSuffix.getValue()) {
            return label + " \2477" + suffix;
        } else
            return label;
    }


    private void updateModulePositions(ScaledResolution scaledResolution) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        if (moduleCache == null)
            moduleCache = new ArrayList<>(Simp.INSTANCE.getModuleManager().getModules());

        float y = 2 + offsetY.getValue().floatValue();
        float screenX = scaledResolution.getScaledWidth() - offsetX.getValue().floatValue();
        float spacing = roundedBg.getValue() ? roundedSpacing.getValue().floatValue() : 0;

        for (Module module : moduleCache) {
            if (hideVisuals.getValue() && module.getCategory() == ModuleCategory.VISUALS) {
                continue;
            }

            if (module.isEnabled()) {
                module.getTranslate().setX(screenX -
                        fr.getStringWidth(getDisplayLabel(module)) + 2);
            } else
                module.getTranslate().setX(screenX);
            module.getTranslate().setY(y);
            if (module.isEnabled())
                y += 12 + spacing;
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
