package cc.simp.utils.client.font;

import cc.simp.utils.client.Util;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.IOException;

public class TTFUtils extends Util {
    public static Font getFontFromLocation(String fileName, int size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, mc.getResourceManager()
                            .getResource(new ResourceLocation("simp/fonts/" + fileName))
                            .getInputStream())
                    .deriveFont(Font.PLAIN, size);
        } catch (FontFormatException | IOException ignored) {
            return null;
        }
    }
}
