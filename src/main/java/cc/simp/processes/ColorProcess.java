package cc.simp.processes;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.modules.impl.client.ClientSettingsModule;
import cc.simp.utils.misc.Pair;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.Getter;

import java.awt.*;

public class ColorProcess {
    @Getter
    private static Color color = new Color(255, 255, 255);
    @Getter
    public static Pair<Color, Color> colors = Pair.of(new Color(255, 255, 255), new Color(255, 255, 255));

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        Color first = new Color(255, 255, 255);
        Color second = new Color(255, 255, 255);
        switch (ClientSettingsModule.color.getValue()) {
            case Rainbow -> {
                colors = Pair.of(RenderUtils.rainbowColors(15, 75), RenderUtils.rainbowColors(15, 75));
                color = RenderUtils.rainbowColors(15, 75);
            }
            case Astolfo -> {
                colors = Pair.of(RenderUtils.astolfoColors(15, 75), RenderUtils.astolfoColors(15, 75));
                color = RenderUtils.astolfoColors(15, 75);
            }
            case Exhibition -> {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                color = Color.getHSBColor(hue, 0.55f, 0.9f);
                colors = Pair.of(color, color);
            }
            case Simp -> {
                first = new Color(54, 59, 181);
                second = new Color(98, 102, 217);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case White -> {
                first = new Color(255, 255, 255);
                second = new Color(155, 155, 155);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Red -> {
                first = new Color(255, 57, 57);
                second = new Color(168, 14, 14);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Ruby -> {
                first = new Color(255, 0, 0);
                second = new Color(100, 0, 0);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case DarkPurple -> {
                first = new Color(100, 0, 180);
                second = new Color(50, 0, 130);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Purple -> {
                first = new Color(199, 139, 255);
                second = new Color(132, 26, 236);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Lavender -> {
                first = new Color(194, 156, 255);
                second = new Color(131, 101, 182);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Pink -> {
                first = new Color(255, 90, 255);
                second = new Color(255, 205, 255);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case HotPink -> {
                first = new Color(255, 0, 255);
                second = new Color(154, 51, 154);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Vaporwave -> {
                first = new Color(180, 0, 180);
                second = new Color(0, 200, 255);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Sunset -> {
                first = new Color(161, 82, 230);
                second = new Color(255, 104, 69);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 20, colors.getFirst(), colors.getSecond(), false);
            }
            case Tenacity -> {
                first = new Color(236, 133, 209);
                second = new Color(28, 167, 222);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case FDP -> {
                first = new Color(29, 116, 148);
                second = new Color(38, 180, 113);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Rise -> {
                first = new Color(71, 148, 253);
                second = new Color(71, 253, 160);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
        }
    };
}
