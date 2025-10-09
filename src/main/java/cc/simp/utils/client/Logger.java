package cc.simp.utils.client;

import cc.simp.utils.Util;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Logger extends Util {
    public static void chatPrint(boolean prefix, String message) {
        if (mc.thePlayer != null) {
            if (prefix) message = "Simp | " + message;
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    public static void chatError(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("Error | " + message));
        }
    }

    public static void chatPrint(String prefix, EnumChatFormatting color, String message) {
        if (mc.thePlayer != null) {
            message = "§7[§" + color.formattingCode + "§l" + prefix.toUpperCase() + "§r§7]§r §" + color.formattingCode + message;
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    public static void chatPrint(Object o) {
        chatPrint(true, String.valueOf(o));
    }

    public static void sendChat(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message);
        }
    }
}
