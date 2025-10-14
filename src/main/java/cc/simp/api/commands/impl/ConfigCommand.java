package cc.simp.api.commands.impl;

import cc.simp.Simp;
import cc.simp.api.commands.Command;
import cc.simp.api.config.Config;
import cc.simp.utils.client.Logger;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Saves or Loads Configs.", ".config save/load/remove [config] or .config list", "c");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        String command = Simp.requireNonNull(args[0]);

        if (command.equalsIgnoreCase("save")) {
            if (args.length != 2) {
                usage();
                return;
            }
            String name = Simp.requireNonNull(args[1]);
            Simp.INSTANCE.getConfigManager().saveConfig(name);
            Logger.chatPrint("Config '" + name + "' has been saved.");
        } else if (command.equalsIgnoreCase("load")) {
            if (args.length != 2) {
                usage();
                return;
            }
            String name = Simp.requireNonNull(args[1]);
            Simp.INSTANCE.getConfigManager().loadConfig(name);
            Logger.chatPrint("Config '" + name + "' has been loaded.");
        } else if (command.equalsIgnoreCase("remove")) {
            if (args.length != 2) {
                usage();
                return;
            }
            String name = Simp.requireNonNull(args[1]);
            Simp.INSTANCE.getConfigManager().deleteConfig(name);
            Logger.chatPrint("Config '" + name + "' has been removed.");
        } else if (command.equalsIgnoreCase("list")) {
            if (args.length != 1) {
                usage();
                return;
            }
            Logger.chatPrint("Available Configs:");
            for (Config config : Simp.INSTANCE.getConfigManager().getElements())
                Logger.chatPrint(config.getName());
        } else {
            usage();
        }
    }
}