package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.player.SprintEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.api.properties.Property;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Sprint", category = ModuleCategory.MOVEMENT)
public final class SprintModule extends Module {

    public static Property<Boolean> omni = new Property<>("Omni", false);

    public SprintModule() {
        toggle();
    }

    @EventLink
    public final Listener<SprintEvent> onSprintEvent = event -> {
        if (!event.isSprinting()) {
            if (MovementUtils.isMoving() && !omni.getValue()) {
                mc.gameSettings.keyBindSprint.setPressed(true);
            }
            if (omni.getValue()) {
                mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
                event.setSprinting(MovementUtils.canSprint(true));
            }
        }
    };
}
