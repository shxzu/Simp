package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.Property;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Movement Fix", category = ModuleCategory.MOVEMENT)
public final class MovementFixModule extends Module {

    public static final Property<Boolean> killAuraProperty = new Property<>("Work on Kill Aura", true);
    public final Property<Boolean> scaffoldProperty = new Property<>("Work on Scaffold", true);

    @EventLink
    public final Listener<StrafeEvent> strafeEventListener = event -> {
        if (mc.thePlayer == null) {
            return;
        }
        if (scaffoldProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            if (Simp.INSTANCE.getRotationManager().isRotating()) {
                event.setCancelled();
                MovementUtils.silentRotationStrafe(event, Simp.INSTANCE.getRotationManager().getClientYaw());
            }
        }
        if(killAuraProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null) {
            if (Simp.INSTANCE.getRotationManager().isRotating()) {
                event.setCancelled();
                MovementUtils.silentRotationStrafe(event, Simp.INSTANCE.getRotationManager().getClientYaw());
            }
        }
    };
}
