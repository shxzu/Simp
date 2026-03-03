package cc.simp.processes;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.modules.impl.client.ClientSettingsModule;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class BlinkProcess {

    public static boolean enabled;
    private static boolean disable = false;
    public static final List<Packet<?>> blinkedSendPackets = new ArrayList<>();
    public static final List<Packet<?>> blinkedReceivePackets = new ArrayList<>();

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = event -> {
        if (enabled) {
            Packet<?> packet = event.getPacket();
            blinkedSendPackets.add(packet);
            event.setCancelled();
        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (enabled && ClientSettingsModule.blinkCancelsIncoming.getValue()) {
            Packet<?> packet = event.getPacket();
            blinkedReceivePackets.add(packet);
            event.setCancelled();
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (enabled) {
            disable();
        }
    };

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (disable) {
            if (event.isPre()) return;

            // Disable first to prevent new packets from being added
            enabled = false;
            disable = false;

            // Create copies to avoid ConcurrentModificationException
            List<Packet<?>> sendPacketsCopy = new ArrayList<>(blinkedSendPackets);
            List<Packet<?>> receivePacketsCopy = new ArrayList<>(blinkedReceivePackets);

            // Clear the original lists
            blinkedSendPackets.clear();
            blinkedReceivePackets.clear();

            // Send the packets from the copies
            for (Packet<?> packet : sendPacketsCopy) {
                PacketUtils.sendPacket(packet);
            }
            for (Packet<?> packet : receivePacketsCopy) {
                PacketUtils.receivePacket(packet);
            }
        }
    };

    public static void enable() {
        blinkedSendPackets.clear();
        blinkedReceivePackets.clear();
        enabled = true;
    }

    public static void disable() {
        disable = true;
    }
}
