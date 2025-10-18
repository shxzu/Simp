package cc.simp.utils.mc;

import cc.simp.utils.Util;
import cc.simp.utils.misc.PathFinder;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PathFinderUtils extends Util {

    public static List<Vec3> computePath(final Vec3 from, final Vec3 to, final boolean exact) {
        return computePath(from, to, exact, 9.5);
    }

    public static List<Vec3> computePath(Vec3 from, final Vec3 to, final boolean exact, final double step) {
        final BlockPos blockPos = new BlockPos(from);
        final IBlockState state = mc.theWorld.getBlockState(blockPos);

        if (state == null) {
            return null;
        }

        final Block block = state.getBlock();

        if (block == null) {
            return null;
        }

        if (!canPassThroughMaterial(block)) {
            from = from.addVector(0, 1, 0);
        }

        final PathFinder pathFinder = new PathFinder(from, to);
        pathFinder.compute();

        int i = 0;
        Vec3 lastLoc = null;
        Vec3 lastDashLoc = null;
        final ArrayList<Vec3> path = new ArrayList<>();
        final ArrayList<Vec3> pathFinderPath = pathFinder.getPath();
        for (final Vec3 pathElm : pathFinderPath) {
            if (i == 0 || i == pathFinderPath.size() - 1) {
                path.add(pathElm.addVector(0.5, 0, 0.5));
                lastDashLoc = pathElm;
            } else {
                boolean canContinue = true;
                if (pathElm.squareDistanceTo(lastDashLoc) > step * step) {
                    canContinue = false;
                } else {
                    final double smallX = Math.min(lastDashLoc.xCoord, pathElm.xCoord);
                    final double smallY = Math.min(lastDashLoc.yCoord, pathElm.yCoord);
                    final double smallZ = Math.min(lastDashLoc.zCoord, pathElm.zCoord);
                    final double bigX = Math.max(lastDashLoc.xCoord, pathElm.xCoord);
                    final double bigY = Math.max(lastDashLoc.yCoord, pathElm.yCoord);
                    final double bigZ = Math.max(lastDashLoc.zCoord, pathElm.zCoord);
                    cordsLoop:
                    for (int x = (int) smallX; x <= bigX; x++) {
                        for (int y = (int) smallY; y <= bigY; y++) {
                            for (int z = (int) smallZ; z <= bigZ; z++) {
                                if (!PathFinder.checkPositionValidity(x, y, z, false)) {
                                    canContinue = false;
                                    break cordsLoop;
                                }
                            }
                        }
                    }
                }

                if (!canContinue) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }
            lastLoc = pathElm;
            i++;
        }

        if (exact) {
            path.add(to);
        }

        return path;
    }

    private static boolean canPassThroughMaterial(final Block block) {
        final Material material = block.getMaterial();

        return material == Material.air || material == Material.plants || material == Material.vine || block == Blocks.ladder || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.wall_sign || block == Blocks.standing_sign;
    }
}
