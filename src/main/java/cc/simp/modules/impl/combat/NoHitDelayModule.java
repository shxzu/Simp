package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Hit Delay", category = ModuleCategory.COMBAT)
public final class NoHitDelayModule extends Module {

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        if (mc.theWorld != null && mc.thePlayer != null) {
            if (!mc.inGameHasFocus) return;

            mc.leftClickCounter = 0;
        }
    };
}