package cc.simp.modules.impl.combat;


import cc.simp.Simp;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.processes.LagProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.render.ESPUtils;
import cc.simp.utils.render.GlUtils;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Blink Range", category = ModuleCategory.COMBAT)
public final class BlinkRangeModule extends Module {

    public static ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Range);
    public static NumberProperty rangeToUnblinkProperty = new NumberProperty("Range To Reset Blink", 2, () -> modeProperty.getValue() == Mode.Range, 0.1, 6, 0.05);
    public static final Property<Boolean> displayProperty = new Property<>("Display", true);
    public static final Property<Boolean> onlyWithKillaura = new Property<>("Only With Killaura", true);
    public static final Property<Boolean> renderBlinkPos = new Property<>("Render Blink Pos", true);
    public static ModeProperty<RenderMode> renderModeProperty = new ModeProperty<>("Render Mode", RenderMode.Box, () -> renderBlinkPos.getValue());

    private enum RenderMode {
        Box,
        Player
    }


    private Timer timer = new Timer();
    private boolean blinked = false;
    private double blinkedX, blinkedY, blinkedZ;


    private enum Mode {
        Range,
        Time
    }

    @EventLink
    public Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (blinked) {
            blinkToggle(false);
            blinked = false;
        }
    };

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) return;

        if (onlyWithKillaura.getValue() && !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target == null) {
            if (blinked) blinkToggle(false);
            return;
        }

        setSuffix(modeProperty.getValue() == Mode.Range ? String.valueOf(rangeToUnblinkProperty.getValue().intValue()) : String.valueOf(timer.getTime()));

        if (modeProperty.getValue() == Mode.Time) {
            if (timer.hasTimeElapsed(500L) && !blinked) {
                blinkToggle(true);
                blinked = true;
                timer.reset();
            }
            if (timer.hasTimeElapsed(500L) && blinked) {
                blinkToggle(false);
                blinked = false;
                timer.reset();
            }
        }

        if (modeProperty.getValue() == Mode.Range) {
            if (TargetSelectionProcess.getTarget() == null) {
                if (blinked) blinkToggle(false);
                blinked = false;
                return;
            }
            if (!MovementUtils.isMoving()) {
                if (blinked) blinkToggle(false);
                blinked = false;
                return;
            }

            double distance = mc.thePlayer.getDistanceToEntity(TargetSelectionProcess.getTarget());

            if (distance <= KillAuraModule.seekRange.getValue() && distance > rangeToUnblinkProperty.getValue()) {
                if (!blinked) {
                    blinkedX = mc.thePlayer.posX;
                    blinkedY = mc.thePlayer.posY;
                    blinkedZ = mc.thePlayer.posZ;
                    blinkToggle(true);
                    blinked = true;
                }
            } else if (distance <= rangeToUnblinkProperty.getValue()) {
                if (blinked) {
                    blinkToggle(false);
                    blinked = false;
                }
            }
        }
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = event -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        if (blinked && displayProperty.getValue()) {
            fr.drawStringWithShadow("Blinking..", (float) sr.getScaledWidth() / 2 - (float) fr.getStringWidth("Blinking..") / 2, sr.getScaledHeight() / 10f - fr.FONT_HEIGHT, Color.YELLOW.getRGB());
        }
    };

    @EventLink
    public Listener<Render3DEvent> render3DEventListener = event -> {
        if (!blinked || !renderBlinkPos.getValue()) return;

        double x = blinkedX - mc.getRenderManager().viewerPosX;
        double y = blinkedY - mc.getRenderManager().viewerPosY;
        double z = blinkedZ - mc.getRenderManager().viewerPosZ;

        if (renderModeProperty.getValue() == RenderMode.Box) {
            AxisAlignedBB bb = new AxisAlignedBB(
                    x - 0.3, y, z - 0.3,
                    x + 0.3, y + 1.8, z + 0.3
            );

            Color color = ColorProcess.getColor();
            RenderUtils.start3D();
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f,
                    color.getBlue() / 255f, color.getAlpha() / 255f);
            RenderUtils.drawBoundingBox(bb);
            RenderUtils.stop3D();
            GlUtils.resetColor();
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);

            mc.getRenderManager().renderEntityStatic(mc.thePlayer, mc.timer.renderPartialTicks, false);

            GlStateManager.popMatrix();
        }
    };


    public void blinkToggle(boolean toggle) {
        if (toggle) {
            LagProcess.blink();
        } else {
            LagProcess.dispatch();
            LagProcess.disable();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (blinked) {
            blinkToggle(false);
            blinked = false;
        }
        timer.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
    }

}