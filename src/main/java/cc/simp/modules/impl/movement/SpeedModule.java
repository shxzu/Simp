package cc.simp.modules.impl.movement;

import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.BooleanProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Speed", category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module {

    private final EnumProperty<SpeedMode> speedModeProperty = new EnumProperty<>("Mode", SpeedMode.LEGIT);
    private final EnumProperty<IntaveMode> intaveModeProperty = new EnumProperty<>("Intave Mode", IntaveMode.MOTION, () -> speedModeProperty.getValue() == SpeedMode.INTAVE);
    public final Property<Boolean> intaveTimerProperty = new Property<>("Timer", false, () -> speedModeProperty.getValue() == SpeedMode.INTAVE);
    public final Property<Boolean> fullStopProperty = new Property<>("Stop on Disable", true);

    private boolean wasOnGround;
    private int airTicks;

    private enum SpeedMode {
        LEGIT, INTAVE, VERUS, STRAFE
    }

    private enum IntaveMode {
        MOTION, FRICTION
    }

    public SpeedModule() {
        setSuffixListener(speedModeProperty);
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if (mc.gameSettings.keyBindJump.isKeyDown() && speedModeProperty.getValue() != SpeedMode.LEGIT) return;
        if (event.isPost()) return;

        switch (speedModeProperty.getValue()) {
            case VERUS:
                if (MovementUtils.isMoving()) {
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                        wasOnGround = true;
                    } else if (wasOnGround) {
                        if (!mc.thePlayer.isCollidedHorizontally) {
                            mc.thePlayer.motionY = -0.0784000015258789;
                        }
                        wasOnGround = false;
                    }
                    MovementUtils.setSpeed(0.33);
                } else {
                    mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                }
                break;
            case STRAFE:
                if (MovementUtils.isMoving()) {
                    MovementUtils.strafe();
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                }
                break;
            case LEGIT:
                mc.gameSettings.keyBindJump.setPressed(MovementUtils.isMoving() && MovementUtils.isOnGround());
                break;
            case INTAVE:
                if (intaveModeProperty.getValue() == IntaveMode.MOTION) {

                    if(MovementUtils.isOnGround() && MovementUtils.isMoving()) {
                        mc.thePlayer.jump();
                    }

                    switch (mc.thePlayer.offGroundTicks) {
                        case 1:
                            mc.thePlayer.motionX *= 1.005;
                            mc.thePlayer.motionZ *= 1.005;
                            break;
                        case 2:
                        case 3:
                        case 5:
                        case 6:
                        case 4:
                            mc.thePlayer.motionX *= 1.011;
                            mc.thePlayer.motionZ *= 1.011;
                            break;
                    }
                }

                if (mc.thePlayer.onGroundTicks == 1) {
                    mc.thePlayer.motionX *= 1.0045;
                    mc.thePlayer.motionZ *= 1.0045;
                }

                if (intaveTimerProperty.getValue()) {
                    mc.timer.timerSpeed = 1.0075f;
                }
                break;
        }

        if (mc.thePlayer.onGround) {
            airTicks = 0;
        } else {
            airTicks++;
        }
    };

    @EventLink
    public final Listener<StrafeEvent> strafeEventListener = event -> {
        if (speedModeProperty.getValue() == SpeedMode.INTAVE && intaveModeProperty.getValue() == IntaveMode.FRICTION) {

            // future proof method! (original method used shit that didnt bypass in intave 13 but did in 14. so
            // it was most likely to flag in future versions.) p.s. u flag celery sometimes but its really rare..

            if (mc.isSingleplayer())
                return;

            if (mc.gameSettings.keyBindJump.isPressed())
                return;

            if (!MovementUtils.isMoving())
                return;

            if (intaveTimerProperty.getValue()) {
                mc.timer.timerSpeed = 1.0075f;
            }

            if (mc.thePlayer.onGround) {
                if (!mc.thePlayer.isCollidedHorizontally && (!(mc.thePlayer.isInLava() || mc.thePlayer.isInWater()))) {
                    event.setFriction(event.getFriction() * 1.084f);
                    mc.thePlayer.jump();
                }
            }
        }
    };

    @Override
    public void onDisable() {
        if (intaveTimerProperty.getValue()) {
            mc.timer.timerSpeed = 1.0f;
        }
        if(fullStopProperty.getValue()) {
            mc.thePlayer.motionX *= 0;
            mc.thePlayer.motionZ *= 0;
        }
        if(speedModeProperty.getValue() == SpeedMode.LEGIT && mc.gameSettings.keyBindJump.isPressed()) mc.gameSettings.keyBindJump.setPressed(false);
    }

}
