package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

@ModuleInfo(label = "ClientSpoofer", category = ModuleCategory.CLIENT)
public final class ClientSpooferModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.LUNAR);
    private final Property<String> customName = new Property<>("Custom Name", "A client", () -> mode.getValue() == Mode.CUSTOM);

    private enum Mode {
        LUNAR("Lunar"),
        FEATHER("Feather"),
        CUSTOM("Custom");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = e -> {
        if (e.getPacket() instanceof C17PacketCustomPayload) {
            String data;
            switch (mode.getValue()) {
                case LUNAR:
                    data = "lunarclient:v2.14.5-2411";
                    break;
                case FEATHER:
                    data = "Feather Forge";
                    break;
                case CUSTOM:
                    data = customName.getValue();
                    break;
                default:
                    data = "";
                    break;
            }

            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(data.getBytes()));
            C17PacketCustomPayload spoofedPacket = new C17PacketCustomPayload("MC|Brand", buffer);

            e.setPacket(spoofedPacket);
        }
    };
}