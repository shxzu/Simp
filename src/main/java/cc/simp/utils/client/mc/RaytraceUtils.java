package cc.simp.utils.client.mc;

import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.utils.client.Util;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import net.optifine.reflect.Reflector;

import java.util.List;

public class RaytraceUtils extends Util {

    public static boolean isOnBlock(EnumFacing facing, BlockPos position, boolean strict, float reach, float yaw, float pitch) {
        MovingObjectPosition blockHitResult = rayTraceBlocks(yaw, pitch);
        if (blockHitResult == null) {
            return false;
        }

        if (blockHitResult.getBlockPos().getX() == position.getX() && blockHitResult.getBlockPos().getY() == position.getY() && blockHitResult.getBlockPos().getZ() == position.getZ()) {
            if (strict) {
                mc.objectMouseOver = blockHitResult;
                return blockHitResult.sideHit == facing;
            } else {
                mc.objectMouseOver = blockHitResult;
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean isOnBlockForVec3(final BlockPos pos, final boolean strict) {
        EnumFacing enumFacing = mc.thePlayer.getHorizontalFacing();
        final MovingObjectPosition movingObjectPosition = mc.objectMouseOver;

        if (movingObjectPosition == null) return false;

        final Vec3 hitVec = movingObjectPosition.hitVec;
        if (hitVec == null || movingObjectPosition.getBlockPos() == null) return false;

        return movingObjectPosition.getBlockPos().equals(pos) && (!strict || movingObjectPosition.sideHit == enumFacing);
    }

    private static MovingObjectPosition rayTraceBlocks(float yaw, float pitch) {
        double blockReachDistance = mc.playerController.getBlockReachDistance();
        final Vec3 vec3 = mc.thePlayer.getPositionEyes(1.0F);
        final Vec3 vec31 = Entity.getVectorForRotation(pitch, yaw);
        final Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        return mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, false);
    }
    
}
