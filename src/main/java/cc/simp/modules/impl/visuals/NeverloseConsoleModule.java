package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.DragUtils;
import cc.simp.utils.render.FontUtils;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Neverlose Console", category = ModuleCategory.VISUALS)
public final class NeverloseConsoleModule extends Module {

    // ── Properties ──────────────────────────────────────────────────────────
    public static NumberProperty maxLines    = new NumberProperty("Max Lines",    20, 5,  50, 1);
    public static NumberProperty maxAge      = new NumberProperty("Max Age (s)",  12, 2,  60, 1);
    public static Property<Boolean> showSubmit = new Property<>("Show Submit", true);

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int    HUD_WIDTH       = 260;
    private static final int    TITLE_HEIGHT    = 16;
    private static final int    LINE_HEIGHT     = 10;
    private static final int    PADDING         = 5;
    private static final int    SCROLLBAR_WIDTH = 6;
    private static final int    SUBMIT_HEIGHT   = 18;
    private static final String COMPONENT_KEY   = "Console";

    // ── State ────────────────────────────────────────────────────────────────
    private static boolean positionInitialized = false;

    /** A single log entry */
    public static class LogEntry {
        public final String  text;
        public final Color   color;
        public final long    timestamp;

        public LogEntry(String text, Color color) {
            this.text      = text;
            this.color     = color;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Static so other modules can push entries via ConsoleModule.log(...)
    private static final List<LogEntry> entries = new ArrayList<>();
    private static int scrollOffset = 0;   // lines scrolled up from bottom

    /**
     * Push a new entry into the console.
     * Call this from combat event handlers, etc.
     */
    public static synchronized void log(String text, Color color) {
        entries.add(new LogEntry(text, color));
    }

    // ── Color palette (matches screenshot) ──────────────────────────────────
    private static final Color COL_BG_TITLE  = new Color(30,  30,  30,  230);
    private static final Color COL_BG_BODY   = new Color(20,  20,  20,  210);
    private static final Color COL_SCROLLBAR = new Color(80,  80,  80,  200);
    private static final Color COL_SUBMIT_BG = new Color(50,  50,  50,  220);
    private static final Color COL_WHITE      = Color.WHITE;
    private static final Color COL_GREY       = new Color(170, 170, 170);

    // ── Highlight colours (same as in the screenshot) ────────────────────────
    public static final Color COL_HIT_MISS   = new Color(200, 200, 200); // default / plain
    public static final Color COL_PREDICTION = new Color(100, 180, 255); // blue  – prediction error
    public static final Color COL_SPREAD     = new Color(255, 100, 100); // red   – spread
    public static final Color COL_RESOLVER   = new Color(180, 255, 130); // green – resolver
    public static final Color COL_CONNECTION = new Color(255, 200,  80); // gold  – connection
    public static final Color COL_DAMAGE     = new Color(255, 255, 255); // white – hit/dmg line

    // ── Render events ────────────────────────────────────────────────────────
    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> drawConsole();

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> drawConsole();

    @EventLink
    public Listener<AttackEvent> attackEventListener = e -> {
        if (e.target == null) return;

        String victim = e.target.getName();
        logHit(victim, e.target.getHealth());
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> logPredictionError(victim);
            case 1 -> logSpread(victim);
            case 2 -> logResolver(victim);
            case 3 -> logConnection(victim);
        }
    };

    // ── Core draw ────────────────────────────────────────────────────────────
    private void drawConsole() {
        if (mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        pruneOldEntries();

        List<LogEntry> snapshot;
        synchronized (NeverloseConsoleModule.class) {
            snapshot = new ArrayList<>(entries);
        }

        int visibleLines = (int) maxLines.getValue().intValue();
        int submitBlockH = showSubmit.getValue() ? (SUBMIT_HEIGHT + PADDING) : 0;
        int bodyHeight   = visibleLines * LINE_HEIGHT + PADDING * 2;
        int totalHeight  = TITLE_HEIGHT + bodyHeight + submitBlockH;

        // ── Position init (bottom-right corner by default) ───────────────────
        if (!positionInitialized && !DragUtils.components.containsKey(COMPONENT_KEY)) {
            int defaultX = sr.getScaledWidth()  - HUD_WIDTH - 10;
            int defaultY = sr.getScaledHeight() - totalHeight - 10;
            DragUtils.components.put(COMPONENT_KEY,
                    new DragUtils.DraggableComponent(defaultX, defaultY));
            positionInitialized = true;
        }

        DragUtils.DraggableComponent drag = DragUtils.components.get(COMPONENT_KEY);
        if (drag == null) return;
        drag.setWidth(HUD_WIDTH);
        drag.setHeight(totalHeight);

        GlStateManager.pushMatrix();
        GlStateManager.translate(drag.getX(), drag.getY(), 0);

        // ── Title bar ────────────────────────────────────────────────────────
        Gui.drawRect(0, 0, HUD_WIDTH, TITLE_HEIGHT, COL_BG_TITLE.getRGB());
        // subtle bottom border on title
        Gui.drawRect(0, TITLE_HEIGHT - 1, HUD_WIDTH, TITLE_HEIGHT, new Color(60, 60, 60, 200).getRGB());

        String title = "Console";
        int titleW = FontUtils.getFont("mc").getStringWidth(title);
        FontUtils.getFont("mc").drawString(title, (float) (HUD_WIDTH - titleW) / 2, 4, COL_WHITE.getRGB());

        // Close "button" decoration  [×]
        FontUtils.getFont("mc").drawString("×", HUD_WIDTH - 10, 4, COL_GREY.getRGB());

        // ── Body ─────────────────────────────────────────────────────────────
        int bodyY = TITLE_HEIGHT;
        Gui.drawRect(0, bodyY, HUD_WIDTH, bodyY + bodyHeight, COL_BG_BODY.getRGB());

        // Clip-like rendering: show last `visibleLines` entries, accounting for scroll
        int totalEntries = snapshot.size();
        int startIdx = Math.max(0, totalEntries - visibleLines - scrollOffset);
        int endIdx   = Math.max(0, totalEntries - scrollOffset);

        int lineY = bodyY + PADDING;
        for (int i = startIdx; i < endIdx; i++) {
            LogEntry entry = snapshot.get(i);
            renderColoredLine(entry.text, entry.color, PADDING, lineY);
            lineY += LINE_HEIGHT;
        }

        // ── Scrollbar ────────────────────────────────────────────────────────
        if (totalEntries > visibleLines) {
            int sbX      = HUD_WIDTH - SCROLLBAR_WIDTH - 2;
            int sbTrackH = bodyHeight - PADDING;
            int sbH      = Math.max(12, (int) ((visibleLines / (float) totalEntries) * sbTrackH));
            int maxScroll = totalEntries - visibleLines;
            int sbY = bodyY + PADDING + (int) ((1f - (scrollOffset / (float) maxScroll)) * (sbTrackH - sbH));

            // track
            Gui.drawRect(sbX, bodyY + PADDING, sbX + SCROLLBAR_WIDTH, bodyY + PADDING + sbTrackH,
                    new Color(40, 40, 40, 180).getRGB());
            // thumb
            Gui.drawRect(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + sbH, COL_SCROLLBAR.getRGB());
        }

        // ── Submit bar ───────────────────────────────────────────────────────
        if (showSubmit.getValue()) {
            int sbY = bodyY + bodyHeight;
            Gui.drawRect(0, sbY, HUD_WIDTH, sbY + SUBMIT_HEIGHT, COL_SUBMIT_BG.getRGB());
            Gui.drawRect(0, sbY, HUD_WIDTH, sbY + 1, new Color(55, 55, 55).getRGB()); // top border

            // placeholder text
            FontUtils.getFont("mc").drawString("|", PADDING, sbY + 4, COL_GREY.getRGB());

            // Submit button
            int btnW = 38;
            int btnX = HUD_WIDTH - btnW - PADDING;
            Gui.drawRect(btnX, sbY + 3, btnX + btnW, sbY + SUBMIT_HEIGHT - 3,
                    new Color(60, 60, 60, 220).getRGB());
            Gui.drawRect(btnX, sbY + 3, btnX + btnW, sbY + 4,
                    new Color(90, 90, 90).getRGB());  // top highlight
            int submitTextW = FontUtils.getFont("mc").getStringWidth("Submit");
            FontUtils.getFont("mc").drawString("Submit",
                    btnX + (btnW - submitTextW) / 2, sbY + 5, COL_WHITE.getRGB());
        }

        RenderUtils.resetColor();
        GlStateManager.popMatrix();
    }

    /**
     * Renders a log line where the last "word" (reason/tag) is coloured and
     * the rest of the line is grey — matching the screenshot layout:
     *   "[astolfo.cc] missed hit due to <colored-reason>"
     */
    private void renderColoredLine(String text, Color highlight, int x, int y) {
        // Split at the last space to find the highlighted tail word
        int lastSpace = text.lastIndexOf(' ');
        if (lastSpace == -1 || highlight.equals(COL_DAMAGE)) {
            // No split – render whole line in the given colour
            FontUtils.getFont("mc").drawString(text, x, y, highlight.getRGB());
            return;
        }

        String prefix = text.substring(0, lastSpace + 1);
        String suffix = text.substring(lastSpace + 1);

        FontUtils.getFont("mc").drawString(prefix, x, y, COL_GREY.getRGB());
        int prefixW = FontUtils.getFont("mc").getStringWidth(prefix);
        FontUtils.getFont("mc").drawString(suffix, x + prefixW, y, highlight.getRGB());
    }

    /** Remove entries older than maxAge seconds */
    private synchronized void pruneOldEntries() {
        long cutoff = System.currentTimeMillis() - (long)(maxAge.getValue() * 1000);
        entries.removeIf(e -> e.timestamp < cutoff);
        // clamp scroll
        int max = Math.max(0, entries.size() - (int) maxLines.getValue().intValue());
        if (scrollOffset > max) scrollOffset = max;
    }

    /** Convenience wrappers matching the colour scheme in the screenshot */
    public static void logPredictionError(String victim) {
        log("[" + Simp.NAME + "] missed hit due to prediction error", COL_PREDICTION);
    }
    public static void logSpread(String victim) {
        log("[" + Simp.NAME + "] missed hit due to spread", COL_SPREAD);
    }
    public static void logResolver(String victim) {
        log("[" +Simp.NAME + "] missed hit due to resolver", COL_RESOLVER);
    }
    public static void logConnection(String victim) {
        log("[" + Simp.NAME + "] missed hit due to connection", COL_CONNECTION);
    }
    public static void logHit(String victim, float hlt) {
        log(String.format("[%s] swung at [%s] hlt [%.1f]",
                Simp.NAME, victim, hlt), COL_DAMAGE);
    }
}
