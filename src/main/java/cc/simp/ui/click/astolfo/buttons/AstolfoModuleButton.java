package cc.simp.ui.click.astolfo.buttons;

import cc.simp.modules.Module;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.font.FontManager;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.ArrayList;

public class AstolfoModuleButton extends AstolfoButton {
    public Module module;
    public Color color;

    public boolean extended;

    public float finalHeight;

    public ArrayList<AstolfoButton> astolfoButtons = new ArrayList<>();

    public AstolfoModuleButton(float x, float y, float width, float height, Module mod, Color col) {
        super(x, y, width, height);

        module = mod;

        color = col;

        final float startY = y + height;

        int count = 0;

        for(Property<?> set : module.getElements()) {
            if(set.getType() ==  Boolean.class) astolfoButtons.add(new AstolfoBooleanButton(x, startY + 18*count, width, 9, (Property<Boolean>) set, color));
            if(set instanceof EnumProperty) astolfoButtons.add(new AstolfoModeButton(x, startY + 18*count, width, 9, (EnumProperty<?>)set, color));
            if(set instanceof DoubleProperty) astolfoButtons.add(new AstolfoNumberButton(x, startY + 18*count, width, 9, (DoubleProperty)set, color));
            count++;
        }
    }

    @Override
    public void drawPanel(int mouseX, int mouseY) {
        Gui.drawRect2(x, y, width, height, 0xff181A17);

        if(!extended)
            Gui.drawRect2(x + 1, y, width - 2, height, module.isEnabled() ? color.getRGB() : 0xff232623);
        else
            Gui.drawRect2(x + 1, y, width - 2, height, 0xff181A17);

        FontManager.ARIAL.drawStringWithShadow(module.getLabel().toLowerCase(), (x + width) - FontManager.ARIAL.getWidth(module.getLabel().toLowerCase()) - 3, y + height/2, extended ? module.isEnabled() ? color.getRGB() : 0xffffffff : 0xffffffff);

        int count = 0;

        float hehe = 0;

        if(extended) {
            final float startY = y + height;
            for(AstolfoButton pan : astolfoButtons) {
                pan.x = x;
                pan.y = startY + pan.height*count;
                pan.drawPanel(mouseX, mouseY);
                count++;

                hehe = pan.height;
            }
        }

        finalHeight = hehe * count + height;
    }

    @Override
    public void mouseAction(int mouseX, int mouseY, boolean click, int button) {
        if(isHovered(mouseX, mouseY) && click) {
            if(button == 0) {
                module.toggle();
            } else if(!module.getElements().isEmpty()) extended = !extended;
        }
    }
}
