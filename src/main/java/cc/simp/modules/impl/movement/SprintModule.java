package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.Property;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.util.MathHelper;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Sprint", category = ModuleCategory.MOVEMENT)
public final class SprintModule extends Module {

    public static final Property<Boolean> omniProperty = new Property<>("Omni", false);

    public SprintModule() {
        toggle();
    }

    @EventLink
    public final Listener<MoveEvent> moveEventListener = event -> {
        final boolean canSprint = MovementUtils.canSprint(omniProperty.getValue());
        if (MovementUtils.isMoving()) {
            if (!Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
                float currentYaw = mc.thePlayer.rotationYaw;
                float yawDifference = Math.abs(MathHelper.wrapAngleTo180_float(currentYaw - Simp.INSTANCE.getRotationManager().getClientYaw()));
                if (!omniProperty.getValue()) {
                    mc.gameSettings.keyBindSprint.setPressed(!(yawDifference > 30));
                } else {
                    if (!(yawDifference > 30)) {
                        mc.thePlayer.setSprinting(canSprint);
                    } else {
                        mc.thePlayer.setSprinting(false);
                    }
                }
            }
        }
    };
}
