package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.SprintEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Speed", category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.RotateExploit);

    private enum Mode {
        Jump("Jump"),
        RotateExploit("Rotate Exploit"),
        VerusGround("Verus Ground"),
        VerusLowHop("Verus Low Hop"),
        UpdatedNCP("Updated NCP"),
        Strafe("Strafe"),
        GroundStrafe("Ground Strafe"),
        NCP("NCP");

        public String name;
        Mode(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (mc.gameSettings.keyBindJump.isKeyDown() && mode.getValue() != Mode.Jump && mode.getValue() != Mode.RotateExploit)
            mc.gameSettings.keyBindJump.setPressed(false);
        if (!e.isPre()) return;
        switch (mode.getValue()) {
            case VerusGround:
                if (MovementUtils.isMoving()) {
                    if (mc.thePlayer.ticksExisted % 9 == 0 && MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                    MovementUtils.setSpeed(0.32);
                }
                break;
            case VerusLowHop:
                if (MovementUtils.isMoving()) {
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                    if (mc.thePlayer.ticksExisted % 7 == 0 && !MovementUtils.isOnGround()) {
                        mc.thePlayer.motionY = -0.44;
                    }
                    MovementUtils.setSpeed(0.32);
                }
                break;
            case Strafe:
                if (MovementUtils.isMoving()) {
                    MovementUtils.strafe();
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                }
                break;
            case GroundStrafe:
                if (MovementUtils.isMoving()) {
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                        MovementUtils.strafe();
                    }
                }
                break;
            case UpdatedNCP:
                if (MovementUtils.isMoving() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    MovementUtils.strafe();
                    if (mc.thePlayer.offGroundTicks >= 5.1) {
                        mc.timer.timerSpeed = 1.2f;
                    } else
                        mc.timer.timerSpeed = 1.0f;
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                }
                if (!MovementUtils.isMoving()) {
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
            case Jump:
            case RotateExploit:
                mc.gameSettings.keyBindJump.setPressed(MovementUtils.isMoving() && MovementUtils.isOnGround());
                break;
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (!MovementUtils.isOnGround() && mode.getValue() == Mode.RotateExploit) {
            RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch), 10, MovementFix.NORMAL);
        }
    };

    @Override
    public void onDisable() {
        if (mode.getValue() == Mode.UpdatedNCP) {
            mc.timer.timerSpeed = 1.0f;
            mc.thePlayer.jumpMovementFactor = 0.02F;
        }
        if(mode.getValue() == Mode.Jump && mc.gameSettings.keyBindJump.isPressed()) mc.gameSettings.keyBindJump.setPressed(false);
        if(mode.getValue() == Mode.RotateExploit && mc.gameSettings.keyBindJump.isPressed()) mc.gameSettings.keyBindJump.setPressed(false);
    }
}
