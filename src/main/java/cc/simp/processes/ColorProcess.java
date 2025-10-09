package cc.simp.processes;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.interfaces.click.ClickInterface;
import cc.simp.modules.impl.client.ClickInterfaceModule;
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
        switch (ClickInterfaceModule.color.getValue()) {
            case Rainbow -> {
                colors = Pair.of(RenderUtils.rainbowColors(15, 75), RenderUtils.rainbowColors(15, 75));
                color = RenderUtils.rainbowColors(15, 75);
            }
            case Astolfo -> {
                colors = Pair.of(RenderUtils.astolfoColors(15, 75), RenderUtils.astolfoColors(15, 75));
                color = RenderUtils.astolfoColors(15, 75);
            }
            case Simp -> {
                first = new Color(54, 59, 181);
                second = new Color(98, 102, 217);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case White -> {
                first = new Color(255, 255, 255);
                second = new Color(200, 200, 200);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Red -> {
                first = new Color(255, 0, 0);
                second = new Color(200, 0, 0);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Purple -> {
                first = new Color(128, 0, 128);
                second = new Color(98, 0, 98);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
            case Pink -> {
                first = new Color(255, 192, 203);
                second = new Color(235, 172, 183);
                colors = Pair.of(first, second);
                color = RenderUtils.interpolateColorsBackAndForth(15, 75, colors.getFirst(), colors.getSecond(), false);
            }
        }
    };
}
