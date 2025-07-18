package cc.simp.modules.impl.combat;

import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Timer Range", category = ModuleCategory.COMBAT)
public class TimerRangeModule extends Module {

    public enum TimerMode {
        SLOW_DOWN, // Timer <= 1.0 (Uses slowedTimerSpeedProperty)
        SPEED_UP,   // Timer >= 1.0 (Uses spedUpTimerSpeedProperty)
        ADAPTIVE    // Timer adapts based on conditions (Interpolates between slowed and sped up)
    }

    // --- Properties ---\
    public EnumProperty<TimerMode> timerModeProperty = new EnumProperty<>("Mode", TimerMode.ADAPTIVE); // Default to Adaptive

    public DoubleProperty slowedTimerSpeedProperty = new DoubleProperty("Slowed Timer Speed", 0.3, () -> timerModeProperty.getValue() != TimerMode.SPEED_UP, 0.1, 1.0, 0.05);
    public DoubleProperty spedUpTimerSpeedProperty = new DoubleProperty("Sped Up Timer Speed", 1.5, () -> timerModeProperty.getValue() != TimerMode.SLOW_DOWN, 1.0, 2.0, 0.05);

    public DoubleProperty rangeToResetTimerProperty = new DoubleProperty("Range To Reset Timer", 2.75, 0.1, 6.0, 0.05); // Resets to 1.0 when target is this close
    public DoubleProperty verticalRangeProperty = new DoubleProperty("Vertical Range", 2.0, 0.1, 5.0, 0.1);

    public Property<Boolean> onlyMovingProperty = new Property<>("Only Moving", true);
    public Property<Boolean> onlyWhenAttackingProperty = new Property<>("Only When Attacking", false);

    public Property<Boolean> resetOnPlayerHurtProperty = new Property<>("Reset On Player Hurt", false);
    public Property<Boolean> resetOnTargetHurtTimeProperty = new Property<>("Reset On Target Hurt", false);
    public Property<Boolean> resetOnTargetDeadProperty = new Property<>("Reset On Target Dead", true);
    public Property<Boolean> resetOutOfKASRProperty = new Property<>("Reset Out Of KA Range", false); // Reset if target exits KillAura's range

    // --- Internal State ---
    private boolean timerActive = false;

    // --- Listeners ---
    @EventLink
    private final Listener<WorldLoadEvent> worldLoadEventEventListener = event -> {
        // Always reset timer speed and state on world load
        mc.timer.timerSpeed = 1.0f;
        timerActive = false;
    };

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        // Only run logic on pre-motion to decide timer state for the tick
        if (event.isPre()) {

            // --- Get Target and Distance (Optimized) ---
            EntityLivingBase currentTarget = KillAuraModule.target;
            double distanceToTarget = currentTarget != null ? mc.thePlayer.getDistanceToEntity(currentTarget) : Double.MAX_VALUE;
            double verticalDistanceToTarget = currentTarget != null ? Math.abs(mc.thePlayer.posY - currentTarget.posY) : Double.MAX_VALUE;

            // --- Conditions to Reset Timer ---
            boolean shouldResetTimer = false;

            // No target or target dead
            if (currentTarget == null) {
                shouldResetTimer = true;
            } else if (resetOnTargetDeadProperty.getValue() && currentTarget.isDead) {
                shouldResetTimer = true;
            }

            // Not moving
            if (onlyMovingProperty.getValue() && !MovementUtils.isMoving()) {
                shouldResetTimer = true;
            }

            // Target too close (priority reset)
            if (distanceToTarget <= rangeToResetTimerProperty.getValue()) {
                shouldResetTimer = true;
            }

            // Target out of KillAura's range (if option enabled)
            if (resetOutOfKASRProperty.getValue() && distanceToTarget > KillAuraModule.attackRangeProperty.getValue()) {
                shouldResetTimer = true;
            }

            // Player hurt
            if (resetOnPlayerHurtProperty.getValue() && mc.thePlayer.hurtTime > 0) {
                shouldResetTimer = true;
            }

            // Target hurt time
            if (resetOnTargetHurtTimeProperty.getValue() && currentTarget != null && currentTarget.hurtTime > 0) {
                shouldResetTimer = true;
            }


            // --- Apply Reset or Determine Timer Speed ---
            if (shouldResetTimer) {
                if (timerActive) { // Only set if changing
                    mc.timer.timerSpeed = 1.0f;
                    timerActive = false;
                }
            } else {
                // --- Conditions to Activate Timer ---
                boolean canActivateTimer = true;

                // Within KillAura's horizontal range
                if (distanceToTarget > KillAuraModule.attackRangeProperty.getValue()) {
                    canActivateTimer = false;
                }

                // Within vertical range
                if (verticalDistanceToTarget > verticalRangeProperty.getValue()) {
                    canActivateTimer = false;
                }

                // Only when KillAura is trying to attack
                if (onlyWhenAttackingProperty.getValue()) {
                    // This assumes currentTarget is within KA range property and not dead
                    if (currentTarget == null || distanceToTarget > KillAuraModule.attackRangeProperty.getValue() || currentTarget.isDead) {
                        canActivateTimer = false;
                    }
                }

                // Ensure target is NOT too close (already handled by shouldResetTimer, but for clarity)
                if (distanceToTarget <= rangeToResetTimerProperty.getValue()){
                    canActivateTimer = false;
                }

                // --- Calculate Target Timer Speed based on Mode ---
                float targetTimerSpeed = 1.0f; // Default if not activating

                if (canActivateTimer) {
                    if (timerModeProperty.getValue() == TimerMode.SLOW_DOWN) {
                        targetTimerSpeed = slowedTimerSpeedProperty.getValue().floatValue();
                    } else if (timerModeProperty.getValue() == TimerMode.SPEED_UP) {
                        targetTimerSpeed = spedUpTimerSpeedProperty.getValue().floatValue();
                    } else if (timerModeProperty.getValue() == TimerMode.ADAPTIVE) {
                        // Adaptive Logic: Interpolate between slowed and sped up based on distance
                        // The active range is between rangeToResetTimerProperty and KillAura.rangeProperty
                        // Clamp distance within this range
                        double clampedDistance = MathHelper.clamp_double(
                                distanceToTarget,
                                rangeToResetTimerProperty.getValue(),
                                KillAuraModule.attackRangeProperty.getValue()
                        );

                        // Normalize distance to a 0-1 scale within the active range
                        double normalizedDistance = (clampedDistance - rangeToResetTimerProperty.getValue()) /
                                (KillAuraModule.attackRangeProperty.getValue() - rangeToResetTimerProperty.getValue());

                        // Interpolate between slowed and sped-up speeds
                        // If normalizedDistance is 0 (very close), use spedUp. If 1 (farther), use slowed.
                        // Or vice-versa depending on desired adaptive behavior.
                        // Let's assume: Closer = faster (more hits), Further = slower (better reach)
                        // If normalizedDistance is 0, factor is 1. If normalizedDistance is 1, factor is 0.
                        double interpolationFactor = 1.0 - normalizedDistance; // Ranges from 1 (closest) to 0 (farthest)

                        targetTimerSpeed = (float) (slowedTimerSpeedProperty.getValue() +
                                (spedUpTimerSpeedProperty.getValue() - slowedTimerSpeedProperty.getValue()) * interpolationFactor);

                        // Ensure adaptive speed doesn't go below min or above max set by user
                        targetTimerSpeed = MathHelper.clamp_float(targetTimerSpeed,
                                slowedTimerSpeedProperty.getValue().floatValue(),
                                spedUpTimerSpeedProperty.getValue().floatValue());

                        // If rangeToResetTimerProperty and KillAura.rangeProperty are too close, avoid division by zero
                        if (KillAuraModule.attackRangeProperty.getValue() - rangeToResetTimerProperty.getValue() < 0.01) {
                            targetTimerSpeed = spedUpTimerSpeedProperty.getValue().floatValue(); // Default to fast if range is negligible
                        }
                    }
                }

                // --- Apply Timer Speed ---
                if (!timerActive || mc.timer.timerSpeed != targetTimerSpeed) {
                    mc.timer.timerSpeed = targetTimerSpeed;
                    timerActive = true;
                }
            }
        }
    };

    @Override
    public void onDisable() {
        super.onDisable();
        mc.timer.timerSpeed = 1.0f; // Ensure timer is reset when module is disabled
        timerActive = false;
    }
}