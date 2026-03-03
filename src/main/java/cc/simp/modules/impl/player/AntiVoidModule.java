package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.LagProcess;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.PlayerUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.util.Vector3d;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Anti Void", category = ModuleCategory.PLAYER)
public final class AntiVoidModule extends Module {

    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Position);
    private final NumberProperty distance = new NumberProperty("Distance", 5, () -> mode.getValue() != Mode.Blink, 0, 10, 1);

    public enum Mode {
        Position,
        Packet,
        Collision,
        Blink
    }

    private Vector3d position, motion;
    private Vector2f rotation;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (mode.getValue() == Mode.Blink) {
            if (event.isPre()) return;
            if (mc.thePlayer.ticksExisted <= 60) return;

            if (position != null && motion != null && rotation != null && !PlayerUtils.isBlockUnder(50, true)) {
                if (mc.thePlayer.fallDistance > 4) {
                    mc.thePlayer.setPosition(position.x, position.y, position.z);
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionY = MovementUtils.predictedMotion(motion.y);
                    mc.thePlayer.motionZ = 0;
                    mc.thePlayer.rotationYaw = rotation.x;
                    mc.thePlayer.rotationPitch = rotation.y;
                    mc.thePlayer.fallDistance = 0;
                    LagProcess.packets.removeIf(timedPacket -> !(timedPacket.getPacket() instanceof C0FPacketConfirmTransaction || timedPacket.getPacket() instanceof C00PacketKeepAlive));
                    LagProcess.disable();
                    LagProcess.dispatch();
                }
            } else {
                position = new Vector3d(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                motion = new Vector3d(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
                rotation = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            }
        }
        if (mode.getValue() == Mode.Position) {
            if (!event.isPre()) return;
            if (mc.thePlayer.fallDistance > distance.getValue().floatValue() && !PlayerUtils.isBlockUnder()) {
                event.setPosY(event.getPosY() + mc.thePlayer.fallDistance);
            }
        }
        if (mode.getValue() == Mode.Packet) {
            if (!event.isPre()) return;
            if (mc.thePlayer.fallDistance > distance.getValue().floatValue() && !PlayerUtils.isBlockUnder()) {
                PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition());
            }
        }
        if (mode.getValue() == Mode.Collision) {
            if (!event.isPre()) return;
            if (mc.thePlayer.fallDistance > distance.getValue().intValue() && !PlayerUtils.isBlockUnder() && mc.thePlayer.posY + mc.thePlayer.motionY < Math.floor(mc.thePlayer.posY)) {
                mc.thePlayer.motionY = Math.floor(mc.thePlayer.posY) - mc.thePlayer.posY;
                if (mc.thePlayer.motionY == 0) {
                    mc.thePlayer.onGround = true;
                    event.setOnGround(true);
                }
            }
        }
    };
}
