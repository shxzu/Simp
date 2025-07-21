package cc.simp.ui.click.astolfo.buttons;

import cc.simp.font.FontManager;
import cc.simp.property.impl.EnumProperty;
import net.minecraft.client.gui.Gui;

import java.awt.*;

public class AstolfoModeButton extends AstolfoButton {
    public EnumProperty<?> setting;
    public Color color;

    public AstolfoModeButton(float x, float y, float width, float height, EnumProperty<?> set, Color col) {
        super(x, y, width, height);
        setting = set;
        color = col;
    }

    @Override
    public void drawPanel(int mouseX, int mouseY) {
        Gui.drawRect2(x, y, width, height, 0xff181A17);
        FontManager.getCurrentFont().drawStringWithShadow(setting.getLabel() + " = " + setting.getValue(), x + 2, y + height/2 - 0.5f - 3, 0xffffffff);
    }

    @Override
    public void mouseAction(int mouseX, int mouseY, boolean click, int button) {
        if(isHovered(mouseX, mouseY) && click) {
            if(button == 0) cycleEnumProperty(setting);
        }
    }

    private void cycleEnumProperty(EnumProperty<?> property) {
        Object current = property.getValue();
        Object[] values = current.getClass().getEnumConstants();
        int nextOrdinal = ((Enum<?>) current).ordinal() + 1;
        if (nextOrdinal >= values.length) nextOrdinal = 0;
        property.setValueObj(values[nextOrdinal]);
    }

}
