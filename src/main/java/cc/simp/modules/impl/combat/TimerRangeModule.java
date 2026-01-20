package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.TargetSelectionProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Timer Range", category = ModuleCategory.COMBAT)
public final class TimerRangeModule extends Module {

    public static final NumberProperty delay = new NumberProperty("Delay", 80.0, 0.0, 200.0, 1.0);
    private final Property<Boolean> ka = new Property<>("Only On Kill Aura", false);

    private int tickableTick = 0;
    private float currentTimerSpeed = 1.0f;
    private boolean burstNextTick = false;
    private boolean slowNextTick = false;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        setSuffix(String.valueOf(delay.getValue()));

        if (ka.getValue() && !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled()) {
            if (currentTimerSpeed != 1f) {
                slowlyReturnToNormal();
            }
            return;
        }

        EntityLivingBase target = TargetSelectionProcess.getTarget();

        if (target != null && tickableTick == 0) {
            adjustTimerRange(target);
        } else if (currentTimerSpeed != 1f) {
            slowlyReturnToNormal();
            if (tickableTick > 0) {
                tickableTick--;
            }
        } else {
            if (tickableTick > 0) {
                tickableTick--;
            }
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (currentTimerSpeed != 1f) {
            slowlyReturnToNormal();
        }
        tickableTick = 0;
        currentTimerSpeed = 1.0f;
        burstNextTick = false;
        slowNextTick = false;
    };

    private void adjustTimerRange(EntityLivingBase target) {
        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (!burstNextTick && !slowNextTick && dist <= TargetSelectionProcess.getSeekRange() + 0.15 && dist > TargetSelectionProcess.getSeekRange() - 0.55) {
            burstNextTick = true;
        }

        if (burstNextTick) {
            currentTimerSpeed = 8f;
            mc.timer.timerSpeed =(currentTimerSpeed);
            burstNextTick = false;
            slowNextTick = true;
            return;
        }

        if (slowNextTick) {
            currentTimerSpeed = 0.1f;
            mc.timer.timerSpeed =(currentTimerSpeed);
            tickableTick = delay.getValue().intValue();
            slowNextTick = false;
            return;
        }

        slowlyReturnToNormal();
    }

    private void slowlyReturnToNormal() {
        if (Math.abs(currentTimerSpeed - 1.0f) > 0.01f) {
            currentTimerSpeed += (1.0f - currentTimerSpeed) * 0.5f;
            mc.timer.timerSpeed = currentTimerSpeed;
        } else {
            mc.timer.timerSpeed = 1.0f;
        }
    }

    @Override
    public void onDisable() {
        tickableTick = 0;
        currentTimerSpeed = 1.0f;
        burstNextTick = false;
        slowNextTick = false;
        super.onDisable();
    }
}
