package cc.simp.commands.impl;

import cc.simp.Simp;
import cc.simp.commands.Command;
import cc.simp.modules.Module;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Binds a module to a certain key", ".bind or .b [module] [key]", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            usage();
        } else {
            try {
                StringBuilder moduleNameBuilder = new StringBuilder();
                for (int i = 0; i < args.length - 1; i++) {
                    moduleNameBuilder.append(args[i]);
                    if (i < args.length - 2) moduleNameBuilder.append(" ");
                }
                String moduleName = moduleNameBuilder.toString();
                Module module = Simp.INSTANCE.getModuleManager().getModule(moduleName);
                module.setKey(Keyboard.getKeyIndex(args[args.length - 1].toUpperCase()));
                sendChatWithPrefix("Set keybind for " + module.getLabel() + " to " + args[args.length - 1].toUpperCase());
            } catch (Exception e) {
                usage();
            }
        }
    }

}