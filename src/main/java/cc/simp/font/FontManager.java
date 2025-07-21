package cc.simp.font;

import cc.simp.font.CFontRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, CFontRenderer> fontRegistry = new HashMap<>();
    private static final Map<String, CFontRenderer> scaledFontCache = new HashMap<>();
    private static String currentFont = "tahoma";
    
    static {
        registerFont("tahoma", createFont("tahoma", 20));
        registerFont("nunito", createFont("nunito", 20));
    }

    public static void registerFont(String name, CFontRenderer font) {
        fontRegistry.put(name, font);
    }
    
    public static CFontRenderer getFont(String name) {
        return fontRegistry.get(name);
    }
    
    public static void setCurrentFont(String name) {
        if (fontRegistry.containsKey(name)) {
            currentFont = name;
        }
    }
    
    public static CFontRenderer getCurrentFont() {
        return getFont(currentFont);
    }

    public static void swapFonts(String name1, String name2) {
        CFontRenderer font1 = getFont(name1);
        CFontRenderer font2 = getFont(name2);
        
        if (font1 != null && font2 != null) {
            fontRegistry.put(name1, font2);
            fontRegistry.put(name2, font1);
            
            if (currentFont.equals(name1)) currentFont = name2;
            else if (currentFont.equals(name2)) currentFont = name1;
        }
    }
    
    public static void aliasFont(String aliasName, String sourceName) {
        CFontRenderer source = getFont(sourceName);
        if (source != null) {
            registerFont(aliasName, source);
        }
    }
    
    public static void rebindFont(String name, CFontRenderer newFont) {
        if (fontRegistry.containsKey(name)) {
            fontRegistry.put(name, newFont);
        }
    }
    
    //Ngl deepseek made the caching system :pray:
    
    public static CFontRenderer getScaledFont(String name, float scale) {
        String cacheKey = name + "|" + scale; 
        if (scaledFontCache.containsKey(cacheKey)) {
            return scaledFontCache.get(cacheKey);
        }
        
        CFontRenderer original = getFont(name);
        if (original == null) return null;
        
        String fontName = original.getNameFontTTF();
        float originalSize = original.getFont().getSize();
        float newSize = originalSize * scale;
        
        CFontRenderer scaledFont = new CFontRenderer(fontName, newSize, Font.PLAIN, true, false);
        scaledFontCache.put(cacheKey, scaledFont);
        
        return scaledFont;
    }

    public static void clearScaledFontCache() {
        scaledFontCache.clear();
    }

    private static CFontRenderer createFont(String name, int size) {
        return new CFontRenderer(name, size, Font.PLAIN, true, false);
    }
}