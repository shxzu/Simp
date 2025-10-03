package cc.simp;

import cc.simp.api.config.ConfigManager;
import cc.simp.api.events.Event;
import cc.simp.api.events.impl.game.ClientStartupEvent;
import cc.simp.modules.ModuleManager;
import cc.simp.utils.client.BuildType;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.bus.impl.EventBus;
import lombok.Getter;

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

    private Simp() {
        getEventBus().subscribe(this);
    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {
        moduleManager = new ModuleManager();
        moduleManager.postInit();
        configManager = new ConfigManager();
    };

    public EventBus<Event> getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus<>();
        }

        return eventBus;
    }

}
