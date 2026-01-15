package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Jump Delay", category = ModuleCategory.MOVEMENT)
public final class NoJumpDelayModule extends Module {
    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        mc.thePlayer.jumpTicks = 0;
    };
}
