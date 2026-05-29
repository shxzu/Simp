package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.processes.LagProcess;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.GlUtils;
import cc.simp.api.properties.impl.ModeProperty;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;

import static cc.simp.utils.Util.mc;

import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import java.awt.*;

@ModuleInfo(label = "Fake Lag", category = ModuleCategory.CLIENT)
public class FakeLagModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 200, 50, 2000, 5);
    private final Property<Boolean> teleports = new Property<>("Delay Teleports", true);
    private final Property<Boolean> velocity = new Property<>("Delay Velocity", true);
    private final Property<Boolean> entities = new Property<>("Delay Entity Movements", true);
    private final Property<Boolean> renderLagPos = new Property<>("Render Lag Pos", true);

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) return;
        LagProcess.spoof(delay.getValue().intValue(), true, velocity.getValue(),
                teleports.getValue(), entities.getValue());
    };

    @EventLink
    public Listener<Render3DEvent> render3DEventListener = event -> {
        if (!renderLagPos.getValue()) return;
        if (!LagProcess.hasServerPosition) return;
        if (mc.gameSettings.thirdPersonView == 0) return;

        // Render the chosen visualization at the last position known to be sent to the server
        double x = LagProcess.serverX - mc.getRenderManager().viewerPosX;
        double y = LagProcess.serverY - mc.getRenderManager().viewerPosY;
        double z = LagProcess.serverZ - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);

        RenderUtils.start3D();
        GlStateManager.pushMatrix();
        Color color = ColorProcess.getColor();
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        RenderUtils.drawBoundingBox(bb);
        GlStateManager.popMatrix();
        RenderUtils.stop3D();
        GlUtils.resetColor();
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = event -> {
        if (!renderLagPos.getValue()) return;
        if (!LagProcess.hasServerPosition) return;

    };
}
