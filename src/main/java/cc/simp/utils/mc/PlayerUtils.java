package cc.simp.utils.mc;

import cc.simp.utils.Util;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class PlayerUtils extends Util {

    public static boolean isHoldingSword() {
        final ItemStack stack;
        return (stack = mc.thePlayer.getCurrentEquippedItem()) != null && stack.getItem() instanceof ItemSword;
    }

    public static boolean isBlockAbovePlayer(final EntityPlayer player, final int distance) {
        double horizontal = 0.6;
        final World world = player.worldObj;
        final AxisAlignedBB bb = player.getEntityBoundingBox();
        final double centerX = (bb.minX + bb.maxX) / 2.0;
        final double centerZ = (bb.minZ + bb.maxZ) / 2.0;
        final double minX = centerX - horizontal;
        final double maxX = centerX + horizontal;
        final double minZ = centerZ - horizontal;
        final double maxZ = centerZ + horizontal;
        for (int i = 1; i <= distance; ++i) {
            final double checkY = bb.maxY + i;
            if (checkY >= 256.0) {
                break;
            }
            for (double x = minX; x <= maxX; x += 0.3) {
                for (double z = minZ; z <= maxZ; z += 0.3) {
                    final BlockPos pos = new BlockPos(x, checkY, z);
                    if (!world.isAirBlock(pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
