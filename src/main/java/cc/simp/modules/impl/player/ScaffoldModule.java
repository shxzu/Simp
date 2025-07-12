package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.SafeWalkEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.property.impl.Representation;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.RaytraceUtils;
import cc.simp.utils.client.mc.RotationUtils;
import cc.simp.utils.client.mc.ScaffoldUtils;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.potion.Potion;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Scaffold", category = ModuleCategory.PLAYER)
public final class ScaffoldModule extends Module {

    public static EnumProperty<Rotations> rotationsProperty = new EnumProperty<>("Rotations", Rotations.NORMAL);
    public DoubleProperty delayProperty = new DoubleProperty("Delay", 25, 0, 100, 5);
    public static Property<Boolean> sprintProperty = new Property<>("Sprint", false);
    public static Property<Boolean> raytraceProperty = new Property<>("Raytrace", true);
    public static Property<Boolean> raytraceStrictProperty = new Property<>("Strict", false, raytraceProperty::getValue);
    public static Property<Boolean> safewalkProperty = new Property<>("Safe Walk", false);
    public static Property<Boolean> autoJumpProperty = new Property<>("Auto Jump", false);
    public static Property<Boolean> sneakProperty = new Property<>("Sneak", false);
    public DoubleProperty sneakIntervalProperty = new DoubleProperty("Sneak Every Blocks", 5, sneakProperty::getValue, 1, 10, 1, Representation.INT);
    public static Property<Boolean> keepYProperty = new Property<>("Keep Y", false);
    public static Property<Boolean> allowSpeedModuleProperty = new Property<>("Allow Speed", false);
    public static Property<Boolean> autoF5Property = new Property<>("Auto F5", false);

    private enum Rotations {
        NORMAL,
        STATIC,
        HYPIXEL,
        VULCAN;
    }

    private static final float NORMAL_PITCH = 82.5f;
    private static final float VULCAN_PITCH = 78;
    private static final float RAYTRACE_DISTANCE = 4.5f;
    private static final int RANDOM_DELAY_MIN = 1;
    private static final int RANDOM_DELAY_MAX = 19;

    private static ScaffoldUtils.BlockCache currentBlockCache;
    private static ScaffoldUtils.BlockCache lastBlockCache;
    private final Timer delayTimer = new Timer();
    public static double keepYCoord;
    private boolean rotatedThisTick = false;
    private int blocksPlacedCount;
    private int previousHotbarSlot;
    private boolean isSneakingNow = false;

    public ScaffoldModule() {
        setSuffixListener(rotationsProperty);
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) {
            handleSprint();

            // Cache current slot
            previousHotbarSlot = mc.thePlayer.inventory.currentItem;

            // Slot Finalization
            int blockSlot = ScaffoldUtils.getBlockSlot();
            if (blockSlot == -1) { // No block found, early exit
                currentBlockCache = null;
                lastBlockCache = null;
                rotatedThisTick = false;
                if (isSneakingNow) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                    isSneakingNow = false;
                }
                return;
            }
            mc.thePlayer.inventory.currentItem = blockSlot;

            handleKeepY();
            handleAutoJump();
            handleSpeedModuleInteraction();

            // Block Cache Setup
            currentBlockCache = ScaffoldUtils.getBlockInfo();

            // Update lastBlockCache
            if (currentBlockCache != null) {
                lastBlockCache = currentBlockCache;
            } else {
                lastBlockCache = null;
            }

            // Rotations Setup
            float[] rotations = getRotationsForPlacement();
            Simp.INSTANCE.getRotationManager().rotateToward(rotations[0], rotations[1], ClientRotationsModule.rotSpeed.getValue().floatValue());
            rotatedThisTick = true;

            // Place The Block
            placeBlock();

            // Handle Sneak
            handleSneak();
        }
    };

    @EventLink
    private final Listener<SafeWalkEvent> safeWalkListener = event -> {
        event.setCancelled(safewalkProperty.getValue());
    };

    private void handleSprint() {
        if (sprintProperty.getValue()) {
            mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
        } else {
            mc.thePlayer.setSprinting(false);
        }
    }

    private void handleKeepY() {
        if (keepYProperty.getValue() && mc.thePlayer.onGround) {
            keepYCoord = Math.floor(mc.thePlayer.posY - 1.0);
        }
    }

    private void handleAutoJump() {
        if (autoJumpProperty.getValue() && !mc.gameSettings.keyBindJump.isPressed() && mc.thePlayer.onGround
                && MovementUtils.isMoving()) {
            mc.thePlayer.jump();
        }
    }

    private void handleSpeedModuleInteraction() {
        if (!allowSpeedModuleProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()) {
            Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).setEnabled(false);
        }
    }

    public static float[] getRotationsForPlacement() {
        float[] rotations = new float[2];

        switch (rotationsProperty.getValue()) {
            case NORMAL:
                rotations[0] = MovementUtils.getDirection() - 180;
                rotations[1] = NORMAL_PITCH;
                if (currentBlockCache != null) {
                    for (float possiblePitch = 90; possiblePitch > 30; possiblePitch -= possiblePitch > (mc.thePlayer
                            .isPotionActive(Potion.moveSpeed) ? 60 : 80) ? 1 : 10) {
                        if (RaytraceUtils.isOnBlock(currentBlockCache.getFacing(), currentBlockCache.getPosition(), true, mc.playerController.getBlockReachDistance(),
                                rotations[0], possiblePitch)) {
                            rotations[1] = possiblePitch;
                        }
                    }
                }
                break;
            case HYPIXEL:
                rotations[0] = ScaffoldUtils.getHypixelNCPYaw();
                rotations[1] = NORMAL_PITCH;
                if (currentBlockCache != null) {
                    for (float possiblePitch = 90; possiblePitch > 30; possiblePitch -= possiblePitch > (mc.thePlayer
                            .isPotionActive(Potion.moveSpeed) ? 60 : 80) ? 1 : 10) {
                        if (RaytraceUtils.isOnBlock(currentBlockCache.getFacing(), currentBlockCache.getPosition(), true, mc.playerController.getBlockReachDistance(),
                                rotations[0], possiblePitch)) {
                            rotations[1] = possiblePitch;
                        }
                    }
                }
                break;
            case STATIC:
                rotations[0] = MovementUtils.getDirection() - 180;
                rotations[1] = NORMAL_PITCH;
                break;
            case VULCAN:
                rotations[0] = MovementUtils.getDirection() - 180;
                rotations[1] = VULCAN_PITCH;
                break;
        }
        return rotations;
    }

    private void placeBlock() {
        if (currentBlockCache == null || mc.thePlayer.inventory.currentItem == -1 || !rotatedThisTick) {
            return;
        }

        int randomDelay = MathUtils.getRandomNumberUsingNextInt(RANDOM_DELAY_MIN, RANDOM_DELAY_MAX);
        if (delayTimer.hasTimeElapsed(delayProperty.getValue().intValue() + randomDelay)) {

            boolean performedAction = false;
            if (raytraceProperty.getValue()) {
                if (raytraceStrictProperty.getValue()) {
                    if (RaytraceUtils.isOnBlock(currentBlockCache.getFacing(), currentBlockCache.getPosition(), true,
                            RAYTRACE_DISTANCE, Simp.INSTANCE.getRotationManager().getClientYaw(),
                            Simp.INSTANCE.getRotationManager().getClientPitch())) {
                        performedAction = attemptPlaceBlock(currentBlockCache);
                    }
                } else {
                    if (RaytraceUtils.isOnBlock(currentBlockCache.getFacing(), currentBlockCache.getPosition(), false,
                            RAYTRACE_DISTANCE, Simp.INSTANCE.getRotationManager().getClientYaw(),
                            Simp.INSTANCE.getRotationManager().getClientPitch())) {
                        performedAction = attemptPlaceBlock(currentBlockCache);
                    }
                }
            } else {
                performedAction = attemptPlaceBlock(currentBlockCache);
            }

            if (performedAction) {
                blocksPlacedCount++;
            }

            delayTimer.reset();
            currentBlockCache = null;
        }
    }

    private boolean attemptPlaceBlock(ScaffoldUtils.BlockCache cache) {
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem),
                cache.getPosition(),
                cache.getFacing(),
                cache.getHitVec())) {
            mc.thePlayer.swingItem();
            return true;
        }
        return false;
    }

    private void handleSneak() {
        if (sneakProperty.getValue()) {
            double sneakInterval = sneakIntervalProperty.getValue();

            if (blocksPlacedCount >= sneakInterval) {
                if (!isSneakingNow) {
                    mc.gameSettings.keyBindSneak.setPressed(true);
                    isSneakingNow = true;
                }
                blocksPlacedCount = 0;
            } else {
                if (isSneakingNow) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                    isSneakingNow = false;
                }
            }
        } else {
            if (isSneakingNow) {
                mc.gameSettings.keyBindSneak.setPressed(false);
                isSneakingNow = false;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastBlockCache = null;
        currentBlockCache = null;
        previousHotbarSlot = mc.thePlayer.inventory.currentItem;
        rotatedThisTick = false;
        blocksPlacedCount = 0;
        isSneakingNow = false;

        if (autoF5Property.getValue()) {
            mc.gameSettings.thirdPersonView = 1;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.thePlayer.inventory.currentItem = previousHotbarSlot;

        if (autoF5Property.getValue()) {
            mc.gameSettings.thirdPersonView = 0;
        }

        if (isSneakingNow) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            isSneakingNow = false;
        }
        rotatedThisTick = false;
        blocksPlacedCount = 0;
        currentBlockCache = null;
        lastBlockCache = null;
    }
}