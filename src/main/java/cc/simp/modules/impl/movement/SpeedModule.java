package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.events.impl.player.MovePlayerEvent;
import cc.simp.api.events.impl.player.SprintEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.PlayerUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockStairs;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Speed", category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module {

    private static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.RotateExploit);

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

    double speedV;
    float timer;
    int offGroundTicks;
    int onGroundTicks;
    boolean prevOnGround;
    private int stage;

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
            case NCP:
                if (MovementUtils.isMoving()) {
                    if (mc.thePlayer.fallDistance > 1.0) {
                        mc.timer.timerSpeed = 0.8f;
                    } else {
                        mc.timer.timerSpeed = 1.9f;
                    }
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42f;
                        float speed = 0.0310f;
                        if (mc.thePlayer.speedInAir < speed) {
                            if (mc.thePlayer.speedInAir < 0.025) {
                                mc.thePlayer.speedInAir = (float) (0.025f + (Math.random() / 100));
                            }
                            mc.thePlayer.speedInAir += 0.0091f;
                        } else {
                            mc.thePlayer.speedInAir = speed;
                        }
                        if (mc.thePlayer.jumpMovementFactor > 0.022) {
                            mc.thePlayer.jumpMovementFactor -= 0.002f;
                        }
                    } else {
                        mc.thePlayer.motionY *= 1.00;
                        mc.thePlayer.motionX *= 0.982f;
                        mc.thePlayer.motionZ *= 0.982f;
                        mc.thePlayer.speedInAir -= 0.00019f;
                        if (mc.thePlayer.fallDistance > 0.4 && mc.thePlayer.fallDistance < 0.41) {
                            mc.thePlayer.motionY -= 0.1f;
                        }
                        if (mc.thePlayer.hurtTime > 4) {
                            mc.thePlayer.speedInAir += 0.006f;
                        }
                    }
                }
                break;
        }
    };

    @EventLink
    public final Listener<MovePlayerEvent> movePlayerEventListener = e -> {
        if (mode.getValue() == Mode.NCP) {
            if (mc.thePlayer.onGround) {
                e.setY(mc.thePlayer.motionY = 0.42F);
                if (speedV < 0.2805) {
                    speedV = 0.2805;
                }
                speedV *= 1.949;
                stage = 0;
            }
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
        if (mode.getValue() == Mode.UpdatedNCP || mode.getValue() == Mode.NCP) {
            mc.timer.timerSpeed = 1.0f;
            mc.thePlayer.speedInAir = 0.02F;
        }
        if (mode.getValue() == Mode.Jump && mc.gameSettings.keyBindJump.isPressed())
            mc.gameSettings.keyBindJump.setPressed(false);
        if (mode.getValue() == Mode.RotateExploit && mc.gameSettings.keyBindJump.isPressed())
            mc.gameSettings.keyBindJump.setPressed(false);
    }
}
