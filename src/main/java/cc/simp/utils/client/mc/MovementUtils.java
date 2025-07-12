package cc.simp.utils.client.mc;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.client.Util;
import net.minecraft.client.Minecraft;
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
        final int dif = (int)((MathHelper.wrapAngleTo180_float(MovementUtils.mc.thePlayer.rotationYaw - yaw - 23.5f - 135.0f) + 180.0f) / 45.0f);
        final float strafe = event.getStrafe();
        final float forward = event.getForward();
        final float friction = event.getFriction();
        float calcForward = 0.0f;
        float calcStrafe = 0.0f;
        switch (dif) {
            case 0: {
                calcForward = forward;
                calcStrafe = strafe;
                break;
            }
            case 1: {
                calcForward += forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe += strafe;
                break;
            }
            case 2: {
                calcForward = strafe;
                calcStrafe = -forward;
                break;
            }
            case 3: {
                calcForward -= forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe -= strafe;
                break;
            }
            case 4: {
                calcForward = -forward;
                calcStrafe = -strafe;
                break;
            }
            case 5: {
                calcForward -= forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe -= strafe;
                break;
            }
            case 6: {
                calcForward = -strafe;
                calcStrafe = forward;
                break;
            }
            case 7: {
                calcForward += forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe += strafe;
                break;
            }
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
            f = friction / f;
            final float f2 = MathHelper.sin(yaw * 3.1415927f / 180.0f);
            final float f3 = MathHelper.cos(yaw * 3.1415927f / 180.0f);
            final EntityPlayerSP thePlayer = MovementUtils.mc.thePlayer;
            thePlayer.motionX += (calcStrafe *= f) * f3 - (calcForward *= f) * f2;
            final EntityPlayerSP thePlayer2 = MovementUtils.mc.thePlayer;
            thePlayer2.motionZ += calcForward * f3 + calcStrafe * f2;
        }
    }

    public static float[] handleMovementFix(final float strafe, final float forward, final float yaw, final boolean advanced) {
        final Minecraft mc = Minecraft.getMinecraft();
        final float diff = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        float newForward = 0.0f;
        float newStrafe = 0.0f;
        if (!advanced) {
            if (diff >= 22.5 && diff < 67.5) {
                newStrafe += strafe;
                newForward += forward;
                newStrafe -= forward;
                newForward += strafe;
            }
            else if (diff >= 67.5 && diff < 112.5) {
                newStrafe -= forward;
                newForward += strafe;
            }
            else if (diff >= 112.5 && diff < 157.5) {
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe -= forward;
                newForward += strafe;
            }
            else if (diff >= 157.5 || diff <= -157.5) {
                newStrafe -= strafe;
                newForward -= forward;
            }
            else if (diff > -157.5 && diff <= -112.5) {
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe += forward;
                newForward -= strafe;
            }
            else if (diff > -112.5 && diff <= -67.5) {
                newStrafe += forward;
                newForward -= strafe;
            }
            else if (diff > -67.5 && diff <= -22.5) {
                newStrafe += strafe;
                newForward += forward;
                newStrafe += forward;
                newForward -= strafe;
            }
            else {
                newStrafe += strafe;
                newForward += forward;
            }
            return new float[] { newStrafe, newForward };
        }
        final float baseYaw = Simp.INSTANCE.getRotationManager().isRotating() ? Simp.INSTANCE.getRotationManager().getClientYaw() : mc.thePlayer.rotationYaw;
        double[] realMotion = getMotion(0.22, strafe, forward, baseYaw);
        final double[] array;
        final double[] realPos = array = new double[] { mc.thePlayer.posX, mc.thePlayer.posZ };
        final int n = 0;
        array[n] += realMotion[0];
        final double[] array2 = realPos;
        final int n2 = 1;
        array2[n2] += realMotion[1];
        final ArrayList<float[]> possibleForwardStrafe = new ArrayList<float[]>();
        int i = 0;
        boolean b = false;
        while (!b) {
            newForward = 0.0f;
            newStrafe = 0.0f;
            if (i == 0) {
                newStrafe += strafe;
                newForward += forward;
                newStrafe -= forward;
                newForward += strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 1) {
                newStrafe -= forward;
                newForward += strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 2) {
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe -= forward;
                newForward += strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 3) {
                newStrafe -= strafe;
                newForward -= forward;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 4) {
                newStrafe -= strafe;
                newForward -= forward;
                newStrafe += forward;
                newForward -= strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 5) {
                newStrafe += forward;
                newForward -= strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else if (i == 6) {
                newStrafe += strafe;
                newForward += forward;
                newStrafe += forward;
                newForward -= strafe;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
            }
            else {
                newStrafe += strafe;
                newForward += forward;
                possibleForwardStrafe.add(new float[] { newForward, newStrafe });
                b = true;
            }
            ++i;
        }
        double distance = 5000.0;
        float[] floats = new float[2];
        for (final float[] flo : possibleForwardStrafe) {
            if (flo[0] > 1.0f) {
                flo[0] = 1.0f;
            }
            else if (flo[0] < -1.0f) {
                flo[0] = -1.0f;
            }
            if (flo[1] > 1.0f) {
                flo[1] = 1.0f;
            }
            else if (flo[1] < -1.0f) {
                flo[1] = -1.0f;
            }
            final double[] motion2;
            final double[] motion = motion2 = getMotion(0.22, flo[1], flo[0], mc.thePlayer.rotationYaw);
            final int n3 = 0;
            motion2[n3] += mc.thePlayer.posX;
            final double[] array3 = motion;
            final int n4 = 1;
            array3[n4] += mc.thePlayer.posZ;
            final double diffX = Math.abs(realPos[0] - motion[0]);
            final double diffZ = Math.abs(realPos[1] - motion[1]);
            final double d0 = diffX * diffX + diffZ * diffZ;
            if (d0 < distance) {
                distance = d0;
                floats = flo;
            }
        }
        return new float[] { floats[1], floats[0] };
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
