package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

@ModuleInfo(label = "Keep Sprint", category = ModuleCategory.COMBAT)
public final class KeepSprintModule extends Module {

    @EventLink
    public final Listener<HitSlowDownEvent> hitSlowDownEventListener = e -> {
            e.setSprint(true);
            e.setSlowDown(1.0);
    };
}
