package cc.simp.api.commands.impl;

import cc.simp.api.commands.Command;

import static cc.simp.utils.client.Logger.sendChat;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Shows all available commands", ".help", "h");
    }

    @Override
    public void execute(String[] args) {
        sendChatWithPrefix("§6§lSimp §f--- §6§lHelp");
        sendChat("");

        sendChat("§6.bind §7- Lets you bind modules to certain keys");
        sendChat("§6.clientname §7- Lets you change the Clientname to anything on the watermark");
        sendChat("§6.binds §7- Shows you the binds");
        sendChat("§6.toggle §7- Lets you toggle modules");
        sendChat("§6.config §7- Lets you load / save / delete configs");
        sendChat("§6.help §7- Shows all available commands");

        sendChat("");
        sendChat("§7Use §6.command §7or §6.c §7to execute commands");
    }
}