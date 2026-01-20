package cc.simp.utils.mc;

import cc.simp.processes.RotationProcess;
import cc.simp.utils.Util;
import cc.simp.utils.client.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

public class RotationUtils extends Util {
    public static Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (MathHelper.atan2(diff.getZ(), diff.getX()) * MathUtils.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.getY(), distance) * MathUtils.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f calculate(final Entity entity) {
        return calculate(entity.getCustomPositionVector().add(0, Math.max(0, Math.min(mc.thePlayer.posY - entity.posY +
                mc.thePlayer.getEyeHeight(), (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.9)), 0));
    }

    public static Vector2f calculate(final Entity entity, final boolean adaptive, final double range) {
        Vector2f normalRotations = calculate(entity);
        if (!adaptive || RayCastUtils.rayCast(normalRotations, range).typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return normalRotations;
        }

        for (double yPercent = 1; yPercent >= 0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1; zPercent >= -0.5; zPercent -= 0.5) {
                    Vector2f adaptiveRotations = calculate(entity.getCustomPositionVector().add(
                            (entity.getEntityBoundingBox().maxX - entity.getEntityBoundingBox().minX) * xPercent,
                            (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * yPercent,
                            (entity.getEntityBoundingBox().maxZ - entity.getEntityBoundingBox().minZ) * zPercent));

                    if (RayCastUtils.rayCast(adaptiveRotations, range).typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        return adaptiveRotations;
                    }
                }
            }
        }

        return normalRotations;
    }

    public Vector2f calculate(final Vec3 to, final EnumFacing enumFacing) {
        return calculate(new Vector3d(to.xCoord, to.yCoord, to.zCoord), enumFacing);
    }

    public static Vector2f calculate(final Vec3 to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), new Vector3d(to.xCoord, to.yCoord, to.zCoord));
    }

    public Vector2f calculate(final BlockPos to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), new Vector3d(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public static Vector2f calculate(final Vector3d to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), to);
    }

    public static Vector2f calculate(final Vector3d position, final EnumFacing enumFacing) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) enumFacing.getDirectionVec().getX() * 0.5D;
        y += (double) enumFacing.getDirectionVec().getY() * 0.5D;
        z += (double) enumFacing.getDirectionVec().getZ() * 0.5D;
        return calculate(new Vector3d(x, y, z));
    }

    public static Vector2f puhfyRotations(final Entity entity) {

        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        AxisAlignedBB box = entity.getEntityBoundingBox();

        // Basically the 3 parts of the human entity.
        Vec3[] points = new Vec3[]{
                new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0),
                new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0),
                new Vec3((box.minX + box.maxX) / 2.0, box.maxY - 0.1, (box.minZ + box.maxZ) / 2.0)
        };

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

        final float[] rotations = getRotationsTo(eyePos, bestPoint);
        final float targetYaw = rotations[0];
        final float targetPitch = rotations[1];

        return new Vector2f(targetYaw, targetPitch);
    }

    public static float[] getRotationsTo(Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        double distHorizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distHorizontal));
        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        pitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch);
        return new float[]{yaw, pitch};
    }

    public static Vector2f move(final Vector2f targetRotation, final double speed) {
        return move(RotationProcess.lastRotations, targetRotation, speed);
    }

    public static Vector2f move(final Vector2f lastRotation, final Vector2f targetRotation, double speed) {
        if (speed != 0) {

            double deltaYaw = MathHelper.wrapAngleTo180_float(targetRotation.x - lastRotation.x);
            final double deltaPitch = (targetRotation.y - lastRotation.y);

            final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            final double distributionYaw = Math.abs(deltaYaw / distance);
            final double distributionPitch = Math.abs(deltaPitch / distance);

            final double maxYaw = speed * distributionYaw;
            final double maxPitch = speed * distributionPitch;

            final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            return new Vector2f(moveYaw, movePitch);
        }

        return new Vector2f(0, 0);
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation) {
        final Vector2f previousRotation = mc.thePlayer.getPreviousRotation();
        final float mouseSensitivity = (float) (mc.gameSettings.mouseSensitivity * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90, 90));
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation, final Vector2f previousRotation) {
        final float mouseSensitivity = (float) (mc.gameSettings.mouseSensitivity * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90, 90));
    }

    public static float[] faceTrajectory(Entity target, boolean predict, float predictSize, float gravity, float velocity) {
        EntityPlayerSP player = mc.thePlayer;

        double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0.0) - (player.posX + (predict ? player.posX - player.prevPosX : 0.0));
        double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0.0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? player.posY - player.prevPosY : 0.0)) - player.getEyeHeight();
        double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0.0) - (player.posZ + (predict ? player.posZ - player.prevPosZ : 0.0));
        double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        velocity = Math.min((velocity * velocity + velocity * 2) / 3, 1f);

        float gravityModifier = 0.12f * gravity;

        return new float[]{
                (float) Math.toDegrees(Math.atan2(posZ, posX)) - 90f,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(
                        velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
                )) / (gravityModifier * posSqrt)))
        };
    }

    public static Vector2f resetRotation(final Vector2f rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.x + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotation.x);
        final float pitch = mc.thePlayer.rotationPitch;
        return new Vector2f(yaw, pitch);
    }
    public static Vector2f smooth(final Vector2f targetRotation, final double speed) {
        return smooth(RotationProcess.lastRotations, targetRotation, speed);
    }

    public static Vector2f smooth(final Vector2f lastRotation, final Vector2f targetRotation, final double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        final float lastYaw = lastRotation.x;
        final float lastPitch = lastRotation.y;

        if (speed != 0) {
            Vector2f move = move(targetRotation, speed);

            yaw = lastYaw + move.x;
            pitch = lastPitch + move.y;

            for (int i = 1; i <= (int) (Minecraft.getDebugFPS() / 20f + Math.random() * 10); ++i) {

                if (Math.abs(move.x) + Math.abs(move.y) > 0.0001) {
                    yaw += (Math.random() - 0.5) / 1000;
                    pitch -= Math.random() / 200;
                }

                /*
                 * Fixing GCD
                 */
                final Vector2f rotations = new Vector2f(yaw, pitch);
                final Vector2f fixedRotations = RotationUtils.applySensitivityPatch(rotations);

                /*
                 * Setting rotations
                 */
                yaw = fixedRotations.x;
                pitch = Math.max(-90, Math.min(90, fixedRotations.y));
            }
        }

        return new Vector2f(yaw, pitch);
    }

    public static float getMovementYaw() {
        float yaw = 180.0f;
        KeyBinding forward = RotationUtils.mc.gameSettings.keyBindForward;
        KeyBinding back = RotationUtils.mc.gameSettings.keyBindBack;
        KeyBinding right = RotationUtils.mc.gameSettings.keyBindRight;
        KeyBinding left = RotationUtils.mc.gameSettings.keyBindLeft;
        if (back.isKeyDown()) {
            yaw -= 180.0f;
            if (right.isKeyDown()) {
                yaw -= 45.0f;
            }
            if (left.isKeyDown()) {
                yaw += 45.0f;
            }
        } else if (forward.isKeyDown()) {
            if (right.isKeyDown()) {
                yaw += 45.0f;
            }
            if (left.isKeyDown()) {
                yaw -= 45.0f;
            }
        } else {
            if (right.isKeyDown()) {
                yaw += 90.0f;
            }
            if (left.isKeyDown()) {
                yaw -= 90.0f;
            }
        }
        return (MathHelper.wrapAngleTo180_float(RotationUtils.mc.thePlayer.rotationYaw) + yaw % 360.0f + 360.0f) % 360.0f;
    }

}
