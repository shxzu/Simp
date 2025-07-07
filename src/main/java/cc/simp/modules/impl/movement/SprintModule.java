package cc.simp.modules.impl.movement;

import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Sprint", category = ModuleCategory.MOVEMENT)
public final class SprintModule extends Module {

    public final Property<Boolean> omniProperty = new Property<>("Omni", false);

    private int groundTicks;

    public SprintModule() {
        toggle();
    }

    @EventLink
    public final Listener<MoveEvent> moveEventListener = event -> {
        final boolean canSprint = MovementUtils.canSprint(omniProperty.getValue());
        mc.thePlayer.setSprinting(canSprint);
    };
}
