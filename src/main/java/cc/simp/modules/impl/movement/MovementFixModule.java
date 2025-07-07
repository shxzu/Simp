package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.event.impl.player.PlayerInputEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.property.Property;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.RotationUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Movement Fix", category = ModuleCategory.MOVEMENT)
public final class MovementFixModule extends Module {

    public final Property<Boolean> killAuraProperty = new Property<>("Work on Kill Aura", true);

    @EventLink
    public final Listener<PlayerInputEvent> playerInputEventListener = event -> {
        if(killAuraProperty.getValue() && KillAuraModule.target != null) {
            MovementUtils.fixMovement(event, RotationUtils.serverYaw);
        }
    };
}
