package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Spin Bot", category = ModuleCategory.PLAYER)
public final class SpinBotModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Fake);
    private final Property<Boolean> movementFix = new Property<>("Movement Fix", false, () -> mode.getValue() == Mode.Server);

    private int yaw;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        yaw += 10 % 360;
        switch (mode.getValue()) {
            case Fake -> mc.thePlayer.renderYawOffset = yaw;
            case Server ->
                    RotationProcess.setRotations(new Vector2f(yaw, mc.thePlayer.rotationPitch), 10, movementFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
            case Player -> mc.thePlayer.rotationYaw = yaw;
        }
    };

    public enum Mode {
        Fake,
        Server,
        Player
    }
}
