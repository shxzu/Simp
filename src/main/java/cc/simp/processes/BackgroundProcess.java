package cc.simp.processes;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.modules.impl.client.FpsEnhancerModule;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.ViaMCPFixes;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

public class BackgroundProcess {

    private Timer cfgTimer = new Timer();

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (this.cfgTimer.hasTimeElapsed(30000, true)) {
            if (Simp.INSTANCE.getModuleManager().getModule(FpsEnhancerModule.class).isEnabled() && FpsEnhancerModule.disableConfigAutoSave.getValue()) return;
            Simp.INSTANCE.getConfigManager().saveConfig("default");
            Simp.INSTANCE.getBindsConfig().saveToFile();
        }

        DraggingProcess.update();
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        ViaMCPFixes.initialized = false;
    };
}
