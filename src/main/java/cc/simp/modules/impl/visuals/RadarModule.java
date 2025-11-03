package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.DraggingProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Radar", category = ModuleCategory.VISUALS)
public class RadarModule extends Module {

    private final NumberProperty scale = new NumberProperty("Scale", 2.0, 0.1, 5.0, 0.1);
    private final NumberProperty size = new NumberProperty("Size", 125, 50, 500, 5);

    private static boolean positionInitialized = false;
    private static final int DEFAULT_SIZE = 125;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        renderRadar();
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        renderRadar();
    };

    private void initializePosition(ScaledResolution sr) {
        if (!positionInitialized && !DraggingProcess.components.containsKey("Radar")) {
            DraggingProcess.components.put("Radar",
                    new DraggingProcess.DraggableComponent(
                            125,
                            sr.getScaledHeight() / 2.0 - DEFAULT_SIZE / 2.0
                    )
            );
            positionInitialized = true;
        }
    }

    private void renderRadar() {
        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("Radar");

        int radarSize = size.getValue().intValue();
        draggableComponent.setWidth(radarSize);
        draggableComponent.setHeight(radarSize);

        float x = (float) draggableComponent.getX();
        float y = (float) draggableComponent.getY();

        float pTicks = mc.timer.renderPartialTicks;
        double playerOffsetX = mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * pTicks;
        double playerOffsetZ = mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * pTicks;

        // Colors
        Color darkest = new Color(10, 10, 10, 180);
        Color secondDarkest = new Color(22, 22, 22, 180);
        Color lightest = new Color(44, 44, 44, 180);
        Color middleColor = new Color(34, 34, 34, 180);
        Color accentColor = ColorProcess.getColor();

        // Draw outer borders
        Gui.drawRect((int) (x - 3.5), (int) (y - 3.5), (int) (x + radarSize + 3.5), (int) (y + radarSize + 3.5), darkest.getRGB());
        Gui.drawRect((int) (x - 3), (int) (y - 3), (int) (x + radarSize + 3), (int) (y + radarSize + 3), middleColor.getRGB());
        Gui.drawRect((int) (x - 1), (int) (y - 1), (int) (x + radarSize + 1), (int) (y + radarSize + 1), lightest.getRGB());
        Gui.drawRect((int) x, (int) y, (int) (x + radarSize), (int) (y + radarSize), secondDarkest.getRGB());

        // Draw inner border
        Gui.drawRect((int) (x + 2.5), (int) (y + 2.5), (int) (x + radarSize - 2.5), (int) (y + radarSize - 2.5), lightest.getRGB());

        // Draw background
        Gui.drawRect((int) (x + 3), (int) (y + 3.5), (int) (x + radarSize - 3), (int) (y + radarSize - 3.5), new Color(15, 20, 35, 200).getRGB());

        // Draw crosshair
        Gui.drawRect((int) (x + radarSize / 2f - 0.5), (int) (y + 3.5), (int) (x + radarSize / 2f + 0.5), (int) (y + radarSize - 3.5), new Color(255, 255, 255, 80).getRGB());
        Gui.drawRect((int) (x + 3.5), (int) (y + radarSize / 2f - 0.5), (int) (x + radarSize - 3.5), (int) (y + radarSize / 2f + 0.5), new Color(255, 255, 255, 80).getRGB());

        // Draw top accent bar
        Gui.drawRect((int) (x + 3.5), (int) (y + 3.5), (int) (x + radarSize - 3.5), (int) (y + 4.5), accentColor.getRGB());
        Gui.drawRect((int) (x + 3.5), (int) (y + 4), (int) (x + radarSize - 3.5), (int) (y + 4.5), new Color(0, 0, 0, 110).getRGB());

        // Render entities
        GlStateManager.pushMatrix();
        for (Object o : mc.theWorld.getLoadedEntityList()) {
            if (o instanceof EntityPlayer) {
                EntityPlayer entity = (EntityPlayer) o;

                if (entity.isEntityAlive() && entity != mc.thePlayer && !entity.isInvisible() && !entity.isInvisibleToPlayer(mc.thePlayer)) {
                    // Calculate entity position relative to player
                    float posX = (float) (((entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pTicks) - playerOffsetX) * scale.getValue());
                    float posZ = (float) (((entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pTicks) - playerOffsetZ) * scale.getValue());

                    // Determine color based on visibility
                    Color entityColor = mc.thePlayer.canEntityBeSeen(entity)
                            ? new Color(255, 50, 50)
                            : new Color(255, 50, 50, 150);

                    // Rotate based on player's yaw
                    float cos = (float) Math.cos(mc.thePlayer.rotationYaw * (Math.PI * 2 / 360));
                    float sin = (float) Math.sin(mc.thePlayer.rotationYaw * (Math.PI * 2 / 360));
                    float rotY = -(posZ * cos - posX * sin);
                    float rotX = -(posX * cos + posZ * sin);

                    // Clamp to radar edges
                    float maxDistance = radarSize / 2f - 5;
                    rotY = MathHelper.clamp_float(rotY, -maxDistance, maxDistance);
                    rotX = MathHelper.clamp_float(rotX, -maxDistance, maxDistance);

                    // Draw entity marker
                    float markerX = x + (radarSize / 2f) + rotX;
                    float markerY = y + (radarSize / 2f) + rotY;

                    Gui.drawRect((int) (markerX - 1.5), (int) (markerY - 1.5), (int) (markerX + 1.5), (int) (markerY + 1.5), entityColor.getRGB());
                    Gui.drawRect((int) (markerX - 1), (int) (markerY - 1), (int) (markerX + 1), (int) (markerY + 1), new Color(0, 0, 0, 180).getRGB());
                }
            }
        }
        GlStateManager.popMatrix();

        // Draw local player marker (center)
        Gui.drawRect((int) (x + radarSize / 2f - 2), (int) (y + radarSize / 2f - 2), (int) (x + radarSize / 2f + 2), (int) (y + radarSize / 2f + 2), new Color(0, 255, 0).getRGB());
        Gui.drawRect((int) (x + radarSize / 2f - 1.5), (int) (y + radarSize / 2f - 1.5), (int) (x + radarSize / 2f + 1.5), (int) (y + radarSize / 2f + 1.5), new Color(0, 0, 0, 180).getRGB());
    }
}
