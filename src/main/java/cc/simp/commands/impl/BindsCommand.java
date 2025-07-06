package cc.simp.commands.impl;

import cc.simp.Simp;
import cc.simp.commands.Command;

public class BindsCommand extends Command {
    public BindsCommand() {
        super("binds", "Lists binds", ".binds .bs", "bs");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder sb = new StringBuilder();

        Simp.INSTANCE.getModuleManager().getModules().forEach(module -> {
            int key = module.getKey();
            if (key != 0) {
                String keyName = org.lwjgl.input.Keyboard.getKeyName(key);
                sb.append("\n")
                        .append(module.getLabel())
                        .append(" | ")
                        .append(keyName);
            }
        });

        sendChatWithPrefix(sb.toString());
    }
}
