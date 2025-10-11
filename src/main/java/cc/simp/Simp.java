package cc.simp;

import cc.simp.api.config.ConfigManager;
import cc.simp.api.events.Event;
import cc.simp.api.events.impl.game.ClientStartupEvent;
import cc.simp.api.events.impl.game.KeyPressEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.interfaces.click.ClickInterface;
import cc.simp.modules.ModuleManager;
import cc.simp.modules.impl.client.ClickInterfaceModule;
import cc.simp.processes.BackgroundProcess;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.BuildType;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.bus.impl.EventBus;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class Simp {
    public static final Simp INSTANCE = new Simp();
    public static final String NAME = "Simp";
    public static final String BUILD = BuildType.ALPHA.getName();
    public static final String VERSION = "1.0 " + BUILD;
    public static final String FULL = NAME + " " + VERSION;

    private EventBus<Event> eventBus;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ConfigManager configManager;
    private BackgroundProcess backgroundProcess;
    private RotationProcess rotationProcess;
    private ColorProcess colorProcess;
    private ClickInterface clickInterface;

    private Simp() {
        getEventBus().subscribe(this);
    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {
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
    };

    public EventBus<Event> getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus<>();
        }

        return eventBus;
    }

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
        if(!Simp.INSTANCE.getModuleManager().getModule(ClickInterfaceModule.class).isEnabled()) Simp.INSTANCE.getModuleManager().getModule(ClickInterfaceModule.class).setEnabled(true);

        String currentFont = FontProcess.getCurrentFont().getNameFontTTF().toLowerCase();
        String desiredFont = ClickInterfaceModule.font.getValue().toString().toLowerCase();

        if (!currentFont.equals(desiredFont)) {
            FontProcess.setCurrentFont(desiredFont);
        }
    };

}
