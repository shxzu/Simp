package cc.simp.processes;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.modules.impl.client.ClickInterfaceModule;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

public class BackgroundProcess {

    private Timer cfgTimer = new Timer();

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (this.cfgTimer.hasTimeElapsed(30000, true)) Simp.INSTANCE.getConfigManager().saveConfig("default");
    };

}
