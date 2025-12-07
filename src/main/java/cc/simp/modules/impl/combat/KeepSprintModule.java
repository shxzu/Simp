package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Keep Sprint", category = ModuleCategory.COMBAT)
public final class KeepSprintModule extends Module {

    public static final Property<Boolean> onlyInAirSprint = new Property<>("Keep Sprint Only In Air", false);

    @EventLink
    public final Listener<HitSlowDownEvent> hitSlowDownEventListener = e -> {
        if (!onlyInAirSprint.getValue() || !mc.thePlayer.onGround) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
    };
}
