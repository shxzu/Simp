package cc.simp.utils.client.mc;

import cc.simp.Simp;
import cc.simp.modules.impl.player.SmoothRotationsModule;
import cc.simp.utils.client.Util;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.optifine.util.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class RotationUtils extends Util {
    public static float currentYaw = mc.thePlayer.rotationYaw;
    public static float currentPitch = mc.thePlayer.rotationPitch;

    public static float smoothRotation(float from, float to, float speed) {
        float f = MathHelper.wrapAngleTo180_float(to - from);
        if (f > speed) {
            f = speed;
        }
        if (f < -speed) {
            f = -speed;
        }
        return from + f;
    }

    public static float smoothPitch(float Pitch) {
        int value = SmoothRotationsModule.rotSpeed.getValue().intValue();
        float pitch;
        pitch = smoothRotation(Simp.INSTANCE.getRotationHandler().getPrevServerPitch(), Pitch, 10 * value);
        return pitch;
    }

    public static float smoothYaw(float Yaw) {
        int value = SmoothRotationsModule.rotSpeed.getValue().intValue();
        float yaw;
        yaw = smoothRotation(Simp.INSTANCE.getRotationHandler().getPrevServerYaw(), Yaw, 10 * value);
        return yaw;
    }

    public static float[] getClosestRotations(Entity entity, float jitterAmount) {
        if (entity == null) return null;

        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        AxisAlignedBB box = entity.getEntityBoundingBox();

        //Basically the 3 parts of the human entity.
        Vec3[] points = new Vec3[]{
                new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0),
                new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0),
                new Vec3((box.minX + box.maxX) / 2.0, box.maxY - 0.1, (box.minZ + box.maxZ) / 2.0)
        };

        //So this kinda just gets the closest point from the 3 points above

        Vec3 bestPoint = null;
        double closestDist = Double.MAX_VALUE;

        for (Vec3 point : points) {
            double dist = eyePos.distanceTo(point);
            if (dist < closestDist) {
                closestDist = dist;
                bestPoint = point;
            }
        }

        if (bestPoint == null) return null;

        float targetYaw = getRotationTo(eyePos, bestPoint)[0];
        float targetPitch = getRotationTo(eyePos, bestPoint)[1];

        currentYaw = targetYaw;
        currentPitch = targetPitch;

        currentYaw += (float) ((Math.random() - 0.5) * 2 * jitterAmount);
        currentPitch += (float) ((Math.random() - 0.5) * 2 * jitterAmount);
        currentPitch = Math.max(-90F, Math.min(90F, currentPitch));

        return new float[]{currentYaw, currentPitch};
    }

    private static float[] getRotationTo(Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;

        double distHorizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distHorizontal));

        return new float[]{
                MathHelper.wrapAngleTo180_float(yaw),
                MathHelper.wrapAngleTo180_float(pitch)
        };
    }

    // GCD Patch
    public static float[] getFixedRotation(final float[] rotations, final float[] lastRotations) {
        final float yaw = rotations[0];
        final float pitch = rotations[1];

        final float lastYaw = lastRotations[0];
        final float lastPitch = lastRotations[1];

        final float f = (float) (mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        final float gcd = f * f * f * 1.2F;

        final float deltaYaw = yaw - lastYaw;
        final float deltaPitch = pitch - lastPitch;

        final float fixedDeltaYaw = deltaYaw - (deltaYaw % gcd);
        final float fixedDeltaPitch = deltaPitch - (deltaPitch % gcd);

        final float fixedYaw = lastYaw + fixedDeltaYaw;
        final float fixedPitch = lastPitch + fixedDeltaPitch;

        return new float[]{fixedYaw, fixedPitch};
    }

    // Rotation Fixes
    public static float[] getPatchedAndCappedRots(float[] prev, float[] current, float speed) {
        return getFixedRotation(getCappedRotations(prev, current, speed), prev);
    }

    public static float[] getCappedRotations(float[] prev, float[] current, float speed) {
        float yawDiff = RotationUtils.getYawDifference(current[0], prev[0]);
        if (Math.abs(yawDiff) > speed)
            yawDiff = (speed * (yawDiff > 0 ? 1 : -1)) / 2f;
        float cappedPYaw = MathHelper.wrapAngleTo180_float(prev[0] + yawDiff);

        float pitchDiff = RotationUtils.getYawDifference(current[1], prev[1]);
        if (Math.abs(pitchDiff) > speed / 2f)
            pitchDiff = (speed / 2f * (pitchDiff > 0 ? 1 : -1)) / 2f;
        float cappedPitch = MathHelper.wrapAngleTo180_float(prev[1] + pitchDiff);
        return new float[]{cappedPYaw, cappedPitch};
    }

    // Differences
    public static float getYawDifference(float yaw1, float yaw2) {
        float yawDiff = MathHelper.wrapAngleTo180_float(yaw1) - MathHelper.wrapAngleTo180_float(yaw2);
        if (Math.abs(yawDiff) > 180) {
            yawDiff = yawDiff + 360;
        }
        return MathHelper.wrapAngleTo180_float(yawDiff);
    }

}
