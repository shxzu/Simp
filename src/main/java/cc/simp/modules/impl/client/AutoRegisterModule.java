package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.server.S02PacketChat;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Register", category = ModuleCategory.CLIENT)
public class AutoRegisterModule extends Module {

    private static final String[] REGISTER_KEYWORDS = {
            "/register",    // English
            "/registrar",   // Spanish/Portuguese
            "/reg",         // Short form
            "/зарег",       // Russian (zareg)
            "/rejestracja", // Polish
            "/cadastrar",   // Portuguese
            "/kayit",       // Turkish
            "/enregistrer"  // French
    };

    @EventLink
    public final Listener<PacketReceiveEvent> PacketReceiveEvent = event -> {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();
        String chatComponent = packetChat.getChatComponent().getUnformattedText().toLowerCase();

        for (String keyword : REGISTER_KEYWORDS) {
            if (chatComponent.contains(keyword.toLowerCase())) {
                mc.thePlayer.sendChatMessage(keyword + " amogus123 amogus123");
                break;
            }
        }
    };
}
