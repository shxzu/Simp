package cc.simp.commands.impl;

import cc.simp.Simp;
import cc.simp.commands.Command;
import cc.simp.modules.Module;

public class HideCommand extends Command {

    public HideCommand() {
        super("hide", "Hides a module", ".h or .hide [module]", "h");
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
                	module.setHidden(!module.isHidden());
                    if(module.isHidden()) {
                    	sendChatWithPrefix("Hid " + module.getLabel() + "!");
                    } else {
                    	sendChatWithPrefix("Unhid " + module.getLabel() + "!");
                    }
                } else {
                    sendChatWithPrefix("Cannot find module \"" + moduleName + "\"");
                }
          
            } catch (Exception e) {
                usage();
            }
        }
    }

}