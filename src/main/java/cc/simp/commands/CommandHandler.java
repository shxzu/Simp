package cc.simp.commands;

import cc.simp.utils.client.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// pasted from xiva. hell their both my clients anyway.

public class CommandHandler {
    public static String CHAT_PREFIX = ".";
    public List<Command> commands = new ArrayList<>();

    public boolean execute(String txt) {
        if (!txt.startsWith(CHAT_PREFIX)) return false;
        String[] arguments = txt.substring(1).split(" ");
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(arguments[0])
                    || Arrays.stream(command.getOtherPrefixes()).anyMatch(p -> p.equalsIgnoreCase(arguments[0]))) {
                command.execute(Arrays.copyOfRange(arguments, 1, arguments.length));
                return true;
            }
        }
        Logger.chatPrint("Command doesnt exist");
        return false;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public Command getCommand(Class<? extends Command> command) {
        return getCommands().stream().filter(com -> command == com.getClass()).findFirst().orElse(null);
    }

}
