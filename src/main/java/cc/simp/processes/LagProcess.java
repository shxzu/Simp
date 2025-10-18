package cc.simp.processes;

import cc.simp.api.events.CancellableEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.Packet;
import net.minecraft.util.Tuple;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import static cc.simp.utils.Util.mc;


public class LagProcess {
    public static ConcurrentLinkedQueue<PacketUtils.TimedPacket> packets = new ConcurrentLinkedQueue<>();
    static Timer enabledTimer = new Timer();
    public static boolean enabled;
    static long amount;
    static Tuple<Class[], Boolean> regular = new Tuple<>(new Class[]{C0FPacketConfirmTransaction.class, C00PacketKeepAlive.class, S1CPacketEntityMetadata.class}, false);
    static Tuple<Class[], Boolean> velocity = new Tuple<>(new Class[]{S12PacketEntityVelocity.class, S27PacketExplosion.class}, false);
    static Tuple<Class[], Boolean> teleports = new Tuple<>(new Class[]{S08PacketPlayerPosLook.class, S39PacketPlayerAbilities.class, S09PacketHeldItemChange.class}, false);
    static Tuple<Class[], Boolean> players = new Tuple<>(new Class[]{S13PacketDestroyEntities.class, S14PacketEntity.class, S14PacketEntity.S16PacketEntityLook.class, S14PacketEntity.S15PacketEntityRelMove.class, S14PacketEntity.S17PacketEntityLookMove.class, S18PacketEntityTeleport.class, S20PacketEntityProperties.class, S19PacketEntityHeadLook.class}, false);
    static Tuple<Class[], Boolean> blink = new Tuple<>(new Class[]{C02PacketUseEntity.class, C0DPacketCloseWindow.class, C0EPacketClickWindow.class, C0CPacketInput.class, C0BPacketEntityAction.class, C08PacketPlayerBlockPlacement.class, C07PacketPlayerDigging.class, C09PacketHeldItemChange.class, C13PacketPlayerAbilities.class, C15PacketClientSettings.class, C16PacketClientStatus.class, C17PacketCustomPayload.class, C18PacketSpectate.class, C19PacketResourcePackStatus.class, C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class, C0APacketAnimation.class}, false);
    static Tuple<Class[], Boolean> movement = new Tuple<>(new Class[]{C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class}, false);

    public static Tuple<Class[], Boolean>[] types = new Tuple[]{regular, velocity, teleports, players, blink, movement};

    @EventLink
    public final Listener<PacketSendEvent> send = event -> event.setCancelled(onPacket(event.getPacket(), event).isCancelled());

    @EventLink
    public final Listener<PacketReceiveEvent> receive = event -> event.setCancelled(onPacket(event.getPacket(), event).isCancelled());

    public CancellableEvent onPacket(Packet<?> packet, CancellableEvent event) {
        if (!event.isCancelled() && enabled && Arrays.stream(types).anyMatch(tuple -> tuple.getSecond() && Arrays.stream(tuple.getFirst()).anyMatch(regularpacket -> regularpacket == packet.getClass()))) {
            event.setCancelled();
            packets.add(new PacketUtils.TimedPacket(packet));
        }

        return event;
    }

    public static void dispatch() {
        if (!packets.isEmpty()) {
            // Stops the packets from being called twice
            boolean enabled = LagProcess.enabled;
            LagProcess.enabled = false;
            packets.forEach(timedPacket -> PacketUtils.queue(timedPacket.getPacket()));
            LagProcess.enabled = enabled;
            packets.clear();
        }
    }

    public static void disable() {
        enabled = false;
        enabledTimer.setTime(enabledTimer.getTime() - 999999999);
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> dispatch();

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if(event.isPre()) return;
        if (!(enabled = !enabledTimer.hasTimeElapsed(100) && !(mc.currentScreen instanceof GuiDownloadTerrain))) {
            dispatch();
        } else {
            // Stops the packets from being called twice
            enabled = false;

            packets.forEach(packet -> {
                if (packet.getTime() + amount < System.currentTimeMillis()) {
                    PacketUtils.queue(packet.getPacket());
                    packets.remove(packet);
                }
            });

            enabled = true;
        }
    };

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players) {
        spoof(amount, regular, velocity, teleports, players, false);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink, boolean movement) {
        enabledTimer.reset();

        LagProcess.regular.setSecond(regular);
        LagProcess.velocity.setSecond(velocity);
        LagProcess.teleports.setSecond(teleports);
        LagProcess.players.setSecond(players);
        LagProcess.blink.setSecond(blink);
        LagProcess.movement.setSecond(movement);
        LagProcess.amount = amount;
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink) {
        spoof(amount, regular, velocity, teleports, players, blink, false);
    }

    public static void blink() {
        spoof(9999999, true, false, false, false, true);
    }
}
