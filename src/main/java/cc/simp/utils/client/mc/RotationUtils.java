package cc.simp.utils.client.mc;

import cc.simp.utils.client.Util;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RotationUtils extends Util {

    public static float serverYaw;
    public static float serverPitch;
    public static float currentYaw = mc.thePlayer.rotationYaw;
    public static float currentPitch = mc.thePlayer.rotationPitch;

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

        return new float[] {currentYaw, currentPitch };
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

}
