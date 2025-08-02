package cc.simp.utils.mc;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.utils.Util;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

import java.util.ArrayList;

public class MovementUtils extends Util {

    public static float yaw = 0;
    public static float prevYaw = 0;
    public static float pitch = 0;
    public static float prevPitch = 0;

    private static boolean isMovingEnoughForSprint() {
        MovementInput movementInput = mc.thePlayer.movementInput;
        return movementInput.moveForward > 0.8F || movementInput.moveForward < -0.8F ||
                movementInput.moveStrafe > 0.8F || movementInput.moveStrafe < -0.8F;
    }

    public static boolean canSprint(boolean omni) {
        final EntityPlayerSP player = mc.thePlayer;
        return (omni ? isMovingEnoughForSprint() : player.movementInput.moveForward >= 0.8F) &&
                !player.isCollidedHorizontally &&
                (player.getFoodStats().getFoodLevel() > 6 ||
                        player.capabilities.allowFlying) &&
                !player.isSneaking() &&
                (!player.isUsingItem()) &&
                !player.isPotionActive(Potion.moveSlowdown.id);
    }

    public static void setSpeed(double moveSpeed) {
        setSpeed(moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }

    public static void setSpeed(MotionEvent e, double speed) {
        final EntityPlayerSP player = mc.thePlayer;
        setSpeed(e, speed, player.moveForward, player.moveStrafing, player.rotationYaw);
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            if (strafe > 0.0D) {
                yaw += ((forward > 0.0D) ? -45 : 45);
            } else if (strafe < 0.0D) {
                yaw += ((forward > 0.0D) ? 45 : -45);
            }
            strafe = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        if (strafe > 0.0D) {
            strafe = 1.0D;
        } else if (strafe < 0.0D) {
            strafe = -1.0D;
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz;
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx;
    }

    public static void setSpeed(MotionEvent e, double speed, float forward, float strafing, float yaw) {
        if (forward == 0.0F && strafing == 0.0F)
            return;

        boolean reversed = forward < 0.0f;
        float strafingYaw = 90.0f *
                (forward > 0.0f ? 0.5f : reversed ? -0.5f : 1.0f);

        if (reversed)
            yaw += 180.0f;
        if (strafing > 0.0f)
            yaw -= strafingYaw;
        else if (strafing < 0.0f)
            yaw += strafingYaw;

        double x = StrictMath.cos(StrictMath.toRadians(yaw + 90.0f));
        double z = StrictMath.cos(StrictMath.toRadians(yaw));

        e.setPosX(x * speed);
        e.setPosZ(z * speed);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public void strafe(MotionEvent event) {
        strafe(event, getSpeed());
    }

    public static void strafe(double movementSpeed) {
        strafe(null, movementSpeed);
    }

    public static void strafe(MotionEvent motionEvent, double movementSpeed) {
        if (mc.thePlayer.movementInput.moveForward > 0.0) {
            mc.thePlayer.movementInput.moveForward = (float) 1.0;
        } else if (mc.thePlayer.movementInput.moveForward < 0.0) {
            mc.thePlayer.movementInput.moveForward = (float) -1.0;
        }

        if (mc.thePlayer.movementInput.moveStrafe > 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (float) 1.0;
        } else if (mc.thePlayer.movementInput.moveStrafe < 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (float) -1.0;
        }

        if (mc.thePlayer.movementInput.moveForward == 0.0 && mc.thePlayer.movementInput.moveStrafe == 0.0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }

        if (mc.thePlayer.movementInput.moveForward != 0.0 && mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveForward *= (float) Math.sin(0.6398355709958845);
            mc.thePlayer.movementInput.moveStrafe *= (float) Math.cos(0.6398355709958845);
        }

        if (motionEvent != null) {
            motionEvent.setPosX(mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw))
                    + mc.thePlayer.movementInput.moveStrafe * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)));
            motionEvent.setPosZ(mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw))
                    - mc.thePlayer.movementInput.moveStrafe * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)));
        } else {
            mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw))
                    + mc.thePlayer.movementInput.moveStrafe * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw));
            mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw))
                    - mc.thePlayer.movementInput.moveStrafe * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw));
        }
    }

    public static double getSpeed() {
        return mc.thePlayer == null ? 0 : Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (MovementUtils.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            baseSpeed *= 1.0 + 0.2 * (MovementUtils.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return baseSpeed;
    }

    public static boolean isOnGround() {
        return mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically;
    }

    public static boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F;
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static double[] getMotion(final double speed, final float strafe, final float forward, final float yaw) {
        final float friction = (float)speed;
        final float f1 = MathHelper.sin(yaw * 3.1415927f / 180.0f);
        final float f2 = MathHelper.cos(yaw * 3.1415927f / 180.0f);
        final double motionX = strafe * friction * f2 - forward * friction * f1;
        final double motionZ = forward * friction * f2 + strafe * friction * f1;
        return new double[] { motionX, motionZ };
    }

    public static void silentRotationStrafe(final StrafeEvent event, final float yaw) {
        float currentFriction = event.getFriction();

        final int dif = (int)((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw - 23.5f - 135.0f) + 180.0f) / 45.0f);
        final float strafe = event.getStrafe();
        final float forward = event.getForward();

        float calcForward = 0.0f;
        float calcStrafe = 0.0f;

        switch (dif) {
            case 0: { calcForward = forward; calcStrafe = strafe; break; }
            case 1: { calcForward += forward; calcStrafe -= forward; calcForward += strafe; calcStrafe += strafe; break; }
            case 2: { calcForward = strafe; calcStrafe = -forward; break; }
            case 3: { calcForward -= forward; calcStrafe -= forward; calcForward += strafe; calcStrafe -= strafe; break; }
            case 4: { calcForward = -forward; calcStrafe = -strafe; break; }
            case 5: { calcForward -= forward; calcStrafe += forward; calcForward -= strafe; calcStrafe -= strafe; break; }
            case 6: { calcForward = -strafe; calcStrafe = forward; break; }
            case 7: { calcForward += forward; calcStrafe += forward; calcForward -= strafe; calcStrafe += strafe; break; }
        }

        if (calcForward > 1.0f || (calcForward < 0.9f && calcForward > 0.3f) || calcForward < -1.0f || (calcForward > -0.9f && calcForward < -0.3f)) {
            calcForward *= 0.5f;
        }
        if (calcStrafe > 1.0f || (calcStrafe < 0.9f && calcStrafe > 0.3f) || calcStrafe < -1.0f || (calcStrafe > -0.9f && calcStrafe < -0.3f)) {
            calcStrafe *= 0.5f;
        }

        float f = calcStrafe * calcStrafe + calcForward * calcForward;
        if (f >= 1.0E-4f) {
            if ((f = MathHelper.sqrt_float(f)) < 1.0f) {
                f = 1.0f;
            }

            f = currentFriction / f;
            final float f2 = MathHelper.sin(yaw * 3.1415927f / 180.0f);
            final float f3 = MathHelper.cos(yaw * 3.1415927f / 180.0f);
            final EntityPlayerSP thePlayer = MovementUtils.mc.thePlayer;
            thePlayer.motionX += (calcStrafe *= f) * f3 - (calcForward *= f) * f2;
            final EntityPlayerSP thePlayer2 = MovementUtils.mc.thePlayer;
            thePlayer2.motionZ += calcForward * f3 + calcStrafe * f2;
        }
    }


    public static float[] handleMovementFix(final float strafe, final float forward, final float yaw, final boolean advanced) {
        final float diff = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw); // Difference between intended yaw and actual player yaw
        float newForward = 0.0f;
        float newStrafe = 0.0f;

        if (!advanced) { // Non-advanced (simpler, angle-based) movement fix
            if (diff >= 22.5 && diff < 67.5) { // Forward-right quadrant
                newStrafe += strafe;
                newForward += forward;
                newStrafe -= forward; // Adjust strafe for forward input
                newForward += strafe; // Adjust forward for strafe input
            } else if (diff >= 67.5 && diff < 112.5) { // Right quadrant
                newStrafe -= forward;
                newForward += strafe;
            } else if (diff >= 112.5 && diff < 157.5) { // Backward-right quadrant
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe -= forward;
                newForward += strafe;
            } else if (diff >= 157.5 || diff <= -157.5) { // Backward quadrant
                newStrafe -= strafe;
                newForward -= forward;
            } else if (diff > -157.5 && diff <= -112.5) { // Backward-left quadrant
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe += forward;
                newForward -= strafe;
            } else if (diff > -112.5 && diff <= -67.5) { // Left quadrant
                newStrafe += forward;
                newForward -= strafe;
            } else if (diff > -67.5 && diff <= -22.5) { // Forward-left quadrant
                newStrafe += strafe;
                newForward += forward;
                newStrafe += forward;
                newForward -= strafe;
            } else { // Forward quadrant (or very small diff)
                newStrafe += strafe;
                newForward += forward;
            }
            return new float[]{newStrafe, newForward}; // Return adjusted strafe, forward
        }

        // Advanced movement fix (brute-force check of all input combinations)
        // Use the intended yaw for the "real" (desired) motion calculation
        final float intendedYawForRealMotion = Simp.INSTANCE.getRotationManager().isRotating() ? Simp.INSTANCE.getRotationManager().getClientYaw() : mc.thePlayer.rotationYaw;
        // Use a base speed (0.22 is a common walking speed value) for comparison
        double[] realMotion = getMotion(0.22, strafe, forward, intendedYawForRealMotion);

        // Calculate the target real position based on desired motion from intended yaw
        final double[] realPos = new double[] { mc.thePlayer.posX + realMotion[0], mc.thePlayer.posZ + realMotion[1] };

        final ArrayList<float[]> possibleForwardStrafe = new ArrayList<>();

        // Generate all 8 cardinal/intercardinal movement input combinations
        // Values are [forward, strafe] for this list of possible inputs
        possibleForwardStrafe.add(new float[] { 1.0f, 0.0f });   // W
        possibleForwardStrafe.add(new float[] { -1.0f, 0.0f });  // S
        possibleForwardStrafe.add(new float[] { 0.0f, 1.0f });   // A
        possibleForwardStrafe.add(new float[] { 0.0f, -1.0f });  // D
        possibleForwardStrafe.add(new float[] { 1.0f, 1.0f });   // WA
        possibleForwardStrafe.add(new float[] { 1.0f, -1.0f });  // WD
        possibleForwardStrafe.add(new float[] { -1.0f, 1.0f });  // SA
        possibleForwardStrafe.add(new float[] { -1.0f, -1.0f }); // SD
        // Also add no movement (for completeness, though might not be the best match usually)
        possibleForwardStrafe.add(new float[] { 0.0f, 0.0f });


        double minDistanceSq = Double.MAX_VALUE;
        float[] bestFloats = new float[2]; // Stores [forward, strafe] for the best match

        for (final float[] currentInputs : possibleForwardStrafe) {
            // Clamp inputs to [-1.0, 1.0] if not already normalized
            float currentForward = MathHelper.clamp_float(currentInputs[0], -1.0f, 1.0f);
            float currentStrafe = MathHelper.clamp_float(currentInputs[1], -1.0f, 1.0f);

            // Calculate motion for *this* input combination using player's *actual* rotationYaw
            // This is the key: we simulate what Minecraft *would* do with these inputs given the player's server yaw
            final double[] simulatedMotion = getMotion(0.22, currentStrafe, currentForward, mc.thePlayer.rotationYaw);

            // Predict player's next position with this simulated motion
            double simulatedPosX = mc.thePlayer.posX + simulatedMotion[0];
            double simulatedPosZ = mc.thePlayer.posZ + simulatedMotion[1];

            // Calculate squared distance between where we *want* to go (realPos - calculated using intended yaw)
            // and where this simulated movement would take us (simulatedPosX/Z - calculated using actual server yaw)
            final double diffX = realPos[0] - simulatedPosX;
            final double diffZ = realPos[1] - simulatedPosZ;
            final double distanceSq = diffX * diffX + diffZ * diffZ;

            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                bestFloats = new float[]{currentInputs[0], currentInputs[1]}; // Store the [forward, strafe] that gave the best match
            }
        }
        // Return adjusted strafe, forward. Note the original code returned floats[1] (forward) then floats[0] (strafe)
        // This suggests bestFloats stores [forward, strafe] internally and needs swapping for the return type.
        return new float[]{bestFloats[1], bestFloats[0]}; // Return adjusted {strafe, forward}
    }

    public static float getDirection() {
        if (MovementUtils.mc.thePlayer == null) {
            return 0.0f;
        }
        float yaw = MovementUtils.mc.thePlayer.rotationYaw;
        boolean forward = MovementUtils.mc.gameSettings.keyBindForward.isKeyDown();
        boolean back = MovementUtils.mc.gameSettings.keyBindBack.isKeyDown();
        boolean left = MovementUtils.mc.gameSettings.keyBindLeft.isKeyDown();
        boolean right = MovementUtils.mc.gameSettings.keyBindRight.isKeyDown();
        float result = 0.0f;
        if (forward) {
            result = left && !right ? -45.0f : (right && !left ? 45.0f : 0.0f);
        } else if (back) {
            result = left && !right ? -135.0f : (right && !left ? 135.0f : 180.0f);
        } else if (left && !right) {
            result = -90.0f;
        } else if (right && !left) {
            result = 90.0f;
        }
        float direction = yaw + result;
        direction = (direction % 360.0f + 360.0f) % 360.0f;
        return direction;
    }

    public static double calculateBPS() {
        double bps = (Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX, mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * mc.timer.timerSpeed) * 20;
        return Math.round(bps * 100.0) / 100.0;
    }

}
