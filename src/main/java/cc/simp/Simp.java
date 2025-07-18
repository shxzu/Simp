package cc.simp;

import cc.simp.commands.CommandHandler;
import cc.simp.commands.impl.*;
import cc.simp.config.ConfigManager;
import cc.simp.event.Event;
import cc.simp.event.impl.KeyPressEvent;
import cc.simp.event.impl.game.ClientStartupEvent;
import cc.simp.managers.BackgroundManager;
import cc.simp.managers.RotationManager;
import cc.simp.modules.ModuleManager;
import cc.simp.modules.impl.client.ClickGUIModule;
import cc.simp.ui.click.astolfo.AstolfoClickGUI;
import cc.simp.ui.click.window.WindowClickGUI;
import cc.simp.utils.font.FontManager;
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
    public static final String BUILD = "071725";

    //Client Background Stuff
    private ModuleManager moduleManager;
    private EventBus<Event> eventBus;
    private CommandHandler commandHandler;
    private static ConfigManager configManager;
    private RotationManager rotationManager;
    private WindowClickGUI winClickGUI;
    private AstolfoClickGUI astolfoClickGUI;
    private BackgroundManager backgroundManager;

    public static void start() {
        Display.setTitle(NAME + " " + BUILD);
    }

    private Simp() {
        // Subscribe Main Class To The Event Bus To Process Events.
        getEventBus().subscribe(this);

    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {

        // Font Manager
        FontManager.initTextures();

        // Module Manager
        moduleManager = new ModuleManager();
        moduleManager.postInit();

        //Command Handler
        commandHandler = new CommandHandler();
        commandHandler.commands.addAll(Arrays.asList(
                new BindCommand(),
                new BindsCommand(),
                new ToggleCommand(),
                new ConfigCommand(),
                new HideCommand()
        ));
        getEventBus().subscribe(commandHandler);

        // Config Manager
        configManager = new ConfigManager();

        // Rotation Handler
        rotationManager = new RotationManager();
        getEventBus().subscribe(rotationManager);

        configManager.loadConfig("default");

        // Background Handler
        backgroundManager = new BackgroundManager();
        getEventBus().subscribe(backgroundManager);

    };

    @EventLink
    private final Listener<KeyPressEvent> keyPressEventListener = event -> {
        if(ClickGUIModule.clickGuiStyleProperty.getValue() == ClickGUIModule.ClickGUIStyle.SIMP) {
            if (event.getKey() == Keyboard.KEY_RSHIFT) {
                if (winClickGUI == null) {
                    winClickGUI = new WindowClickGUI();
                }
                Minecraft.getMinecraft().displayGuiScreen(winClickGUI);
            }
        } else if(ClickGUIModule.clickGuiStyleProperty.getValue() == ClickGUIModule.ClickGUIStyle.ASTOLFO) {
            if (event.getKey() == Keyboard.KEY_RSHIFT) {
                if (astolfoClickGUI == null) {
                    astolfoClickGUI = new AstolfoClickGUI();
                }
                Minecraft.getMinecraft().displayGuiScreen(astolfoClickGUI);
            }
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

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public RotationManager getRotationManager() {
        return rotationManager;
    }

    public BackgroundManager getBackgroundManager() {
        return backgroundManager;
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        return obj;
    }

}
