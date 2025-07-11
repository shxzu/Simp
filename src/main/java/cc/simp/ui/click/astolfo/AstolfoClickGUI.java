package cc.simp.ui.click.astolfo;

import cc.simp.modules.ModuleCategory;
import cc.simp.ui.click.astolfo.buttons.AstolfoButton;
import cc.simp.ui.click.astolfo.buttons.AstolfoCategoryPanel;
import cc.simp.ui.click.astolfo.buttons.AstolfoModuleButton;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class AstolfoClickGUI extends GuiScreen {

    public ArrayList<AstolfoCategoryPanel> categoryPanels = new ArrayList<>();

    public AstolfoClickGUI() {

        int count = 4;

        for (ModuleCategory cat : ModuleCategory.values()) {
            switch (cat) {
                case COMBAT:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, new Color(0xffE64D3A)));
                    break;
                case MOVEMENT:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, new Color(0xff2ECD6F)));
                    break;
                case PLAYER:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, new Color(0xff8E45AE)));
                    break;
                case EXPLOIT:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, new Color(0xff3398D9)));
                    break;
                case RENDER:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, new Color(0xff3601CE)));
                    break;
                case CLIENT:
                    categoryPanels.add(new AstolfoCategoryPanel(count, 4, 100, 18, cat, Color.YELLOW.brighter()));
                    break;
            }

            count += 120;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (AstolfoCategoryPanel catPanel : categoryPanels) {
            catPanel.drawPanel(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (AstolfoCategoryPanel catPan : categoryPanels) {
            catPan.mouseAction(mouseX, mouseY, true, mouseButton);

            if (catPan.open) {
                for (AstolfoModuleButton modPan : catPan.moduleButtons) {
                    modPan.mouseAction(mouseX, mouseY, true, mouseButton);
                    if (modPan.extended) {
                        for (AstolfoButton pan : modPan.astolfoButtons) {
                            pan.mouseAction(mouseX, mouseY, true, mouseButton);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (AstolfoCategoryPanel catPan : categoryPanels) {
            catPan.mouseAction(mouseX, mouseY, false, state);

            if (catPan.open) {
                for (AstolfoModuleButton modPan : catPan.moduleButtons) {
                    modPan.mouseAction(mouseX, mouseY, false, state);

                    if (modPan.extended) {
                        for (AstolfoButton pan : modPan.astolfoButtons) {
                            pan.mouseAction(mouseX, mouseY, false, state);
                        }
                    }
                }
            }
        }
    }
}
