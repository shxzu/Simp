package cc.simp.ui.click.astolfo.buttons;

import cc.simp.property.Property;
import cc.simp.utils.client.font.FontManager;
import net.minecraft.client.gui.Gui;

import java.awt.*;

public class AstolfoBooleanButton extends AstolfoButton {
    public Property<Boolean> setting;
    public Color color;

    public AstolfoBooleanButton(float x, float y, float width, float height, Property<Boolean> set, Color col) {
        super(x, y, width, height);
        setting = set;
        color = col;
    }

    @Override
    public void drawPanel(int mouseX, int mouseY) {
        Gui.drawRect2(x, y, width, height, 0xff181A17);
        if(setting.getValue()) Gui.drawRect2(x + 1, y, width - 2, height, color.getRGB());
        FontManager.ARIAL.drawStringWithShadow(setting.getLabel(), x + 2, y + height / 2 - 0.5f - 3, 0xffffffff);
    }

    @Override
    public void mouseAction(int mouseX, int mouseY, boolean click, int button) {
        if(isHovered(mouseX, mouseY) && click) {
            setting.setValue(!setting.getValue());
        }
    }
}
