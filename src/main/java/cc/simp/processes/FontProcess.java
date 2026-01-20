package cc.simp.processes;

import cc.simp.api.font.CustomFontRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontProcess {
    private static final Map<String, CustomFontRenderer> fontRegistry = new HashMap<>();
    private static final Map<String, CustomFontRenderer> scaledFontCache = new HashMap<>();
    private static String currentFont = "simp";

    public final static String
            BUG = "a",
            LIST = "b",
            BOMB = "c",
            EYE = "d",
            PERSON = "e",
            WHEELCHAIR = "f",
            SCRIPT = "g",
            SKIP_LEFT = "h",
            PAUSE = "i",
            PLAY = "j",
            SKIP_RIGHT = "k",
            SHUFFLE = "l",
            INFO = "m",
            SETTINGS = "n",
            CHECKMARK = "o",
            XMARK = "p",
            TRASH = "q",
            WARNING = "r",
            FOLDER = "s",
            LOAD = "t",
            SAVE = "u",
            UPVOTE_OUTLINE = "v",
            UPVOTE = "w",
            DOWNVOTE_OUTLINE = "x",
            DOWNVOTE = "y",
            DROPDOWN_ARROW = "z",
            PIN = "s",
            EDIT = "A",
            SEARCH = "B",
            UPLOAD = "C",
            REFRESH = "D",
            ADD_FILE = "E",
            STAR_OUTLINE = "F",
            STAR = "G";

    static {
        registerFont("simp", createFont("simp", 18));
        registerFont("bold", createFont("simp-bold", 18));
        registerFont("big", createFont("simp-bold", 48));
        registerFont("noto", createFont("noto", 18));
        registerFont("arial", createFont("arial", 18));
        registerFont("apple", createFont("apple", 18));
        registerFont("sans", createFont("sans", 18));
        registerFont("convection", createFont("convection", 18));
        registerFont("icon", createFont("icon", 40));
        registerFont("mc", createFont("mc", 18));
    }

    public static void registerFont(String name, CustomFontRenderer font) {
        fontRegistry.put(name, font);
    }

    public static CustomFontRenderer getFont(String name) {
        return fontRegistry.get(name);
    }

    public static void setCurrentFont(String name) {
        if (fontRegistry.containsKey(name)) {
            currentFont = name;
        }
    }

    public static CustomFontRenderer getCurrentFont() {
        return getFont(currentFont);
    }

    public static void swapFonts(String name1, String name2) {
        CustomFontRenderer font1 = getFont(name1);
        CustomFontRenderer font2 = getFont(name2);

        if (font1 != null && font2 != null) {
            fontRegistry.put(name1, font2);
            fontRegistry.put(name2, font1);

            if (currentFont.equals(name1)) currentFont = name2;
            else if (currentFont.equals(name2)) currentFont = name1;
        }
    }

    public static void aliasFont(String aliasName, String sourceName) {
        CustomFontRenderer source = getFont(sourceName);
        if (source != null) {
            registerFont(aliasName, source);
        }
    }

    public static void rebindFont(String name, CustomFontRenderer newFont) {
        if (fontRegistry.containsKey(name)) {
            fontRegistry.put(name, newFont);
        }
    }

    public static CustomFontRenderer getScaledFont(String name, float scale) {
        String cacheKey = name + "|" + scale;
        if (scaledFontCache.containsKey(cacheKey)) {
            return scaledFontCache.get(cacheKey);
        }

        CustomFontRenderer original = getFont(name);
        if (original == null) return null;

        String fontName = original.getNameFontTTF();
        float originalSize = original.getFont().getSize();
        float newSize = originalSize * scale;

        CustomFontRenderer scaledFont = new CustomFontRenderer(fontName, newSize, Font.PLAIN, true, false);
        scaledFontCache.put(cacheKey, scaledFont);

        return scaledFont;
    }

    public static void clearScaledFontCache() {
        scaledFontCache.clear();
    }

    private static CustomFontRenderer createFont(String name, int size) {
        return new CustomFontRenderer(name, size, Font.PLAIN, true, false);
    }

}
