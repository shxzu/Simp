package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Logger;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Crash", category = ModuleCategory.CLIENT)
public final class NoCrashModule extends Module {
    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        Packet<?> packet = event.getPacket();

        if (packet instanceof S2BPacketChangeGameState && ((S2BPacketChangeGameState) packet).getGameState() == 5 && !mc.isDemo()) {
            event.setCancelled();
            Logger.chatPrint("Blocked a demo crash packet");
        }

        if (packet instanceof S27PacketExplosion) {
            if (((S27PacketExplosion) packet).getX() > 1E9 || ((S27PacketExplosion) packet).getY() > 1E9 || ((S27PacketExplosion) packet).getZ() > 1E9 || ((S27PacketExplosion) packet).getStrength() == Integer.MAX_VALUE) {
                event.setCancelled();
                Logger.chatPrint("The server tried to crash your client with explosion");
            }
        }

        if (packet instanceof S2APacketParticles) {
            if (((S2APacketParticles) packet).getXCoordinate() > 1E9 || ((S2APacketParticles) packet).getYCoordinate() > 1E9 || ((S2APacketParticles) packet).getZCoordinate() > 1E9 || ((S2APacketParticles) packet).getParticleSpeed() > 1E9 || ((S2APacketParticles) packet).getXOffset() > 1E9 || ((S2APacketParticles) packet).getYOffset() > 1E9 || ((S2APacketParticles) packet).getZOffset() > 1E9) {
                event.setCancelled();
                Logger.chatPrint("The server tried to crash your client with particles");
            }
        }

        if (packet instanceof S0EPacketSpawnObject) {
            if (Math.abs(((S0EPacketSpawnObject) packet).getYaw()) > 1E4 || Math.abs(((S0EPacketSpawnObject) packet).getPitch()) > 1E4) {
                event.setCancelled();
                Logger.chatPrint("Blocked bogus spawn packet (yaw/pitch overflow)");
            }
        }

        if (packet instanceof S3FPacketCustomPayload) {
            String channel = ((S3FPacketCustomPayload) packet).getChannelName();
            if (((S3FPacketCustomPayload) packet).getBufferData() == null || ((S3FPacketCustomPayload) packet).getBufferData().array().length > 1024 * 50) {
                event.setCancelled();
                Logger.chatPrint("Blocked custom payload packet from channel: " + channel);
            }
        }

        if (packet instanceof S09PacketHeldItemChange) {
            if (((S09PacketHeldItemChange) packet).getHeldItemHotbarIndex() == Integer.MAX_VALUE) {
                event.setCancelled();
                Logger.chatPrint("The server tried to crash your client with hotbar switch");
            }
        }
    };
}
