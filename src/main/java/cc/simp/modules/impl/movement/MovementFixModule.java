package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.event.impl.player.PlayerInputEvent;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.Property;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.RotationUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

@ModuleInfo(label = "Movement Fix", category = ModuleCategory.MOVEMENT)
public final class MovementFixModule extends Module {

    public final Property<Boolean> killAuraProperty = new Property<>("Work on Kill Aura", true);
    public final Property<Boolean> strictProperty = new Property<>("Strict on Kill Aura", true, killAuraProperty::getValue);
    public final Property<Boolean> scaffoldProperty = new Property<>("Work on Scaffold", true);

    @EventLink
    public final Listener<PlayerInputEvent> playerInputEventListener = event -> {
        if(killAuraProperty.getValue() && KillAuraModule.target != null && !strictProperty.getValue()) {
            MovementUtils.fixKillAuraMovement(event, Simp.INSTANCE.getRotationHandler().getServerYaw());
        }
    };
    @EventLink
    public final Listener<StrafeEvent> strafeEventListener = event -> {
        if(scaffoldProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            MovementUtils.fixScaffoldMovement(event);
        }
        if(killAuraProperty.getValue() && KillAuraModule.target != null && strictProperty.getValue()) {
            event.setYaw(Simp.INSTANCE.getRotationHandler().getServerYaw());
        }
    };
}
