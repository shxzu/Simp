package cc.simp.event.impl.packet;

import net.minecraft.network.Packet;
import cc.simp.event.CancellableEvent;

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
