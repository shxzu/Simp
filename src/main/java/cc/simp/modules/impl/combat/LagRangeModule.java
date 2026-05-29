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
import cc.simp.utils.render.FontUtils;
import cc.simp.processes.LagProcess;
import cc.simp.processes.ColorProcess;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
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
    private final Property<Boolean> teleports = new Property<>("Delay Teleports", true);
    private final Property<Boolean> velocity = new Property<>("Delay Velocity", true);
    private final Property<Boolean> entities = new Property<>("Delay Entity Movements", true);
    public static final Property<Boolean> displayProperty = new Property<>("Display", true);
    public static final Property<Boolean> onlyWithKillaura = new Property<>("Only With Killaura", true);
    public static final Property<Boolean> renderLagPos = new Property<>("Render Lag Pos", true);

    private boolean lagging = false;
    private int lagAmount = maxDelayProperty.getValue().intValue();

    @EventLink
    public Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (lagging) {
            disableLag();
        }
    };

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) return;

        if (onlyWithKillaura.getValue() && !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled()) {
            if (lagging) disableLag();
            return;
        }

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
            lagAmount = (int) MathUtils.getRandom(minDelayProperty.getValue().intValue(), maxDelayProperty.getValue().intValue());
            LagProcess.spoof(lagAmount, true, velocity.getValue(),
                    teleports.getValue(), entities.getValue());
            lagging = true;
        } else {
            if (lagging) {
                disableLag();
            }
            return;
        }

        setSuffix(String.format("%.1f | %dms", rangeProperty.getValue(), lagAmount));
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = event -> {
        if (!displayProperty.getValue() || !lagging) return;

        CustomFontRenderer fr = FontUtils.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        String text = String.format("Lagging %dms", lagAmount);
        fr.drawStringWithShadow(text,
                (float) sr.getScaledWidth() / 2 - (float) fr.getStringWidth(text) / 2,
                sr.getScaledHeight() / 10f - fr.FONT_HEIGHT,
                Color.YELLOW.getRGB());
    };

    @EventLink
    public Listener<Render3DEvent> render3DEventListener = event -> {
        if (!renderLagPos.getValue() || !lagging) return;
        if (!LagProcess.hasServerPosition) return;
        if (mc.gameSettings.thirdPersonView == 0) return;

        double x = LagProcess.serverX - mc.getRenderManager().viewerPosX;
        double y = LagProcess.serverY - mc.getRenderManager().viewerPosY;
        double z = LagProcess.serverZ - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
        RenderUtils.start3D();
        GlStateManager.pushMatrix();
        GlStateManager.color(ColorProcess.getColor().getRed() / 255f, ColorProcess.getColor().getGreen() / 255f,
                ColorProcess.getColor().getBlue() / 255f, ColorProcess.getColor().getAlpha() / 255f);
        RenderUtils.drawBoundingBox(bb);
        GlStateManager.popMatrix();
        RenderUtils.stop3D();
    };

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
