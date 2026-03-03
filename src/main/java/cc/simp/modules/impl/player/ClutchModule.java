package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.ModuleManager;
import cc.simp.utils.mc.PlayerUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockAir;
import net.minecraft.util.BlockPos;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Clutch", category = ModuleCategory.PLAYER)
public final class ClutchModule extends Module {

    private final NumberProperty voidDistance = new NumberProperty("Void Distance", 10, 5, 50, 1);
    private final NumberProperty blockSearchRadius = new NumberProperty("Block Search Radius", 3, 1, 5, 1);
    private final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    private ScaffoldModule scaffoldModule;
    private boolean wasScaffoldEnabled = false;

    @Override
    public void onEnable() {
        super.onEnable();
        scaffoldModule = ModuleManager.getInstance(ScaffoldModule.class);
        wasScaffoldEnabled = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Disable scaffold if we enabled it
        if (scaffoldModule != null && wasScaffoldEnabled && scaffoldModule.isEnabled()) {
            scaffoldModule.toggle();
            wasScaffoldEnabled = false;
        }
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (scaffoldModule != null && wasScaffoldEnabled && scaffoldModule.isEnabled()) {
            scaffoldModule.toggle();
            wasScaffoldEnabled = false;
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            if (scaffoldModule != null && wasScaffoldEnabled && scaffoldModule.isEnabled()) {
                scaffoldModule.toggle();
                wasScaffoldEnabled = false;
            }
            return;
        }

        // Check if we're above void
        boolean isAboveVoid = !PlayerUtils.isBlockUnder(voidDistance.getValue(), true);

        if (!isAboveVoid) {
            // Not above void, make sure scaffold is off if we enabled it
            if (wasScaffoldEnabled && scaffoldModule != null && scaffoldModule.isEnabled()) {
                scaffoldModule.toggle();
                wasScaffoldEnabled = false;
            }
            return;
        }

        // We're above void, check if there are blocks nearby to clutch on
        boolean hasNearbyBlocks = hasBlocksNearby();

        if (hasNearbyBlocks) {
            // Enable scaffold if not already enabled
            if (scaffoldModule != null && !scaffoldModule.isEnabled()) {
                scaffoldModule.toggle();
                wasScaffoldEnabled = true;
            }
        } else {
            // No blocks nearby, disable scaffold if we enabled it
            if (wasScaffoldEnabled && scaffoldModule != null && scaffoldModule.isEnabled()) {
                scaffoldModule.toggle();
                wasScaffoldEnabled = false;
            }

            // Auto disable clutch if configured
            if (autoDisable.getValue()) {
                this.toggle();
            }
        }
    };

    /**
     * Checks if there are solid blocks nearby that the player could clutch onto
     */
    private boolean hasBlocksNearby() {
        int radius = blockSearchRadius.getValue().intValue();
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        // Check in a radius around the player (horizontal only, and a few blocks down)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip the block directly below the player as we're looking for blocks to clutch onto
                    if (x == 0 && y == -1 && z == 0) continue;

                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (!(mc.theWorld.getBlockState(checkPos).getBlock() instanceof BlockAir)) {
                        // Found a non-air block nearby
                        return true;
                    }
                }
            }
        }

        return false;
    }
}


