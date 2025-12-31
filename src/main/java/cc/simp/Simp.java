package cc.simp;

import cc.simp.api.commands.CommandHandler;
import cc.simp.api.commands.impl.*;
import cc.simp.api.config.ConfigManager;
import cc.simp.api.events.Event;
import cc.simp.api.events.impl.game.ClientStartupEvent;
import cc.simp.api.events.impl.game.KeyPressEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.interfaces.click.ClickInterface;
import cc.simp.modules.ModuleManager;
import cc.simp.modules.impl.client.ClientSettingsModule;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.processes.*;
import cc.simp.utils.client.BuildType;
import de.florianmichael.viamcp.ViaMCP;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.bus.impl.EventBus;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;

public class Simp {
    public static final Simp INSTANCE = new Simp();
    public static final String NAME = "Simp";
    public static final String BUILD = BuildType.RELEASE.getName();
    public static final String VERSION = "1.0 " + BUILD;
    public static final String FULL = NAME + " " + VERSION;

    private EventBus<Event> eventBus;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ConfigManager configManager;
    @Getter
    private CommandHandler commandHandler;
    private BackgroundProcess backgroundProcess;
    private RotationProcess rotationProcess;
    private ColorProcess colorProcess;
    private ClickInterface clickInterface;
    private LagProcess lagProcess;
    private BadPacketsProcess badPacketsProcess;
    private TargetSelectionProcess targetSelectionProcess;
    @Getter
    private static long startTime;

    private Simp() {
        getEventBus().subscribe(this);
    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {
        startTime = System.currentTimeMillis();
        moduleManager = new ModuleManager();
        moduleManager.postInit();
        configManager = new ConfigManager();
        getEventBus().subscribe(configManager);
        backgroundProcess = new BackgroundProcess();
        getEventBus().subscribe(backgroundProcess);
        rotationProcess = new RotationProcess();
        getEventBus().subscribe(rotationProcess);
        colorProcess = new ColorProcess();
        getEventBus().subscribe(colorProcess);
        configManager.loadConfig("default");
        lagProcess = new LagProcess();
        getEventBus().subscribe(lagProcess);
        badPacketsProcess = new BadPacketsProcess();
        getEventBus().subscribe(badPacketsProcess);
        targetSelectionProcess = new TargetSelectionProcess();
        getEventBus().subscribe(targetSelectionProcess);
        commandHandler = new CommandHandler();
        commandHandler.commands.addAll(Arrays.asList(
                new BindCommand(),
                new ClientNameCommand(),
                new BindsCommand(),
                new ToggleCommand(),
                new ConfigCommand(),
                new HideCommand(),
                new HelpCommand()
        ));
        getEventBus().subscribe(commandHandler);

        // I hate the way minecraft handles rotations when the player is null so much -shxzu

        if (moduleManager.getModule(KillAuraModule.class).isEnabled()) {
            moduleManager.getModule(KillAuraModule.class).setEnabled(false);
        }

        if (moduleManager.getModule(ScaffoldModule.class).isEnabled()) {
            moduleManager.getModule(ScaffoldModule.class).setEnabled(false);
        }

        if (moduleManager.getModule(ScaffoldRecodeModule.class).isEnabled()) {
            moduleManager.getModule(ScaffoldRecodeModule.class).setEnabled(false);
        }

        // ViaMCP!!!

        try {
            ViaMCP.create();
            ViaMCP.INSTANCE.initAsyncSlider();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    };

    @EventLink
    public final Listener<KeyPressEvent> keyPressEventListener = e -> {
        if (e.getKey() == Keyboard.KEY_RSHIFT) {
            if (clickInterface == null) {
                clickInterface = new ClickInterface();
            }
            Minecraft.getMinecraft().displayGuiScreen(clickInterface);
        }
    };

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        if(!Simp.INSTANCE.getModuleManager().getModule(ClientSettingsModule.class).isEnabled()) Simp.INSTANCE.getModuleManager().getModule(ClientSettingsModule.class).setEnabled(true);

        String currentFont = FontProcess.getCurrentFont().getNameFontTTF().toLowerCase();
        String desiredFont = ClientSettingsModule.font.getValue().toString().toLowerCase();

        if (!currentFont.equals(desiredFont)) {
            FontProcess.setCurrentFont(desiredFont);
        }
    };


    public EventBus<Event> getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus<>();
        }

        return eventBus;
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        return obj;
    }
}
