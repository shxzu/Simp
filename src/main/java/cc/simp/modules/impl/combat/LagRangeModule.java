package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.FontProcess;
import cc.simp.processes.LagProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Lag Range", category = ModuleCategory.COMBAT)
public final class LagRangeModule extends Module {

    public static NumberProperty rangeProperty = new NumberProperty("Range", 3.5, 0.1, 6, 0.05);
    public static NumberProperty minDelayProperty = new NumberProperty("Min Delay", 50.0, 0.0, 5000.0, 10.0);
    public static NumberProperty maxDelayProperty = new NumberProperty("Max Delay", 200.0, 0.0, 5000.0, 10.0);
    public static final Property<Boolean> displayProperty = new Property<>("Display", true);
    public static final Property<Boolean> onlyWithKillaura = new Property<>("Only With Killaura", true);

    private boolean lagging = false;
    private int lagAmount = 69;

    @EventLink
    public Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (lagging) {
            disableLag();
        }
    };

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) return;

        if (onlyWithKillaura.getValue() && !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target == null) {
            if (lagging) disableLag();
            return;
        }

        if (lagging) lagAmount = (int) MathUtils.getRandom(minDelayProperty.getValue().intValue(), maxDelayProperty.getValue().intValue());

        if (!lagging) lagAmount = 69;

        setSuffix(String.format("%.1f | %dms", rangeProperty.getValue(), lagAmount));

        if (TargetSelectionProcess.getTarget() == null) {
            if (lagging) disableLag();
            return;
        }

        if (!MovementUtils.isMoving()) {
            if (lagging) disableLag();
            return;
        }

        double distance = mc.thePlayer.getDistanceToEntity(TargetSelectionProcess.getTarget());

        if (distance <= KillAuraModule.seekRange.getValue() && distance > rangeProperty.getValue()) {
            if (!lagging) {
                enableLag();
            }
        } else {
            if (lagging) {
                disableLag();
            }
        }
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = event -> {
        if (!displayProperty.getValue() || !lagging) return;

        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        String text = String.format("Lagging %dms", lagAmount);
        fr.drawStringWithShadow(text,
                (float) sr.getScaledWidth() / 2 - (float) fr.getStringWidth(text) / 2,
                sr.getScaledHeight() / 10f - fr.FONT_HEIGHT,
                Color.YELLOW.getRGB());
    };

    private void enableLag() {
        LagProcess.spoof(lagAmount, true, true, false, true);
        lagging = true;
    }

    private void disableLag() {
        LagProcess.dispatch();
        LagProcess.disable();
        lagging = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (lagging) {
            disableLag();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }
}
