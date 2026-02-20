package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Spider", category = ModuleCategory.MOVEMENT)
public final class SpiderModule extends Module {

    private static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);

    public enum Mode {
        Vanilla,
        Verus
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        switch (mode.getValue()) {
            case Verus -> {
                if (mc.thePlayer.isCollidedHorizontally) {
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        mc.thePlayer.jump();
                    }
                }
            }
            case Vanilla -> {
                if (mc.thePlayer.isCollidedHorizontally) {
                    mc.thePlayer.jump();
                }
            }
        }
    };

}
