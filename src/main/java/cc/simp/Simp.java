package cc.simp;

import cc.simp.commands.CommandHandler;
import cc.simp.commands.impl.BindCommand;
import cc.simp.commands.impl.HideCommand;
import cc.simp.commands.impl.ToggleCommand;
import cc.simp.event.Event;
import cc.simp.event.impl.KeyPressEvent;
import cc.simp.event.impl.game.ClientStartupEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleManager;
import cc.simp.ui.click.window.WindowClickGUI;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.bus.impl.EventBus;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.util.Arrays;

public class Simp {

    //Simp Client Info
    public static final Simp INSTANCE = new Simp();
    public static final String NAME = "Simp";
    public static final String BUILD = "070625";

    //Client Background Stuff
    private ModuleManager moduleManager;
    private EventBus<Event> eventBus;
    private CommandHandler commandHandler;
    private WindowClickGUI winClickGUI;

    public static void start() {
        Display.setTitle(NAME + " " + BUILD);
    }

    private Simp() {
        // Subscribe Main Class To The Event Bus To Process Events.
        getEventBus().subscribe(this);

    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {

        // Module Manager
        moduleManager = new ModuleManager();
        moduleManager.postInit();

        //Command Handler
        commandHandler = new CommandHandler();
        commandHandler.commands.addAll(Arrays.asList(
                new BindCommand(),
                new ToggleCommand(),
                new HideCommand()
        ));
        getEventBus().subscribe(commandHandler);

    };

    @EventLink
    private final Listener<KeyPressEvent> keyPressEventListener = event -> {
        if (event.getKey() == Keyboard.KEY_RSHIFT) {
            if (winClickGUI == null) {
                winClickGUI = new WindowClickGUI();
            }
            Minecraft.getMinecraft().displayGuiScreen(winClickGUI);
        }
    };

    public EventBus<Event> getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus<>();
        }

        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandHandler getCommandHandler() {
       return commandHandler;
    }

}
