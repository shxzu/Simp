package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.FontUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Greeting", category = ModuleCategory.VISUALS)
public class GreetingModule extends Module {

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        CustomFontRenderer fr = FontUtils.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);

        String user = mc.thePlayer.getName();

        String text = "Hello §b" + user + "!";

        if(user.equalsIgnoreCase("x0lumie")) {
            text = "Hello §bDeveloper!";
        } else if(user.equalsIgnoreCase("TheAdamMC")) {
            text = "Hello §bDeveloper!";
        }

        float textWidth = fr.getStringWidth(text);
        float x = sr.getScaledWidth() - textWidth - 2;
        float y = sr.getScaledHeight() - fr.FONT_HEIGHT - 2;

        fr.drawStringWithShadow(text, x, y, Color.WHITE.getRGB());
    };
}