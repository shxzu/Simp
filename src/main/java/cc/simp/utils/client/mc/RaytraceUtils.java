package cc.simp.utils.client.mc;

import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.utils.client.Util;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
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

    private static MovingObjectPosition rayTraceBlocks(float yaw, float pitch) {
        double blockReachDistance = mc.playerController.getBlockReachDistance();
        final Vec3 vec3 = mc.thePlayer.getPositionEyes(1.0F);
        final Vec3 vec31 = Entity.getVectorForRotation(pitch, yaw);
        final Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        return mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, false);
    }

    public static Entity rayTrace(final double range, final float[] rotations) {
        if (RotationUtils.mc.objectMouseOver.entityHit != null) {
            return RotationUtils.mc.objectMouseOver.entityHit;
        }
        final Vec3 vec3 = Minecraft.getMinecraft().thePlayer.getPositionEyes(1.0f);
        final Vec3 vec4 = RotationUtils.mc.thePlayer.getVectorForRotation(rotations[1], rotations[0]);
        final Vec3 vec5 = vec3.addVector(vec4.xCoord * range, vec4.yCoord * range, vec4.zCoord * range);
        Entity pointedEntity = null;
        final float f = 1.0f;
        final List<?> list = Minecraft.getMinecraft().theWorld.getEntitiesInAABBexcluding(Minecraft.getMinecraft().getRenderViewEntity(), Minecraft.getMinecraft().getRenderViewEntity().getEntityBoundingBox().addCoord(vec4.xCoord * range, vec4.yCoord * range, vec4.zCoord * range).expand(f, f, f), Predicates.and((Predicate<? super Entity>)EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
        double d2 = range;
        for (final Object o : list) {
            final Entity entity1 = (Entity)o;
            final float f2 = entity1.getCollisionBorderSize();
            final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f2, f2, f2);
            final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec5);
            if (axisalignedbb.isVecInside(vec3)) {
                if (d2 < 0.0) {
                    continue;
                }
                pointedEntity = entity1;
                d2 = 0.0;
            }
            else {
                if (movingobjectposition == null) {
                    continue;
                }
                final double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                if (d3 >= d2 && d2 != 0.0) {
                    continue;
                }
                boolean flag2 = false;
                if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                    flag2 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract, new Object[0]);
                }
                if (entity1 == Minecraft.getMinecraft().getRenderViewEntity().ridingEntity && !flag2) {
                    if (d2 != 0.0) {
                        continue;
                    }
                    pointedEntity = entity1;
                }
                else {
                    pointedEntity = entity1;
                    d2 = d3;
                }
            }
        }
        return pointedEntity;
    }

    public static MovingObjectPosition getMouseOver(float[] rotation, final double range)
    {
        Entity entity = mc.getRenderViewEntity();
        Entity pointedEntity;

        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null)
        {
            mc.mcProfiler.startSection("pick");
            pointedEntity = null;
            double d0 = range;
            objectMouseOver = mc.thePlayer.rayTrace(rotation[0], rotation[1]);
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(1);
            boolean flag = false;
            int i = 3;

            if (mc.playerController.extendedReach())
            {
                d0 = 3.0D;
                d1 = 6.0D;
            }
            else if (d0 > 3.0D)
            {
                flag = true;
            }

            if (objectMouseOver != null)
            {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            Vec3 vec31 = entity.getLook(1);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord* d0).expand((double)f, (double)f, (double)f), Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>()
            {
                public boolean apply(Entity p_apply_1_)
                {
                    return p_apply_1_.canBeCollidedWith();
                }
            }));
            double d2 = d1;

            for (int j = 0; j < list.size(); ++j)
            {
                Entity entity1 = (Entity)list.get(j);
                float f1 = entity1.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double)f1, (double)f1, (double)f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3))
                {
                    if (d2 >= 0.0D)
                    {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                }
                else if (movingobjectposition != null)
                {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D)
                    {
                        boolean flag1 = false;

                        if (Reflector.ForgeEntity_canRiderInteract.exists())
                        {
                            flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract, new Object[0]);
                        }

                        if (!flag1 && entity1 == entity.ridingEntity)
                        {
                            if (d2 == 0.0D)
                            {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        }
                        else
                        {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > 3.0D)
            {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, (EnumFacing)null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null))
            {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            mc.mcProfiler.endSection();
            return objectMouseOver;
        }
        return null;
    }
    
}
