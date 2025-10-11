package cc.simp.modules.impl.movement;

import cc.simp.Simp;
import cc.simp.api.events.impl.player.SprintEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.api.properties.Property;
import cc.simp.modules.impl.player.ScaffoldWalkModule;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.util.MathHelper;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Sprint", category = ModuleCategory.MOVEMENT)
public final class SprintModule extends Module {

    public static Property<Boolean> omni = new Property<>("Omni", false);
    public static Property<Boolean> rot = new Property<>("Rotation Check", true);

    public SprintModule() {
        toggle();
    }

    @EventLink
    public final Listener<SprintEvent> onSprintEvent = event -> {
        if (!event.isSprinting()) {
            if (MovementUtils.isMoving() && !omni.getValue()) {
                mc.gameSettings.keyBindSprint.setPressed(true);
            }
            if (MovementUtils.isMoving() && omni.getValue()) {
                mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
                event.setSprinting(MovementUtils.canSprint(true));
            }
        }

        if (event.isSprinting()) {
            if (Simp.INSTANCE.getModuleManager().getModule(ScaffoldWalkModule.class).isEnabled() && ScaffoldWalkModule.sprint.getValue()) return;
            if (Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(RotationProcess.rotations.x)) > 90 && rot.getValue()) {
                mc.gameSettings.keyBindSprint.setPressed(false);
                mc.thePlayer.setSprinting(false);
                event.setSprinting(false);
            }
        }
    };
}
