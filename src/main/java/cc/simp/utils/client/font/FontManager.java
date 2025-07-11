package cc.simp.utils.client.font;

import java.awt.*;

public final class FontManager {

    public static final TrueTypeFontRenderer TAHOMA =
            new TrueTypeFontRenderer(
            new Font("Tahoma", Font.PLAIN, 18), true, false);
    public static final TrueTypeFontRenderer ARIAL =
            new TrueTypeFontRenderer(
            new Font("Arial", Font.PLAIN, 18), true, false);

    private FontManager() {
    }

    public static void initTextures() {
        TAHOMA.generateTextures();
        ARIAL.generateTextures();
    }
}
