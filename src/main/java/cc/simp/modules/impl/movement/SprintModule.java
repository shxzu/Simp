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

    private final Property<Boolean> omniProperty = new Property<>("Omni", true);

    private int groundTicks;

    public SprintModule() {
        toggle();
    }

    @EventLink
    public final Listener<SprintEvent> onSprintEvent = event -> {
        if (!event.isSprinting()) {
            final boolean canSprint = MovementUtils.canSprint(omniProperty.getValue());
            mc.thePlayer.setSprinting(canSprint);
            event.setSprinting(canSprint);
        }
    };
}
