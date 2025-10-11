package cc.simp.utils.mc;

import cc.simp.processes.RotationProcess;
import cc.simp.utils.Util;
import cc.simp.utils.client.EnumFacingOffset;
import cc.simp.utils.client.MathUtils;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerUtils extends Util {
    public static Block block(final double x, final double y, final double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }
    public static Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return block(mc.thePlayer.posX + offsetX, mc.thePlayer.posY + offsetY, mc.thePlayer.posZ + offsetZ);
    }

    public Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ) {
        return getPlacePossibility(offsetX, offsetY, offsetZ, null);
    }

    // This methods purpose is to get block placement possibilities, blocks are 1 unit thick so please don't change it to 0.5 it causes bugs.
    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ, Integer plane) {

        final List<Vec3> possibilities = new ArrayList<>();
        final int range = (int) (5 + (Math.abs(offsetX) + Math.abs(offsetZ)));

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = PlayerUtils.blockRelativeToPlayer(x, y, z);

                    if (!block.isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z))) {
                        for (int x2 = -1; x2 <= 1; x2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));

                        for (int y2 = -1; y2 <= 1; y2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));

                        for (int z2 = -1; z2 <= 1; z2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) > 5 || !(PlayerUtils.block(vec3.xCoord, vec3.yCoord, vec3.zCoord).isReplaceable(mc.theWorld, new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))));

        if (possibilities.isEmpty()) return null;

        if (plane != null) {
            possibilities.removeIf(vec3 -> Math.floor(vec3.yCoord + 1) != plane);
        }

        possibilities.sort(Comparator.comparingDouble(vec3 -> {

            final double d0 = (mc.thePlayer.posX + offsetX) - vec3.xCoord;
            final double d1 = (mc.thePlayer.posY - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ + offsetZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

        }));

        return possibilities.isEmpty() ? null : possibilities.get(0);
    }

    public Vec3 getPlacePossibility() {
        return getPlacePossibility(0, 0, 0);
    }

    public EnumFacingOffset getEnumFacing(final Vec3 position) {
        return getEnumFacing(position, false);
    }

    public static EnumFacingOffset getEnumFacing(final Vec3 position, boolean downwards) {
        List<EnumFacingOffset> possibleFacings = new ArrayList<>();
        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (!(PlayerUtils.block(position.xCoord, position.yCoord, position.zCoord + z2).isReplaceable(mc.theWorld, new BlockPos(position.xCoord, position.yCoord, position.zCoord + z2)))) {
                if (z2 < 0) {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.SOUTH, new Vec3(0, 0, z2)));
                } else {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.NORTH, new Vec3(0, 0, z2)));
                }
            }
        }

        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (!(PlayerUtils.block(position.xCoord + x2, position.yCoord, position.zCoord).isReplaceable(mc.theWorld, new BlockPos(position.xCoord + x2, position.yCoord, position.zCoord)))) {
                if (x2 > 0) {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.WEST, new Vec3(x2, 0, 0)));
                } else {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.EAST, new Vec3(x2, 0, 0)));
                }
            }
        }

        possibleFacings.sort(Comparator.comparingDouble(enumFacing -> {
            double enumFacingRotations = Math.toDegrees(Math.atan2(enumFacing.getOffset().zCoord,
                    enumFacing.getOffset().xCoord)) % 360;
            double rotations = RotationProcess.rotations.x % 360 + 90;

            return Math.abs(MathUtils.wrappedDifference(enumFacingRotations, rotations));
        }));

        if (!possibleFacings.isEmpty()) return possibleFacings.get(0);

        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (!(PlayerUtils.block(position.xCoord, position.yCoord + y2, position.zCoord).isReplaceable(mc.theWorld, new BlockPos(position.xCoord, position.yCoord + y2, position.zCoord)))) {
                if (y2 < 0) {
                    return new EnumFacingOffset(EnumFacing.UP, new Vec3(0, y2, 0));
                } else if (downwards) {
                    return new EnumFacingOffset(EnumFacing.DOWN, new Vec3(0, y2, 0));
                }
            }
        }

        return null;
    }

    public static Block blockAheadOfPlayer(final double offsetXZ, final double offsetY) {
        return blockRelativeToPlayer(-Math.sin(MovementUtils.direction()) * offsetXZ, offsetY, Math.cos(MovementUtils.direction()) * offsetXZ);
    }


}
