package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.Property;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Field;
import java.util.Collection;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Nick Hider", category = ModuleCategory.VISUALS)
public class NickHiderModule extends Module {

    private final String fakeName = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "You";
    private final String fakeServerIP = EnumChatFormatting.YELLOW + "github.com/shxzu/Simp";

    public static final Property<Boolean> serverIP = new Property<>("ServerIP", true);

    @EventLink
    public Listener<PacketReceiveEvent> onPacketReceive = event -> {
        if (mc.thePlayer == null) return;

        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getFormattedText();


            if (message.contains(mc.thePlayer.getName())) {
                String newMessage = message.replace(mc.thePlayer.getName(), fakeName);
                try {
                    Field chatComponentField = S02PacketChat.class.getDeclaredField("chatComponent");
                    chatComponentField.setAccessible(true);

                    Object newChatComponent = net.minecraft.util.IChatComponent.Serializer.jsonToComponent(
                            net.minecraft.util.IChatComponent.Serializer.componentToJson(
                                    new net.minecraft.util.ChatComponentText(newMessage)
                            )
                    );
                    chatComponentField.set(packet, newChatComponent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (serverIP.getValue()) {
                String newMessage = message;
                newMessage = newMessage.replace("www.MineBlaze.net", fakeServerIP);
                newMessage = newMessage.replace("blocksmc.com", fakeServerIP);
                newMessage = newMessage.replace("Hypixel.net", fakeServerIP);
                newMessage = newMessage.replace("www.CheatMine.fun", fakeServerIP);
                newMessage = newMessage.replace("dexland.ru", fakeServerIP);
                newMessage = newMessage.replace("mineblaze.ru", fakeServerIP);
                newMessage = newMessage.replace("BlocksMC.com", fakeServerIP);
                newMessage = newMessage.replace("hypixel.net", fakeServerIP);
                newMessage = newMessage.replace("cheatmine.fun", fakeServerIP);
                newMessage = newMessage.replace("mineblaze.net", fakeServerIP);

                if (!newMessage.equals(message)) {
                    try {
                        Field chatComponentField = S02PacketChat.class.getDeclaredField("chatComponent");
                        chatComponentField.setAccessible(true);

                        Object newChatComponent = net.minecraft.util.IChatComponent.Serializer.jsonToComponent(
                                net.minecraft.util.IChatComponent.Serializer.componentToJson(
                                        new net.minecraft.util.ChatComponentText(newMessage)
                                )
                        );
                        chatComponentField.set(packet, newChatComponent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @EventLink
    public Listener<Render2DEvent> onRender2D = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            Collection<NetworkPlayerInfo> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();

            for (NetworkPlayerInfo info : playerInfoMap) {
                if (info.getGameProfile().getName().equals(mc.thePlayer.getName())) {
                    try {
                        try {
                            Field displayNameField = NetworkPlayerInfo.class.getDeclaredField("displayName");
                            displayNameField.setAccessible(true);
                            displayNameField.set(info, fakeName);
                        } catch (NoSuchFieldException e) {
                            Field displayNameField = NetworkPlayerInfo.class.getDeclaredField("field_178867_g");
                            displayNameField.setAccessible(true);
                            displayNameField.set(info, fakeName);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    };
}