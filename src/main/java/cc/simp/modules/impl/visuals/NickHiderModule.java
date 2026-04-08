package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Nick Hider", category = ModuleCategory.VISUALS)
public class NickHiderModule extends Module {

    private final Property<String> fakeNameProp = new Property<>("Fake Name", "x0lumie");

    private String fakeName = " ";

    @EventLink
    public Listener<PacketReceiveEvent> onPacketReceive = event -> {
        if (mc.thePlayer == null) return;

        final Packet<?> packet = event.getPacket();
        if (packet instanceof S02PacketChat) {
            final S02PacketChat wrapper = ((S02PacketChat) packet);
            final IChatComponent iChatComponent = wrapper.getChatComponent();

            if (iChatComponent instanceof ChatComponentText) {
                final String newMessage = iChatComponent.getFormattedText().replace(
                        mc.thePlayer.getGameProfile().getName(), fakeName);

                final ChatComponentText newChatComponentText = new ChatComponentText(newMessage);

                wrapper.setChatComponent(newChatComponentText);
            }

            event.setPacket(wrapper);
        }
    };

    @EventLink
    public Listener<Render2DEvent> onRender2D = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        fakeName = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + fakeNameProp.getValue();

        for (final NetworkPlayerInfo player : mc.getNetHandler().getPlayerInfoMap()) {
            if (player.getGameProfile().getName().length() < 3 || player.getDisplayName() == null) continue;
            player.setDisplayName(new ChatComponentText(player.getDisplayName().getFormattedText().replaceFirst(mc.thePlayer.getGameProfile().getName(), fakeName)));
        }
    };
}