package cc.simp.utils.mc;

import cc.simp.Simp;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.utils.Util;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import java.util.Objects;

public class PacketUtils extends Util {
    public static void correctBlockCount(PacketReceiveEvent event) {
        if (mc.thePlayer.isDead || true) return;

        final Packet<?> packet = event.getPacket();

        if (packet instanceof S2FPacketSetSlot) {
            final S2FPacketSetSlot wrapper = ((S2FPacketSetSlot) packet);

            if (wrapper.item() == null) {
                event.setCancelled();
            } else {
                try {
                    int slot = wrapper.id() - 36;
                    if (slot < 0) return;
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(slot);
                    final Item item = wrapper.item().getItem();

                    if ((itemStack == null && wrapper.item().stackSize <= 6 && item instanceof ItemBlock && !InventoryUtils.blacklist.contains(((ItemBlock) item).getBlock())) ||
                            itemStack != null && Math.abs(Objects.requireNonNull(itemStack).stackSize - wrapper.item().stackSize) <= 6 ||
                            wrapper.item() == null) {
                        event.setCancelled();
                    }
                } catch (ArrayIndexOutOfBoundsException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static void sendPacket(Packet<?> p) {
        PacketSendEvent sendEvent = new PacketSendEvent(p);
        Simp.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().sendPacket(p);
    }

    public static void sendSilentPacket(final Packet<?> p) {
        PacketSendEvent sendEvent = new PacketSendEvent(p);
        Simp.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().sendSilentPacket(p);
    }

    public static void receivePacket(final Packet p) {
        PacketReceiveEvent sendEvent = new PacketReceiveEvent(p);
        Simp.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().receivePacket(p);
    }

    public static void receiveSilentPacket(final Packet p) {
        PacketReceiveEvent sendEvent = new PacketReceiveEvent(p);
        Simp.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().receiveUnregisteredPacket(p);
    }
    
}
