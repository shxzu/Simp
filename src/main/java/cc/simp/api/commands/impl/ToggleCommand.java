package cc.simp.api.commands.impl;

import cc.simp.Simp;
import cc.simp.api.commands.Command;
import cc.simp.modules.Module;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "Toggles a module", ".t or .toggle [module]", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            usage();
        } else {
            try {
                String moduleName = String.join(" ", args);
                Module module = Simp.INSTANCE.getModuleManager().getModule(moduleName);
                if (module != null) {
                	module.toggle();
                    sendChatWithPrefix("Toggled " + moduleName + "!");
                } else {
                    sendChatWithPrefix("Cannot find module \"" + moduleName + "\"");
                }
          
            } catch (Exception e) {
                usage();
            }
        }
    }

}