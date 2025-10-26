package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.JumpEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.FlightModule;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PlayerUtils;
import cc.simp.utils.mc.RotationUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vector3d;

import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Target Strafe", category = ModuleCategory.COMBAT)
public final class TargetStrafeModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 1, 0.2, 6, 0.1);
    public final Property<Boolean> holdJump = new Property<>("Hold Jump", false);

    private float yaw;
    private Entity target;
    private boolean left, colliding;
    private boolean active;

    @EventLink(value = Priorities.HIGH)
    public final Listener<JumpEvent> onJump = event -> {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    };

    @EventLink(value = Priorities.HIGH)
    public final Listener<StrafeEvent> onStrafe = event -> {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    };

    @EventLink(value = Priorities.HIGH)
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        // Disable if scaffold is enabled
        ScaffoldModule scaffold = Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        KillAuraModule killaura = Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);

        if (scaffold == null || scaffold.isEnabled() || killaura == null || !killaura.isEnabled()) {
            active = false;
            return;
        }

        active = true;

        /*
         * Getting targets and selecting the nearest one
         */
        Module speed = Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class);
        Module test = null;
        Module flight = Simp.INSTANCE.getModuleManager().getModule(FlightModule.class);

        if (holdJump.getValue() && !mc.gameSettings.keyBindJump.isKeyDown() || !(mc.gameSettings.keyBindForward.isKeyDown() &&
                ((flight != null && flight.isEnabled()) || ((speed != null && speed.isEnabled()) || (test != null && test.isEnabled()))))) {
            target = null;
            return;
        }

        final List<Entity> targets = killaura.targetList;

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (mc.thePlayer.isCollidedHorizontally || !PlayerUtils.isBlockUnder(5, false)) {
            if (!colliding) {
                MovementUtils.strafe();
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        target = targets.get(0);

        if (target == null) {
            return;
        }

        float yaw = RotationUtils.calculate(target).getX() + (90 + 45) * (left ? -1 : 1);

        final double range = this.range.getValue().doubleValue() + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * range + target.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * range + target.posZ;

        yaw = RotationUtils.calculate(new Vector3d(posX, target.posY, posZ)).getX();

        this.yaw = yaw;
        mc.thePlayer.movementYaw = this.yaw;
    };
}
