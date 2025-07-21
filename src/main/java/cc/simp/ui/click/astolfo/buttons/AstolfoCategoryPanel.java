package cc.simp.ui.click.astolfo.buttons;

import cc.simp.Simp;
import cc.simp.font.FontManager;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.ArrayList;

public class AstolfoCategoryPanel extends AstolfoButton {
    public ModuleCategory category;
    public Color color;

    public boolean dragged, open;
    public int mouseX2, mouseY2;

    public float count;

    public ArrayList<AstolfoModuleButton> moduleButtons = new ArrayList<>();

    public AstolfoCategoryPanel(float x, float y, float width, float height, ModuleCategory cat, Color color) {
        super(x, y, width, height);
        category = cat;
        this.color = color;

        int count = 0;

        final float startY = y + height;

        for(Module mod : Simp.INSTANCE.getModuleManager().getModules()) {
            if(mod.getCategory() == category) {
                moduleButtons.add(new AstolfoModuleButton(x, startY + height*count, width, height, mod, color));
                count++;
            }
        }
    }

    @Override
    public void drawPanel(int mouseX, int mouseY) {
        if(dragged) {
            x = mouseX2 + mouseX;
            y = mouseY2 + mouseY;
        }


        Gui.drawRect2(x, y, width, height, 0xff181A17);
        FontManager.getCurrentFont().drawStringWithShadow(String.valueOf(category).toLowerCase(), x + 4, y + height / 2, 0xffffffff);

        count = 0;

        if(open) {

            final float startY = y + height;

            for (AstolfoModuleButton modulePanel : moduleButtons) {
                modulePanel.x = x;
                modulePanel.y = startY + count;
                modulePanel.drawPanel(mouseX, mouseY);
                count += modulePanel.finalHeight;
            }
        }

        Gui.drawRect2(x, (y + count) + height, width, height - 17, 0xff181A17);

        RenderUtils.drawBorder(x, y, width, count + height + 2 - 1, 2, color.getRGB());
    }

    @Override
    public void mouseAction(int mouseX, int mouseY, boolean click, int button) {
        if(isHovered(mouseX, mouseY)) {
            if(click) {
                if(button == 0) {
                    dragged = true;
                    mouseX2 = (int) (x - mouseX);
                    mouseY2 = (int) (y - mouseY);
                } else {
                    open = !open;
                }
            }
        }

        if(!click) dragged = false;
    }
}
