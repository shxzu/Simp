package cc.simp.modules.impl.movement;

import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.BooleanProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Speed", category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module {

    private final EnumProperty<SpeedMode> speedModeProperty = new EnumProperty<>("Mode", SpeedMode.WATCHDOG);

    private boolean wasOnGround;
    private int airTicks;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if(mc.gameSettings.keyBindJump.isKeyDown()) return;
        if(event.isPost()) return;

        switch (speedModeProperty.getValue()) {
            case WATCHDOG:
                if (MovementUtils.isOnGround()) {
                    if (MovementUtils.isMoving()) {
                        mc.thePlayer.jump();
                        MovementUtils.strafe();
                    }
                }

                break;
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
                if (MovementUtils.isMoving()) {
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                }
                break;
            case UNCP:
                if (MovementUtils.isMoving()) {
                    MovementUtils.strafe();
                    if (airTicks >= 5.1) {
                        mc.timer.timerSpeed = 1.2f;
                    } else mc.timer.timerSpeed = 1.0f;
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                }
                break;
            case MUSH:
                if (MovementUtils.isMoving()) {
                    MovementUtils.strafe();
                    mc.timer.timerSpeed = 1.2f;
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        mc.timer.timerSpeed = 1.0f;
                    }
                }
                break;
        }

        if(mc.thePlayer.onGround) {
            airTicks = 0;
        } else {
            airTicks++;
        }
    };

    private enum SpeedMode {
        WATCHDOG, UNCP, MUSH, VERUS, STRAFE, LEGIT
    }

}
