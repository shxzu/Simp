package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Aim Assist", category = ModuleCategory.COMBAT)
public final class AimAssistModule extends Module {

    private final NumberProperty searchRange = new NumberProperty("Search Range", 4.0, 1.0, 8.0, 0.1);
    private final Property<Boolean> onlyOnClick = new Property<>("Only On Click", true);
    private final NumberProperty resetTime = new NumberProperty("Reset Time", 500.0, () -> onlyOnClick.getValue(), 0.0, 1000.0, 1.0);
    private final Property<Boolean> teamCheck = new Property<>("Team Check", false);
    private final ModeProperty<RotationMode> rotationMode = new ModeProperty<>("Rotation Mode", RotationMode.Server);
    private final NumberProperty horizontalSpeed = new NumberProperty("Horizontal Speed", 3.5, () -> rotationMode.getValue() == RotationMode.Player, 0.1, 10.0, 0.1);
    private final NumberProperty verticalSpeed = new NumberProperty("Vertical Speed", 3.0, () -> rotationMode.getValue() == RotationMode.Player, 0.1, 10.0, 0.1);
    private final NumberProperty maxAngle = new NumberProperty("Max Angle", 90.0, 10.0, 180.0, 1.0);
    private final Property<Boolean> smoothing = new Property<>("Smoothing", true);

    private EntityLivingBase target;
    private boolean angleCalled;
    private long lastClickTime;
    private float smoothYaw;
    private float smoothPitch;

    public enum RotationMode {
        Server,
        Player
    }

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        TargetSelectionProcess.setSeekRange(searchRange.getValue().floatValue());
        TargetSelectionProcess.setDontTargetTeams(teamCheck.getValue());

        angleCalled = true;

        if (onlyOnClick.getValue() && Mouse.isButtonDown(0) && angleCalled) {
            lastClickTime = System.currentTimeMillis();
        }

        if (!onlyOnClick.getValue() || System.currentTimeMillis() - lastClickTime <= resetTime.getValue()) {
            target = TargetSelectionProcess.getTarget();
        } else {
            target = null;
        }

        if (target == null) {
            return;
        }

        float[] rotations = getRotationsToEntity(target);

        // Check if target is within max angle
        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = rotations[1] - mc.thePlayer.rotationPitch;
        float angleDist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        if (angleDist > maxAngle.getValue().floatValue()) {
            return;
        }

        if (rotationMode.getValue() == RotationMode.Player) {
            // Apply smoothing for more natural movement
            float smoothFactor = smoothing.getValue() ? 0.6f : 1.0f;

            // Separate speeds for horizontal and vertical
            float yawSpeed = horizontalSpeed.getValue().floatValue();
            float pitchSpeed = verticalSpeed.getValue().floatValue();

            // Smooth the differences
            smoothYaw = smoothYaw * smoothFactor + yawDiff * (1.0f - smoothFactor);
            smoothPitch = smoothPitch * smoothFactor + pitchDiff * (1.0f - smoothFactor);

            // Apply rotation with separate speeds
            float yawChange = Math.signum(smoothYaw) * Math.min(Math.abs(smoothYaw), yawSpeed);
            float pitchChange = Math.signum(smoothPitch) * Math.min(Math.abs(smoothPitch), pitchSpeed);

            mc.thePlayer.rotationYaw += yawChange;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchChange, -90.0f, 90.0f);
        } else {
            // Server mode
            RotationProcess.setRotations(new Vector2f(rotations[0], rotations[1]), 5, MovementFix.NORMAL);
        }

        angleCalled = false;
    };

    private float[] getRotationsToEntity(EntityLivingBase entity) {
        double x = entity.posX - mc.thePlayer.posX;
        double y = entity.posY + entity.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = entity.posZ - mc.thePlayer.posZ;

        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / Math.PI));

        return new float[]{yaw, pitch};
    }

    @Override
    public void onEnable() {
        target = null;
        angleCalled = false;
        lastClickTime = 0;
        smoothYaw = 0;
        smoothPitch = 0;
    }
}