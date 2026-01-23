package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "FPS Enhancer", category = ModuleCategory.CLIENT)
public final class FpsEnhancerModule extends Module {

    public static final Property<Boolean> reduceParticles = new Property<>("Reduce Particles", true);
    public static final Property<Boolean> disableConfigAutoSave = new Property<>("Disable Config Auto Save", false);
    private static final Property<Boolean> adaptiveRenderDistance = new Property<>("Adaptive Render Distance", false);

    public FpsEnhancerModule() {
        this.toggle();
    }

    @EventLink
    public final Listener<TickEvent> tickEventListener = event -> {
        if (mc.thePlayer.ticksExisted % 600 == 0) {
            System.gc();
            Runtime.getRuntime().gc();
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        if (adaptiveRenderDistance.getValue()) {
            EntityLivingBase entity = getFarthest(16 * 6);
            if (entity == null) {
                mc.gameSettings.renderDistanceChunks = 4;
            } else {
                mc.gameSettings.renderDistanceChunks = mc.thePlayer.getDistanceToEntity(entity) > 16 * 6 ? 6 : (int) (mc.thePlayer.getDistanceToEntity(entity) / 16);
            }
        }
    };

    private EntityLivingBase getFarthest(double range) {
        double dist = range;
        EntityLivingBase target = null;
        for (Object object : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) object;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase player = (EntityLivingBase) entity;
                double currentDist = mc.thePlayer.getDistanceToEntity(player);
                if (currentDist >= dist) {
                    dist = currentDist;
                    target = player;
                }

            }
        }
        return target;
    }

}
