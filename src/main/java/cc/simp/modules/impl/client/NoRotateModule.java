package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S18PacketEntityTeleport;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Rotate", category = ModuleCategory.CLIENT)
public class NoRotateModule extends Module {

    @EventLink
    private final Listener<PacketReceiveEvent> receievePacketEventListener = event -> {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.getPacket();
            packet.setYaw(mc.thePlayer.rotationYaw);
            packet.setPitch(mc.thePlayer.rotationPitch);
        }
        if (event.getPacket() instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport packet = (S18PacketEntityTeleport) event.getPacket();
            packet.setYaw((byte) mc.thePlayer.rotationYaw);
            packet.setPitch((byte) mc.thePlayer.rotationPitch);
        }
    };

}
