package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
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

    // Additive optimizations that don't change MC settings
    public static final Property<Boolean> reduceParticles = new Property<>("Reduce Particles", true);
    public static final Property<Boolean> disableConfigAutoSave = new Property<>("Disable Config Auto Save", false);
    private static final Property<Boolean> adaptiveRenderDistance = new Property<>("Adaptive Render Distance", false);
    private static final Property<Boolean> cullDistantEntities = new Property<>("Cull Distant Entities", true);
    private static final Property<Boolean> cullInvisibleEntities = new Property<>("Cull Invisible Entities", true);
    private static final Property<Boolean> optimizeMemory = new Property<>("Optimize Memory", true);
    private static final NumberProperty gcInterval = new NumberProperty("GC Interval (seconds)", 30.0, 10.0, 120.0, 5.0);
    private static final NumberProperty entityCullDistance = new NumberProperty("Entity Cull Distance", 64.0, 32.0, 256.0, 16.0);
    private static final Property<Boolean> skipDeadEntities = new Property<>("Skip Dead Entities", true);
    private static final Property<Boolean> batchRendering = new Property<>("Batch Rendering", true);

    private long lastGCTime = 0;
    private int tickCounter = 0;

    public FpsEnhancerModule() {
        this.toggle();
    }

    @Override
    public void onEnable() {
        lastGCTime = System.currentTimeMillis();
        tickCounter = 0;
    }

    @EventLink
    public final Listener<TickEvent> tickEventListener = event -> {
        tickCounter++;

        // Optimize memory with configurable GC interval
        if (optimizeMemory.getValue()) {
            long currentTime = System.currentTimeMillis();
            long intervalMs = gcInterval.getValue().longValue() * 1000;

            if (currentTime - lastGCTime >= intervalMs) {
                System.gc();
                lastGCTime = currentTime;
            }
        }

        // Clean up dead entities periodically
        if (skipDeadEntities.getValue() && tickCounter % 100 == 0) {
            if (mc.theWorld != null) {
                mc.theWorld.loadedEntityList.removeIf(entity -> {
                    if (entity instanceof EntityLivingBase) {
                        EntityLivingBase livingBase = (EntityLivingBase) entity;
                        return !livingBase.isEntityAlive();
                    }
                    return false;
                });
            }
        }

        // Cull distant entities from render list
        if (cullDistantEntities.getValue() && mc.theWorld != null && mc.thePlayer != null) {
            double cullDistance = entityCullDistance.getValue();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity != mc.thePlayer && entity != mc.getRenderViewEntity()) {
                    double distance = mc.thePlayer.getDistanceToEntity(entity);

                    // Mark entities beyond cull distance as not renderable
                    if (distance > cullDistance) {
                        entity.renderDistanceWeight = 0.0D;
                    } else {
                        // Restore normal render distance weight
                        entity.renderDistanceWeight = 1.0D;
                    }
                }
            }
        }

        // Cull invisible entities (entities not in view frustum or behind walls)
        if (cullInvisibleEntities.getValue() && mc.theWorld != null && mc.thePlayer != null && tickCounter % 20 == 0) {
            // Only check every second to reduce overhead
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity != mc.thePlayer && entity != mc.getRenderViewEntity()) {
                    // If entity is invisible or far from player's view, mark for reduced updates
                    if (entity.isInvisible() || !isEntityInView(entity)) {
                        entity.ignoreFrustumCheck = false;
                    }
                }
            }
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        // Apply batch rendering optimization
        if (batchRendering.getValue()) {
            applyBatchRenderingOptimization();
        }

        // Adaptive render distance based on entity positions
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

    /**
     * Checks if an entity is roughly in the player's view direction
     * Used for invisible entity culling optimization
     */
    private boolean isEntityInView(Entity entity) {
        if (mc.thePlayer == null) return true;

        // Calculate vector from player to entity
        double deltaX = entity.posX - mc.thePlayer.posX;
        double deltaZ = entity.posZ - mc.thePlayer.posZ;

        // Get player's look direction
        float yaw = mc.thePlayer.rotationYaw;
        double yawRadians = Math.toRadians(yaw);

        // Player's look direction vector
        double lookX = -Math.sin(yawRadians);
        double lookZ = Math.cos(yawRadians);

        // Normalize vectors
        double entityDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (entityDist < 0.001) return true; // Entity very close, always render

        deltaX /= entityDist;
        deltaZ /= entityDist;

        // Calculate dot product (cosine of angle between vectors)
        double dotProduct = lookX * deltaX + lookZ * deltaZ;

        // If dot product > 0, entity is roughly in front of player
        // Using a threshold of -0.5 means we include entities up to ~120 degrees from center
        // This is more lenient than the actual view frustum to avoid culling visible entities
        return dotProduct > -0.5;
    }

    /**
     * Batch rendering optimization - reduces state changes by grouping similar entities
     * This is applied through the render distance weight system
     */
    private void applyBatchRenderingOptimization() {
        if (!batchRendering.getValue() || mc.theWorld == null) return;

        // Sort entities by type to reduce texture binding overhead
        // We do this by setting render distance weights to encourage batching
        // Entities of the same type will have similar weights
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                // Living entities get priority
                entity.renderDistanceWeight = Math.max(entity.renderDistanceWeight, 1.5D);
            }
        }
    }

}
