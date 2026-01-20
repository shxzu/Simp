package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.TeleportEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Rotate", category = ModuleCategory.CLIENT)
public class NoRotateModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Edit);

    private enum Mode {
        Edit,
        Packet,
    }

    private float yaw, pitch;
    private boolean teleport;

    @EventLink
    public final Listener<TeleportEvent> onTeleport = event -> {
        if (mode.getValue() == Mode.Packet) {
            this.yaw = event.getYaw();
            this.pitch = event.getPitch();

            event.setYaw(mc.thePlayer.rotationYaw);
            event.setPitch(mc.thePlayer.rotationPitch);

            this.teleport = true;
        }
        if (mode.getValue() == Mode.Edit) {
            event.setYaw(mc.thePlayer.rotationYaw);
            event.setPitch(mc.thePlayer.rotationPitch);
        }
    };

    @EventLink
    public final Listener<PacketSendEvent> onPacketSend = event -> {
        if (mode.getValue() == Mode.Packet) {
            final Packet packet = event.getPacket();

            if (this.teleport && packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
                final C03PacketPlayer.C06PacketPlayerPosLook c06PacketPlayerPosLook = ((C03PacketPlayer.C06PacketPlayerPosLook) packet);

                c06PacketPlayerPosLook.yaw = this.yaw;
                c06PacketPlayerPosLook.pitch = this.pitch;

                event.setPacket(c06PacketPlayerPosLook);

                this.teleport = false;
            }
        }
    };

}
