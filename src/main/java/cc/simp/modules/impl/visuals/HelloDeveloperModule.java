package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.FontProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Hello", category = ModuleCategory.VISUALS)
public class HelloDeveloperModule extends Module {

    public static final ModeProperty<Type> mode = new ModeProperty<>("Mode", Type.Developer);

    public enum Type {
        Developer,
        Beta,
        User,
        Neckhurt
    }

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);

        String text = "";
        switch (mode.getValue()) {
            case Developer:
                text = "Hello §bDeveloper!";
                break;
            case Beta:
                text = "Hello §aBeta!";
                break;
            case User:
                text = "Hello §7User!";
                break;
            case Neckhurt:
                text = "Hello §0Neckhurt!";
                break;
        }

        float textWidth = fr.getStringWidth(text);
        float x = sr.getScaledWidth() - textWidth - 2;
        float y = sr.getScaledHeight() - fr.FONT_HEIGHT - 2;

        fr.drawStringWithShadow(text, x, y, Color.WHITE.getRGB());
    };
}