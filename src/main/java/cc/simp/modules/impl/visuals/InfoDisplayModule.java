package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.ClickEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.DraggingProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.client.EvictingList;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Info Display", category = ModuleCategory.VISUALS)
public class InfoDisplayModule extends Module {

    // god, I love AI -lumie, 11/01/2025

    public enum DisplayMode {
        Draggable,
        Classic
    }

    private final ModeProperty<DisplayMode> mode = new ModeProperty<>("Mode", DisplayMode.Draggable);
    private final Property<Boolean> cps = new Property<>("CPS", true);
    private final Property<Boolean> fps = new Property<>("FPS", true);
    private final Property<Boolean> bps = new Property<>("BPS", true);
    private final Property<Boolean> version = new Property<>("Version", true, () -> mode.getValue() == DisplayMode.Classic);
    private final Property<Boolean> username = new Property<>("Username", true, () -> mode.getValue() == DisplayMode.Classic);

    private static final int BUBBLE_HEIGHT = 15;
    private static final int BUBBLE_PADDING = 2;
    private static final int RADIUS = 5;

    private static boolean positionInitialized = false;

    private int cpsValue = 0;
    private double bpsValue = 0.0;

    private final EvictingList<Boolean> clicks = new EvictingList<>(20);
    private boolean clicked;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        drawInfoBubbles();
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        drawInfoBubbles();
    };

    @EventLink
    public final Listener<ClickEvent> onClick = event -> {
        clicked = true;
    };

    @EventLink
    public final Listener<TickEvent> onPreMotionEvent = event -> {
        cpsValue = 0;
        clicks.add(clicked);
        clicks.forEach((click) -> {
            if (click) {
                cpsValue++;
            }
        });
        clicked = false;
    };

    private void initializePositions(ScaledResolution sr) {
        if (!positionInitialized) {
            int yOffset = 50;
            int xPos = 10;

            if (cps.getValue() && !DraggingProcess.components.containsKey("InfoDisplay_CPS")) {
                DraggingProcess.components.put("InfoDisplay_CPS",
                        new DraggingProcess.DraggableComponent(xPos, yOffset));
            }
            if (cps.getValue()) yOffset += BUBBLE_HEIGHT + 5;

            if (fps.getValue() && !DraggingProcess.components.containsKey("InfoDisplay_FPS")) {
                DraggingProcess.components.put("InfoDisplay_FPS",
                        new DraggingProcess.DraggableComponent(xPos, yOffset));
            }
            if (fps.getValue()) yOffset += BUBBLE_HEIGHT + 5;

            if (bps.getValue() && !DraggingProcess.components.containsKey("InfoDisplay_BPS")) {
                DraggingProcess.components.put("InfoDisplay_BPS",
                        new DraggingProcess.DraggableComponent(xPos, yOffset));
            }

            positionInitialized = true;
        }
    }

    private void drawInfoBubbles() {
        ScaledResolution sr = new ScaledResolution(mc);

        if (mc.thePlayer != null) {
            double deltaX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double deltaZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            bpsValue = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0;
        }

        if (mode.getValue() == DisplayMode.Classic) {
            drawClassic(sr);
        } else {
            initializePositions(sr);
            boolean isInChat = mc.currentScreen instanceof GuiChat;

            if (cps.getValue()) {
                drawBubble("CPS", String.valueOf(cpsValue), "InfoDisplay_CPS", isInChat);
            }

            if (fps.getValue()) {
                int fpsValue = mc.getDebugFPS();
                drawBubble("FPS", String.valueOf(fpsValue), "InfoDisplay_FPS", isInChat);
            }

            if (bps.getValue()) {
                drawBubble("BPS", String.format("%.2f", bpsValue), "InfoDisplay_BPS", isInChat);
            }
        }
    }

    private void drawClassic(ScaledResolution sr) {
        int yOffset = sr.getScaledHeight() - 2;
        int xPos = 2;
        int lineHeight = FontProcess.getCurrentFont().getHeight() + 2;

        List<String> lines = new ArrayList<>();

        if (bps.getValue()) {
            lines.add(String.format("BPS: %.2f", bpsValue));
        }

        if (fps.getValue()) {
            int fpsValue = mc.getDebugFPS();
            lines.add("FPS: " + fpsValue);
        }

        if (cps.getValue()) {
            lines.add("CPS: " + cpsValue);
        }

        if (version.getValue()) {
            lines.add("Version: " + Simp.VERSION);
        }

        if (username.getValue()) {
            lines.add("Version: " + mc.getSession().getUsername());
        }

        // Draw from bottom to top
        for (int i = lines.size() - 1; i >= 0; i--) {
            FontProcess.getCurrentFont().drawStringWithShadow(lines.get(i), xPos, yOffset - (lines.size() - i) * lineHeight, Color.WHITE.getRGB());
        }
    }

    private void drawBubble(String label, String value, String componentKey, boolean isInChat) {
        DraggingProcess.DraggableComponent component = DraggingProcess.components.get(componentKey);
        if (component == null) return;

        String displayText = label + ": " + value;
        int textWidth = FontProcess.getCurrentFont().getStringWidth(displayText);
        int bubbleWidth = textWidth + BUBBLE_PADDING * 2;

        component.setWidth(bubbleWidth);
        component.setHeight(BUBBLE_HEIGHT);

        GlStateManager.pushMatrix();
        GlStateManager.translate(component.getX(), component.getY(), 0);

        Color bgColor = new Color(ColorProcess.getColor().darker().getRed(),
                ColorProcess.getColor().darker().getGreen(),
                ColorProcess.getColor().darker().getBlue(),
                isInChat ? 60 : 100);
        RenderUtils.drawRoundedRect(0, 0, bubbleWidth, BUBBLE_HEIGHT, RADIUS, bgColor);

        int textY = (BUBBLE_HEIGHT - FontProcess.getCurrentFont().getHeight()) / 2;
        FontProcess.getCurrentFont().drawStringWithShadow(displayText, BUBBLE_PADDING, textY, Color.WHITE.getRGB());

        GlStateManager.popMatrix();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        positionInitialized = false;
    }
}
