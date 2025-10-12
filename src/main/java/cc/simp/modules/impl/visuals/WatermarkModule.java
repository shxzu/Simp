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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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

    private long lastServerTime;
    private long lastUpdateTime;
    private String currentServer;
    private float islandWidth;
    private float islandHeight;
    private ResourceLocation clientLogo;
    private RoundedShader bloomShader;

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
        SimpBold("Simp-Bold");

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
        CustomFontRenderer fr = getSelectedFont();
        ScaledResolution sr = new ScaledResolution(mc);
        SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        String text = "Simp";

        if (type.getValue() == Type.DynamicIsland) {
            renderDynamicIsland(fr, sr);
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
            RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
        }
        fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
    };

    private CustomFontRenderer getSelectedFont() {
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

    private void renderDynamicIsland(CustomFontRenderer fr, ScaledResolution sr) {
        if (System.currentTimeMillis() - lastServerTime > 5000) {
            updateServerInfo();
            lastServerTime = System.currentTimeMillis();
        }

        int centerX = sr.getScaledWidth() / 2;
        int yPos = 5;

        String clientText = "Simp";
        String serverText = currentServer != null ? currentServer : "Loading...";

        int clientWidth = fr.getStringWidth(clientText);
        int smallTextWidth = fr.getStringWidth(" | " + serverText + " | " + Simp.VERSION);

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
        int textY = yPos + (height - fr.getHeight())/2 + 1;

        int clientColor = ColorProcess.getColor().getRGB();
        fr.drawString(clientText, textX, textY, clientColor);
        fr.drawString(
                " | " + serverText + " | " + Simp.VERSION,
                textX + fr.getStringWidth(clientText),
                textY,
                0xAAAAAA
        );
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