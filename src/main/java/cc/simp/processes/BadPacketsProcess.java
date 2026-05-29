package cc.simp.processes;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;

import static net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT;

public class BadPacketsProcess {
    public boolean C08;
    public boolean C07;
    private boolean C02;
    public boolean C09;
    public static boolean delayAttack;
    public boolean delay;
    public static int playerSlot = -1;
    public int serverSlot = -1;

    @EventLink(value = Priorities.VERY_HIGH)
    public final Listener<PacketSendEvent> onPacketSend = event -> {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C02PacketUseEntity) { // sending a C07 on the same tick as C02 can ban, this usually happens when you unblock and attack on the same tick
            if (C07) {
                event.setCancelled(true);
                return;
            }
            C02 = true;
        } else if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08 = true;
        } else if (event.getPacket() instanceof C07PacketPlayerDigging) {
            C07 = true;
        } else if (event.getPacket() instanceof C09PacketHeldItemChange) {
            if (((C09PacketHeldItemChange) event.getPacket()).getSlotId() == playerSlot && ((C09PacketHeldItemChange) event.getPacket()).getSlotId() == serverSlot) {
                event.setCancelled(true);
                return;
            }
            C09 = true;
            serverSlot = playerSlot = ((C09PacketHeldItemChange) event.getPacket()).getSlotId();

        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetInEventListener = event -> {
        if (event.getPacket() instanceof S09PacketHeldItemChange) {
            S09PacketHeldItemChange packet = (S09PacketHeldItemChange) event.getPacket();
            if (packet.getHeldItemHotbarIndex() >= 0 && packet.getHeldItemHotbarIndex() < InventoryPlayer.getHotbarSize()) {
                serverSlot = packet.getHeldItemHotbarIndex();
            }
        }
        else if (event.getPacket() instanceof S0CPacketSpawnPlayer && Minecraft.getMinecraft().thePlayer != null) {
            if (((S0CPacketSpawnPlayer) event.getPacket()).getEntityID() != Minecraft.getMinecraft().thePlayer.getEntityId()) {
                return;
            }
            this.playerSlot = -1;
        }
    };


    @EventLink
    public final Listener<MotionEvent> eventMotionListener = e -> {
        if (delay) {
            delayAttack = false;
            delay = false;
        }
        if (C08 || C09) {
            delay = true;
            delayAttack = true;
        }
        C08 = C07 = C02 = C09 = false;
    };
}
