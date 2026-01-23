package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Web", category = ModuleCategory.MOVEMENT)
public final class NoWebModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);

    private enum Mode {
        Vanilla,
        Intave,
    }

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        setSuffix(mode.getValue().toString());
        switch (mode.getValue()) {
            case Vanilla -> {
                if (mc.thePlayer.isInWeb && MovementUtils.isMoving()) mc.thePlayer.isInWeb = false;
            }
            case Intave -> {
                if (mc.thePlayer.isInWeb && MovementUtils.isMoving() && mc.thePlayer.onGround) {
                    if (mc.thePlayer.ticksExisted % 3 == 0) {
                        MovementUtils.strafe(0.734);
                    } else {
                        mc.thePlayer.jump();
                        MovementUtils.strafe(0.346);
                    }
                }
            }
        }
    };
}
