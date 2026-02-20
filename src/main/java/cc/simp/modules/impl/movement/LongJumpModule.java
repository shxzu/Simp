package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.events.impl.player.TeleportEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Long Jump", category = ModuleCategory.MOVEMENT)
public final class LongJumpModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);
    private final NumberProperty height = new NumberProperty("Height", 0.5, () -> mode.getValue() == Mode.Vanilla, 0.1, 1, 0.01);
    private final NumberProperty speed = new NumberProperty("Speed", 1, () -> mode.getValue() == Mode.Vanilla, 0.1, 9.5, 0.1);
    private final NumberProperty groundSpeed = new NumberProperty("Ground Speed", 0.4, () -> mode.getValue() == Mode.NCP, 0.1, 3, 0.1);
    private final NumberProperty jumpSpeed = new NumberProperty("Jump Speed", 1.4, () -> mode.getValue() == Mode.NCP, 0, 3, 0.1);
    private final NumberProperty glide = new NumberProperty("Glide", 0, () -> mode.getValue() == Mode.NCP, 0, 3, 0.5);
    private final NumberProperty timer = new NumberProperty("Timer", 1, () -> mode.getValue() == Mode.NCP, 0.1, 10, 0.1);

    private enum Mode {
        Vanilla("Vanilla"),
        OldIntaveBoat("Old Intave Boat"),
        NCP("NCP"),
        DoubleJump("Double Jump");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private boolean reset;
    private double nSpeed;

    @EventLink
    public final Listener<StrafeEvent> onStrafe = event -> {
        if (mode.getValue() == Mode.Vanilla) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = height.getValue().floatValue();
            }
            event.setSpeed(speed.getValue().floatValue());
        }
        if (mode.getValue() == Mode.NCP) {
            final double base = MovementUtils.getAllowedHorizontalDistance();

            if (MovementUtils.isMoving()) {
                switch (mc.thePlayer.offGroundTicks) {
                    case 0:
                        mc.thePlayer.motionY = 0.42f;
                        nSpeed = groundSpeed.getValue().doubleValue();
                        break;

                    case 1:
                        nSpeed = jumpSpeed.getValue().doubleValue();
                        break;

                    default:
                        nSpeed -= nSpeed / MovementUtils.BUNNY_FRICTION;
                        break;
                }

                mc.timer.timerSpeed = timer.getValue().floatValue();
                reset = false;
            } else if (!reset) {
                nSpeed = MovementUtils.getAllowedHorizontalDistance();
                mc.timer.timerSpeed = 1;
                reset = true;
            }

            if (mc.thePlayer.fallDistance > 0) {
                mc.thePlayer.motionY += glide.getValue().floatValue() / 100;
            }

            if (mc.thePlayer.isCollidedHorizontally) {
                nSpeed = MovementUtils.getAllowedHorizontalDistance();
            }

            event.setSpeed(Math.max(nSpeed, base), Math.random() / 2000);
        }
        if (mode.getValue() == Mode.DoubleJump) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.jump();
                this.toggle();
            }
        }
    };

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (!e.isPre()) return;
        if (mode.getValue() == Mode.OldIntaveBoat) {
            if (mc.thePlayer.isRiding()) {
                PacketUtils.sendSilentPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX - Math.sin(Math.toRadians((double) mc.thePlayer.rotationYaw)) * 3.0D, mc.thePlayer.posY + 2.0D, mc.thePlayer.posZ + Math.cos(Math.toRadians((double) mc.thePlayer.rotationYaw)) * 3.0D, false));
                this.toggle();
            }
        }
    };

    @EventLink
    public final Listener<TeleportEvent> onTeleport = event -> {
        nSpeed = 0;
    };

    @Override
    public void onDisable() {
        if (mode.getValue() == Mode.Vanilla || mode.getValue() == Mode.NCP) {
            MovementUtils.stop();
            mc.timer.timerSpeed = 1f;
        }
        nSpeed = 0;
        super.onDisable();
    }

}
