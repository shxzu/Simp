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
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.RaytraceUtils;
import cc.simp.utils.client.mc.RotationUtils;
import cc.simp.utils.client.mc.ScaffoldUtils;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Scaffold", category = ModuleCategory.PLAYER)
public final class ScaffoldModule extends Module {

    public static EnumProperty<Rotations> rotationsProperty = new EnumProperty<>("Rotations", Rotations.NORMAL);
    public DoubleProperty delayProperty = new DoubleProperty("Delay", 25, 0, 100, 5);
    public static Property<Boolean> sprintProperty = new Property<>("Sprint", false);
    public static Property<Boolean> raytraceProperty = new Property<>("Raytrace", true);
    public static Property<Boolean> safewalkProperty = new Property<>("Safe Walk", false);
    public static Property<Boolean> autoJumpProperty = new Property<>("Auto Jump", false);
    public static Property<Boolean> keepYProperty = new Property<>("Keep Y", false);
    public static Property<Boolean> canSpeedProperty = new Property<>("Can Speed", false);
    public static Property<Boolean> f5Property = new Property<>("Auto F5", false);

    private enum Rotations {
        NORMAL,
        VULCAN;
    }

    public ScaffoldModule() {
        setSuffixListener(rotationsProperty);
    }

    private static ScaffoldUtils.BlockCache lastBlockCache;
    private static ScaffoldUtils.BlockCache blockCache;
    private final Timer delayTimer = new Timer();
    public static double keepYCoord;
    private boolean rotated = false;
    private int sneak_index;
    private int prevSlot;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) {

            // Slot Finalization
            mc.thePlayer.inventory.currentItem = ScaffoldUtils.getBlockSlot();

            // Keep Y Setup
            if (mc.thePlayer.onGround) {
                keepYCoord = Math.floor(mc.thePlayer.posY - 1.0);
            }

            // Auto Jump Setup
            if (autoJumpProperty.getValue() && !mc.gameSettings.keyBindJump.isPressed() && mc.thePlayer.onGround
                    && MovementUtils.isMoving()) {
                mc.thePlayer.jump();
            }

            // Can Speed Setup
            if(!canSpeedProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()) Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).setEnabled(false);

            // Rotations Setup
            if(Simp.INSTANCE.getModuleManager().getModule(SmoothRotationsModule.class).isEnabled()) {
                event.setYaw(RotationUtils.smoothYaw(getRots()[0]));
                event.setPitch(RotationUtils.smoothPitch(getRots()[1]));
            } else {
                event.setYaw(getRots()[0]);
                event.setPitch(getRots()[1]);
            }
            rotated = true;

            // Sprint Handling
            if (sprintProperty.getValue()) {
                mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
            } else {
                mc.thePlayer.setSprinting(false);
                mc.getNetHandler().sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }

            // Block Cache Setup
            blockCache = ScaffoldUtils.getBlockInfo();
            if (blockCache != null) {
                lastBlockCache = ScaffoldUtils.getBlockInfo();
            }

            // Place The Block
            place();

        }
    };

    @EventLink
    private final Listener<SafeWalkEvent> safeWalkListener = event -> {
        event.setCancelled(safewalkProperty.getValue());
    };

    public static float[] getRots() {
        float[] rotations = new float[2];

        switch (rotationsProperty.getValue()) {
            case NORMAL:
                rotations[0] = ScaffoldUtils.getYaw();
                if (blockCache == null) {
                    rotations[1] = 78;
                } else {
                    for (float possiblePitch = 90; possiblePitch > 30; possiblePitch -= possiblePitch > (mc.thePlayer
                            .isPotionActive(Potion.moveSpeed) ? 60 : 80) ? 1 : 10) {
                        if (RaytraceUtils.isOnBlock(blockCache.getFacing(), blockCache.getPosition(), true, 4.5f,
                                rotations[0], possiblePitch)) {
                            rotations[1] = possiblePitch;
                        }
                    }

                }
                break;
            case VULCAN:
                rotations[0] = ScaffoldUtils.getYaw();
                rotations[1] = 78;
                break;
        }
        return rotations;
    }

    private void place() {
        if (blockCache == null || lastBlockCache == null || mc.thePlayer.inventory.currentItem != ScaffoldUtils.getBlockSlot() || !rotated)
            return;

        if (delayTimer.hasTimeElapsed(delayProperty.getValue().intValue() + MathUtils.getRandomNumberUsingNextInt(1, 19))) {
            if (raytraceProperty.getValue()) {
                if (RaytraceUtils.isOnBlock(lastBlockCache.getFacing(), lastBlockCache.getPosition(), true, 4.5f,
                        Simp.INSTANCE.getRotationHandler().getServerYaw(), Simp.INSTANCE.getRotationHandler().getServerPitch())) {
                    mc.rightClickMouse();
                    sneak_index++;
                }
            } else {
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                        mc.thePlayer.inventory.getStackInSlot(ScaffoldUtils.getBlockSlot()), blockCache.getPosition(),
                        blockCache.getFacing(),
                        ScaffoldUtils.getRaycastedVec3(blockCache, Simp.INSTANCE.getRotationHandler().getServerYaw(), Simp.INSTANCE.getRotationHandler().getServerPitch()))) {
                    mc.thePlayer.swingItem();
                    sneak_index++;
                }
            }
            delayTimer.reset();
            blockCache = null;
        }
    }

    @Override
    public void onEnable() {
        lastBlockCache = null;
        prevSlot = mc.thePlayer.inventory.currentItem;
        if (f5Property.getValue()) {
            mc.gameSettings.thirdPersonView = 1;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.inventory.currentItem = prevSlot;
        if (f5Property.getValue()) {
            mc.gameSettings.thirdPersonView = 0;
        }
        if (mc.thePlayer.isSneaking()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
        rotated = false;
        super.onDisable();
    }

}
