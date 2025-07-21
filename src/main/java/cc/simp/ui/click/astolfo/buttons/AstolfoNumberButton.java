package cc.simp.ui.click.astolfo.buttons;

import cc.simp.font.FontManager;
import cc.simp.property.impl.DoubleProperty;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.awt.*;

public class AstolfoNumberButton extends AstolfoButton {
    public DoubleProperty setting;
    public Color color;

    public boolean dragged;

    public AstolfoNumberButton(float x, float y, float width, float height, DoubleProperty set, Color col) {
        super(x, y, width, height);

        color = col;
        setting = set;
    }

    @Override
    public void drawPanel(int mouseX, int mouseY) {
        double diff = setting.getMax() - setting.getMin();

        double percentWidth = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());

        if (dragged) {
            double val = setting.getMin() + (MathHelper.clamp_double((double) (mouseX - x) / width, 0, 1)) * diff;
            setting.setValue(Math.round(val * 100D)/ 100D);
        }

        Gui.drawRect2(x, y, width, height, 0xff181A17);
        Gui.drawRect2(x, y, (float) (percentWidth*width), height, color.getRGB());
        FontManager.getCurrentFont().drawStringWithShadow(setting.getLabel() + ": " + Math.round(setting.getValue() * 100D)/ 100D, x + 2, y + height / 2 - 0.5f - 3, 0xffffffff);
    }

    @Override
    public void mouseAction(int mouseX, int mouseY, boolean click, int button) {
        if (isHovered(mouseX, mouseY)) {
            dragged = true;
        }

        if(!click) dragged = false;
    }
}
