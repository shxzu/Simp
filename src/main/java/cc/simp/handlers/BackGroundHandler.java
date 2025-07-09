package cc.simp.handlers;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

public class BackGroundHandler {

    private Timer cfgTimer = new Timer();

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if (this.cfgTimer.hasTimeElapsed(30000, true)) {
            Simp.INSTANCE.getConfigManager().saveConfig("default");
        }

    };

}
