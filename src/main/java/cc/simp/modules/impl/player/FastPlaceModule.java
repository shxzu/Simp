package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Fast Place", category = ModuleCategory.PLAYER)
public final class FastPlaceModule extends Module {

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        if (mc.theWorld != null && mc.thePlayer != null) {
            if (!mc.inGameHasFocus) return;

            mc.rightClickDelayTimer = 0;
        }
    };
}
