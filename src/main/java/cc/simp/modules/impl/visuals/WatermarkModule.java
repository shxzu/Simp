package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.shaders.RoundedShader;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.VISUALS)
public final class WatermarkModule extends Module {

    public static final ModeProperty<Type> type = new ModeProperty<>("Client Watermark Type", Type.Simple);
    public static final Property<Boolean> info = new Property<>("Watermark Info", true, () -> type.getValue() != Type.GameSense && type.getValue() != Type.Logo && type.getValue() != Type.DynamicIsland);
    public static final ModeProperty<FontType> fontType = new ModeProperty<>("Font", FontType.Simp);
    public static final Property<Boolean> useCustomFont = new Property<>("Use Custom Font", true);

    private long lastServerTime;
    private long lastUpdateTime;
    private String currentServer;
    private float islandWidth;
    private float islandHeight;
    private ResourceLocation clientLogo;
    private RoundedShader bloomShader;

    private int lastBlockCount = 0;
    private int currentBlockCount = 0;
    private float scaffoldBoxOffset = 0;
    private float scaffoldBoxAlpha = 0;
    private boolean scaffoldWasActive = false;

    public WatermarkModule() {
        toggle();

        String bloomFragmentShader = "#version 120\n" +
                "\n" +
                "uniform vec2 location, rectSize;\n" +
                "uniform vec4 color;\n" +
                "uniform float radius;\n" +
                "\n" +
                "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                "    return length(max(abs(p) - b, 0.0)) - r;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 rectHalf = rectSize * .5;\n" +
                "    float distance = roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1., radius);\n" +
                "    \n" +
                "    // Create bloom/glow effect\n" +
                "    float glowDistance = distance + 8.0;\n" +
                "    float glow = 1.0 - smoothstep(0.0, 15.0, glowDistance);\n" +
                "    glow = pow(glow, 2.0);\n" +
                "    \n" +
                "    // Main shape\n" +
                "    float smoothedAlpha = (1.0 - smoothstep(-1.0, 2.0, distance)) * color.a;\n" +
                "    \n" +
                "    // Combine main shape with glow\n" +
                "    float finalAlpha = max(smoothedAlpha, glow * color.a * 0.3);\n" +
                "    \n" +
                "    // Darken edges slightly for depth\n" +
                "    vec3 finalColor = color.rgb;\n" +
                "    if (distance > 0.0) {\n" +
                "        float edgeDarken = smoothstep(0.0, 5.0, distance);\n" +
                "        finalColor = mix(finalColor, finalColor * 0.6, edgeDarken * 0.4);\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, finalAlpha);\n" +
                "}";

        bloomShader = new RoundedShader(bloomFragmentShader, true);

        try {
            InputStream inputStream = getClass().getResourceAsStream("/assets/minecraft/simp/logo.png");
            if (inputStream != null) {
                BufferedImage image = ImageIO.read(inputStream);
                DynamicTexture dynamicTexture = new DynamicTexture(image);
                clientLogo = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("simplogo", dynamicTexture);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum Type {
        Simple,
        Logo,
        Exhibition,
        GameSense,
        DynamicIsland
    }

    public enum FontType {
        Arial("Arial"),
        Apple("Apple"),
        Sans("Sans"),
        Simp("Simp"),
        SimpBold("Simp-Bold"),
        Minecraft("Minecraft");

        private final String name;

        FontType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        ScaledResolution sr = new ScaledResolution(mc);
        SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        String text = "Simp";

        if (type.getValue() == Type.DynamicIsland) {
            renderDynamicIsland(sr);
            return;
        }

        if (type.getValue() == Type.Exhibition) {
            text = "S" + EnumChatFormatting.GRAY + "imp ";
            if (info.getValue())
                text = "S" + EnumChatFormatting.GRAY + "imp " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
        } else if (type.getValue() == Type.Simple) {
            if (info.getValue()) {
                text = "Simp" + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
            } else {
                text = "Simp";
            }
        } else if (type.getValue() == Type.GameSense) {
            String serverInfo = (mc.getCurrentServerData() != null) ? mc.getCurrentServerData().serverIP : "Singleplayer";
            text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                    Simp.NAME, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
            int textWidth = useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft ?
                    getCustomFontRenderer().getStringWidth(text) : mc.fontRendererObj.getStringWidth(text);
            RenderUtils.drawBorderedRect(0, 0.5f, textWidth + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
        }

        if (useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft) {
            CustomFontRenderer fr = getCustomFontRenderer();
            fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
        }
    };

    private CustomFontRenderer getCustomFontRenderer() {
        switch (fontType.getValue()) {
            case Arial:
                return createFontRenderer("arial", 18, Font.PLAIN);
            case Apple:
                return createFontRenderer("apple", 18, Font.PLAIN);
            case Sans:
                return createFontRenderer("sans", 18, Font.PLAIN);
            case SimpBold:
                return createFontRenderer("simp", 18, Font.BOLD);
            case Simp:
            default:
                return createFontRenderer("simp", 18, Font.PLAIN);
        }
    }

    private CustomFontRenderer createFontRenderer(String fontName, float size, int style) {
        try {
            return new CustomFontRenderer(fontName, size, style, true, true);
        } catch (Exception e) {
            System.err.println("Failed to load font: " + fontName + ", using fallback");
            return new CustomFontRenderer("simp", size, Font.PLAIN, true, true);
        }
    }

    private void renderDynamicIsland(ScaledResolution sr) {
        if (System.currentTimeMillis() - lastServerTime > 5000) {
            updateServerInfo();
            lastServerTime = System.currentTimeMillis();
        }

        int centerX = sr.getScaledWidth() / 2;
        int yPos = 5;

        String clientText = "Simp";
        String serverText = currentServer != null ? currentServer : "Loading...";

        int clientWidth, smallTextWidth, fontHeight;
        if (useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft) {
            CustomFontRenderer fr = getCustomFontRenderer();
            clientWidth = fr.getStringWidth(clientText);
            smallTextWidth = fr.getStringWidth(" | " + serverText + " | " + Simp.VERSION);
            fontHeight = fr.getHeight();
        } else {
            FontRenderer fr = mc.fontRendererObj;
            clientWidth = fr.getStringWidth(clientText);
            smallTextWidth = fr.getStringWidth(" | " + serverText + " | " + Simp.VERSION);
            fontHeight = fr.FONT_HEIGHT;
        }

        int totalWidth = clientWidth + smallTextWidth + 29;
        int height = 24;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        float smoothTime = 0.15f;
        float alpha = 1.0f - (float)Math.exp(-deltaTime / smoothTime);

        islandHeight = RenderUtils.lerp(islandHeight, height, alpha);
        islandWidth = RenderUtils.lerp(islandWidth, totalWidth, alpha);

        int startX = (int) (centerX - islandWidth / 2);

        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0.0f);

        bloomShader.init();
        setupShaderUniforms(bloomShader, centerX - islandWidth / 2, yPos, islandWidth, islandHeight, 10f);
        bloomShader.setUniformf("color", 30f/255f, 35f/255f, 40f/255f, 0.85f);

        RoundedShader.drawQuads(centerX - islandWidth / 2 - 1, yPos - 1, islandWidth + 2, islandHeight + 2);
        bloomShader.unload();
        GlStateManager.disableBlend();

        GL11.glColor4f(1, 1, 1, 1);

        int logoSize = 256;
        int logoX = startX + 5;
        int logoY = yPos + (height - logoSize)/2;
        if (clientLogo != null) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.getTextureManager().bindTexture(clientLogo);
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.1, 0.1, 0.1);
            float newLogoX = (float) (logoX / 0.1 - 55);
            float newLogoY = (float) (logoY / 1.25 + 130);
            drawModalRectWithCustomSizedTexture((int) newLogoX, (int) newLogoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
        }

        int textX = logoX + 20;
        int textY = yPos + (height - fontHeight) / 2 + 1;

        int clientColor = ColorProcess.getColor().getRGB();

        if (useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft) {
            CustomFontRenderer fr = getCustomFontRenderer();
            fr.drawString(clientText, textX, textY, clientColor);
            fr.drawString(
                    " | " + serverText + " | " + Simp.VERSION,
                    textX + clientWidth,
                    textY,
                    0xAAAAAA
            );
        } else {
            FontRenderer fr = mc.fontRendererObj;
            fr.drawString(clientText, textX, textY, clientColor);
            fr.drawString(
                    " | " + serverText + " | " + Simp.VERSION,
                    textX + clientWidth,
                    textY,
                    0xAAAAAA
            );
        }

        renderScaffoldIndicator(sr, centerX, yPos, height, alpha);
    }

    private void renderScaffoldIndicator(ScaledResolution sr, int centerX, int yPos, int mainHeight, float alpha) {
        Module scaffoldModule = getScaffoldModule();
        boolean scaffoldActive = scaffoldModule != null && scaffoldModule.isEnabled();

        float targetAlpha = scaffoldActive ? 1.0f : 0.0f;
        scaffoldBoxAlpha = RenderUtils.lerp(scaffoldBoxAlpha, targetAlpha, alpha * 2);

        float targetOffset = scaffoldActive ? 35f : 0f;
        scaffoldBoxOffset = RenderUtils.lerp(scaffoldBoxOffset, targetOffset, alpha * 2);

        if (scaffoldBoxAlpha < 0.01f) return;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return;

        currentBlockCount = getBlockCount(heldItem);

        if (!scaffoldWasActive && scaffoldActive) {
            lastBlockCount = currentBlockCount;
        }
        scaffoldWasActive = scaffoldActive;

        int boxWidth = 90;
        int boxHeight = 24;
        int boxX = (int) (centerX - boxWidth / 2);
        int boxY = (int) (yPos + mainHeight + scaffoldBoxOffset);

        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        bloomShader.init();
        setupShaderUniforms(bloomShader, boxX, boxY, boxWidth, boxHeight, 8f);
        bloomShader.setUniformf("color", 30f/255f, 35f/255f, 40f/255f, 0.85f * scaffoldBoxAlpha);
        RoundedShader.drawQuads(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2);
        bloomShader.unload();

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        int itemX = boxX + 4;
        int itemY = boxY + 4;
        mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, itemX, itemY);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        float progress = lastBlockCount > 0 ? (float) currentBlockCount / (float) lastBlockCount : 1.0f;
        progress = Math.max(0, Math.min(1, progress));

        int barX = itemX + 20;
        int barY = boxY + 8;
        int barWidth = 45;
        int barHeight = 3;

        drawRoundedRect(barX, barY, barWidth, barHeight, 1.5f, new Color(20, 20, 25, (int)(180 * scaffoldBoxAlpha)).getRGB());

        if (progress > 0) {
            int filledWidth = (int) (barWidth * progress);
            drawGradientProgressBar(barX, barY, filledWidth, barHeight, 1.5f, scaffoldBoxAlpha);
        }

        String countText = String.valueOf(currentBlockCount);
        int textX = barX + barWidth + 4;
        int textY = boxY + (boxHeight - getCurrentFontHeight()) / 2 + 1;

        if (useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft) {
            getCustomFontRenderer().drawString(countText, textX, textY, new Color(170, 170, 170, (int)(255 * scaffoldBoxAlpha)).getRGB());
        } else {
            mc.fontRendererObj.drawString(countText, textX, textY, new Color(170, 170, 170, (int)(255 * scaffoldBoxAlpha)).getRGB());
        }

        GlStateManager.disableBlend();
    }

    private int getCurrentFontHeight() {
        if (useCustomFont.getValue() && fontType.getValue() != FontType.Minecraft) {
            return getCustomFontRenderer().getHeight();
        } else {
            return mc.fontRendererObj.FONT_HEIGHT;
        }
    }

    private Module getScaffoldModule() {
        try {
            java.lang.reflect.Method getInstanceMethod = Simp.class.getMethod("getInstance");
            Simp instance = (Simp) getInstanceMethod.invoke(null);
            java.lang.reflect.Method getModuleManagerMethod = instance.getClass().getMethod("getModuleManager");
            Object moduleManager = getModuleManagerMethod.invoke(instance);
            java.lang.reflect.Method getModuleMethod = moduleManager.getClass().getMethod("getModule", String.class);
            return (Module) getModuleMethod.invoke(moduleManager, "Scaffold");
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getModuleMethod = Simp.class.getMethod("getModule", String.class);
                return (Module) getModuleMethod.invoke(null, "Scaffold");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private int getBlockCount(ItemStack stack) {
        if (stack == null) return 0;

        int count = 0;
        for (int i = 0; i < mc.thePlayer.inventory.getSizeInventory(); i++) {
            ItemStack invStack = mc.thePlayer.inventory.getStackInSlot(i);
            if (invStack != null && invStack.getItem() == stack.getItem() &&
                    invStack.getMetadata() == stack.getMetadata()) {
                count += invStack.stackSize;
            }
        }
        return count;
    }

    private void drawGradientProgressBar(int x, int y, int width, int height, float radius, float alpha) {
        Color color1 = new Color(58, 134, 255, (int)(255 * alpha));
        Color color2 = new Color(0, 100, 255, (int)(255 * alpha));

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float r1 = color1.getRed() / 255f;
        float g1 = color1.getGreen() / 255f;
        float b1 = color1.getBlue() / 255f;
        float a1 = color1.getAlpha() / 255f;

        float r2 = color2.getRed() / 255f;
        float g2 = color2.getGreen() / 255f;
        float b2 = color2.getBlue() / 255f;
        float a2 = color2.getAlpha() / 255f;

        worldrenderer.pos(x, y + height, 0).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x + width, y + height, 0).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x + width, y, 0).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawRoundedRect(int x, int y, int width, int height, float radius, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        Gui.drawRect(x, y, x + width, y + height, color);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void setupShaderUniforms(RoundedShader shader, float x, float y, float width, float height, float radius) {
        ScaledResolution sr = new ScaledResolution(mc);
        shader.setUniformf("location", x * sr.getScaleFactor(),
                (mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        shader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        shader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    private void updateServerInfo() {
        currentServer = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }

    private void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double)x, (double)(y + height), 0.0D).tex((double)(u * f), (double)((v + (float)height) * f1)).endVertex();
        worldrenderer.pos((double)(x + width), (double)(y + height), 0.0D).tex((double)((u + (float)width) * f), (double)((v + (float)height) * f1)).endVertex();
        worldrenderer.pos((double)(x + width), (double)y, 0.0D).tex((double)((u + (float)width) * f), (double)(v * f1)).endVertex();
        worldrenderer.pos((double)x, (double)y, 0.0D).tex((double)(u * f), (double)(v * f1)).endVertex();
        tessellator.draw();
    }
}