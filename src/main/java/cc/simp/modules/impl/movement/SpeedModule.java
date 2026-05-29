package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.*;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Speed", category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module {

    private static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Legit);
    private static final NumberProperty vanillaSpeed = new NumberProperty("Vanilla Speed", 1, () -> mode.getValue() == Mode.Vanilla, 0.1, 9.5, 0.1);

    private enum Mode {
        Vanilla("Vanilla"),
        Legit("Legit"),
        LegitExploit("Legit Exploit"),
        VerusGround("Verus Ground"),
        VerusLowHop("Verus Low Hop"),
        UpdatedNCP("Updated NCP"),
        NCP("NCP"),
        Strafe("Strafe"),
        OldHypixel("Old Hypixel"),
        Intave("Intave"),
        OldGrim("Old Grim");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private double speedV;
    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (mc.gameSettings.keyBindJump.isKeyDown() && mode.getValue() != Mode.Legit && mode.getValue() != Mode.LegitExploit && mode.getValue() != Mode.OldGrim)
            mc.gameSettings.keyBindJump.setPressed(false);
        if (!e.isPre()) return;
        switch (mode.getValue()) {
            case VerusGround:
                if (mc.gameSettings.keyBindJump.isKeyDown()) return;

                PacketUtils.sendSilentPacket(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, new ItemStack(Items.water_bucket), 0, 0.5f, 0));

                if (MovementUtils.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.00001f;
                    }
                } else {
                    mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                }

                MovementUtils.strafe(MovementUtils.getVerusLimit(true));

                if (mc.thePlayer.fallDistance > 0.2) {
                    mc.thePlayer.motionY = -0.1f;
                }
                break;
            case Vanilla:
                if (MovementUtils.isMoving()) {
                    MovementUtils.setSpeed(vanillaSpeed.getValue().floatValue());
                    if (MovementUtils.isOnGround()) {
                        mc.thePlayer.jump();
                    }
                }
                break;
            case VerusLowHop:
                PacketUtils.sendSilentPacket(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, new ItemStack(Items.water_bucket), 0, 0.5f, 0));
                if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                    mc.thePlayer.motionY = 0.42;
                    MovementUtils.strafe(0.48f, 0.52f, 0.6f);
                }
                if (mc.thePlayer.offGroundTicks == 1) {
                    mc.thePlayer.motionY = -0.15233518685055714;
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
            case OldHypixel:
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
            case Legit:
            case OldGrim:
                mc.gameSettings.keyBindJump.setPressed(MovementUtils.isMoving() && MovementUtils.isOnGround());
                break;
            case LegitExploit:
                mc.gameSettings.keyBindJump.setPressed(MovementUtils.isMoving() && MovementUtils.isOnGround());
                mc.timer.timerSpeed = 1.0075f;
                break;
            case Intave:
                if (MovementUtils.isOnGround() && MovementUtils.isMoving()) {
                    mc.thePlayer.jump();
                }

                switch (mc.thePlayer.offGroundTicks) {
                    case 1:
                        mc.thePlayer.motionX *= 1.005;
                        mc.thePlayer.motionZ *= 1.005;
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        mc.thePlayer.motionX *= 1.011;
                        mc.thePlayer.motionZ *= 1.011;
                        break;
                }

                if (mc.thePlayer.onGroundTicks == 1) {
                    mc.thePlayer.motionX *= 1.0045;
                    mc.thePlayer.motionZ *= 1.0045;
                }

                mc.timer.timerSpeed = 1.0075f;
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
            }
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (!MovementUtils.isOnGround() && mode.getValue() == Mode.LegitExploit) {
            RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch), 10, MovementFix.NORMAL);
        }
    };

    @EventLink(value = Priorities.VERY_HIGH)
    public final Listener<StrafeEvent> strafe = event -> {
        if (mode.getValue() == Mode.OldGrim) {
            mc.theWorld.playerEntities.stream()
                    .filter(entityPlayer -> entityPlayer != mc.thePlayer &&
                            mc.thePlayer.getEntityBoundingBox().expand(1, 1, 1)
                                    .intersectsWith(entityPlayer.getEntityBoundingBox()))
                    .forEach(entityPlayer -> MovementUtils.moveFlying(0.08));
        }
    };

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        if (mode.getValue() == Mode.UpdatedNCP || mode.getValue() == Mode.NCP) {
            mc.thePlayer.speedInAir = 0.02F;
        }
        if (mode.getValue() == Mode.Legit && mc.gameSettings.keyBindJump.isPressed())
            mc.gameSettings.keyBindJump.setPressed(false);
        if (mode.getValue() == Mode.LegitExploit && mc.gameSettings.keyBindJump.isPressed())
            mc.gameSettings.keyBindJump.setPressed(false);
        if (mode.getValue() == Mode.OldGrim && mc.gameSettings.keyBindJump.isPressed())
            mc.gameSettings.keyBindJump.setPressed(false);
    }
}
