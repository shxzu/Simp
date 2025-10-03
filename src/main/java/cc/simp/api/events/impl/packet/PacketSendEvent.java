package cc.simp.api.events.impl.packet;

import cc.simp.api.events.CancellableEvent;
import net.minecraft.network.Packet;

public final class PacketSendEvent extends CancellableEvent {

    private Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
