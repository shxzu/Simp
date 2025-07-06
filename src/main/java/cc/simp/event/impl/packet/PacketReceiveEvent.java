package cc.simp.event.impl.packet;

import net.minecraft.network.Packet;
import cc.simp.event.CancellableEvent;

public final class PacketReceiveEvent extends CancellableEvent {

    private Packet<?> packet;
    public Direction direction;

    public enum Direction {
        SEND, RECEIVE
    }

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }
    public Packet<?> getPacket() {
        return packet;
    }
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

}

