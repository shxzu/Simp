package cc.simp.interfaces.menu.alt;

import cc.simp.api.font.CustomFontRenderer;
import cc.simp.modules.impl.client.ClientSettingsModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class CustomTextBox extends Gui {
    private final int id;
    private final CustomFontRenderer fontRendererInstance;
    public int xPosition;
    public int yPosition;

    @Getter @Setter
    private int width, height;

    @Getter
    private String text = "";

    private final int maxStringLength = 32;

    @Getter @Setter
    private boolean focused;

    @Setter
    private String placeholder = "";

    private boolean selectedAll;

    public CustomTextBox(int componentId, CustomFontRenderer fontRendererObj, int x, int y, int w, int h) {
        this.id = componentId;
        this.fontRendererInstance = fontRendererObj;
        this.xPosition = x;
        this.yPosition = y;
        this.width = w;
        this.height = h;
    }

    public void drawTextBox() {
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, new Color(43, 43, 43).getRGB());
        drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, new Color(30, 30, 30).getRGB());

        boolean empty = text.isEmpty();
        String renderText = empty ? placeholder : text;

        int color = empty ? 0x777777 : 0xFFFFFF;

        fontRendererInstance.drawString(
                renderText + (focused && !empty && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : ""),
                xPosition + 4,
                yPosition + (height / 2f) - (fontRendererInstance.getHeight() / 2f),
                color
        );
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        focused = mouseButton == 0 &&
                mouseX >= xPosition &&
                mouseX <= xPosition + width &&
                mouseY >= yPosition &&
                mouseY <= yPosition + height;

        if (!focused) selectedAll = false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (keyCode == Keyboard.KEY_A && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            selectedAll = true;
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (selectedAll) {
                text = "";
                selectedAll = false;
                return;
            }
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
            selectedAll = false;
            return;
        }

        if (typedChar >= 32 && typedChar != 127) {
            if (selectedAll) {
                text = "";
                selectedAll = false;
            }
            if (text.length() < maxStringLength) {
                text += typedChar;
            }
        }
    }

    public void setText(String text) {
        this.text = text.length() > maxStringLength ? text.substring(0, maxStringLength) : text;
    }
}
