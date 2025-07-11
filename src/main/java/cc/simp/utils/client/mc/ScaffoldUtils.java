package cc.simp.utils.client.mc;

import cc.simp.Simp;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class ScaffoldUtils extends Util {
    public static class BlockCache {

        private final BlockPos position;
        private final EnumFacing facing;
        private final Vec3 hitVec;

        public BlockCache(final BlockPos position, final EnumFacing facing, Vec3 hitVec) {
            this.position = position;
            this.facing = facing;
            this.hitVec = hitVec;
        }

        public BlockPos getPosition() {
            return this.position;
        }

        public EnumFacing getFacing() {
            return this.facing;
        }

        public Vec3 getHitVec() {
            return this.hitVec;
        }
    }

    public static double getYLevel() {
        if (!ScaffoldModule.keepYProperty.getValue() || ScaffoldModule.allowSpeedModuleProperty.getValue()
                && !Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            return mc.thePlayer.posY - 1.0;
        }
        return mc.thePlayer.posY - 1.0 >= ScaffoldModule.keepYCoord
                && Math.max(mc.thePlayer.posY, ScaffoldModule.keepYCoord)
                - Math.min(mc.thePlayer.posY, ScaffoldModule.keepYCoord) <= 3.0
                && !mc.gameSettings.keyBindJump.isKeyDown() ? ScaffoldModule.keepYCoord : mc.thePlayer.posY - 1.0;
    }

    public static BlockCache getBlockInfo() {
        final BlockPos belowBlockPos = new BlockPos(mc.thePlayer.posX, getYLevel(), mc.thePlayer.posZ);
        if (mc.theWorld.getBlockState(belowBlockPos).getBlock() instanceof BlockAir) {
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) {
                    for (int i = 1; i > -3; i -= 2) {
                        final BlockPos blockPos = belowBlockPos.add(x * i, 0, z * i);
                        if (mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockAir) {
                            for (EnumFacing direction : EnumFacing.values()) {
                                final BlockPos block = blockPos.offset(direction);
                                final Material material = mc.theWorld.getBlockState(block).getBlock().getMaterial();
                                if (material.isSolid() && !material.isLiquid()) {
                                    return new BlockCache(block, direction.getOpposite(), getVec3(block, direction.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Vec3 getVec3(BlockPos pos, EnumFacing face) {
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.5;
        double z = (double) pos.getZ() + 0.5;
        x += (double) face.getFrontOffsetX() / 2.0;
        z += (double) face.getFrontOffsetZ() / 2.0;
        y += (double) face.getFrontOffsetY() / 2.0;
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += new Random().nextDouble() / 2.0 - 0.25;
            z += new Random().nextDouble() / 2.0 - 0.25;
        } else {
            y += new Random().nextDouble() / 2.0 - 0.25;
        }
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += new Random().nextDouble() / 2.0 - 0.25;
        }
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += new Random().nextDouble() / 2.0 - 0.25;
        }
        return new Vec3(x, y, z);
    }

    public static int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (isBlockValid(itemBlock.getBlock())) {
                    return i;
                }
            }
        }
        Logger.chatPrint("No Blocks!");
        Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).setEnabled(false);
        return mc.thePlayer.inventory.currentItem;
    }

    public static int getBlockSlotCycle(int start) {
        for (int i = (start + 1) % 9; i != start; i = (i + 1) % 9) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (isBlockValid(itemBlock.getBlock())) {
                    return i;
                }
            }
        }

        final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem];
        if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
            return mc.thePlayer.inventory.currentItem;
        }

        Logger.chatPrint("No Blocks");
        Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).setEnabled(false);
        return mc.thePlayer.inventory.currentItem;
    }

    public static int getBlockCount() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (isBlockValid(itemBlock.getBlock())) {
                    count += itemStack.stackSize;
                }
            }
        }
        return count;
    }

    public static boolean isBlockValid(final Block block) {
        return (block.isFullBlock() || block == Blocks.glass || block == Blocks.stained_glass) && block != Blocks.sand
                && block != Blocks.gravel && block != Blocks.dispenser && block != Blocks.command_block
                && block != Blocks.noteblock && block != Blocks.furnace && block != Blocks.crafting_table
                && block != Blocks.tnt && block != Blocks.dropper && block != Blocks.soul_sand && block != Blocks.ice
                && block != Blocks.packed_ice && block != Blocks.beacon;
    }

    public static boolean isAirBlock(final BlockPos blockPos) {
        final Block block = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).getBlock();
        return block instanceof BlockAir;
    }

    public static float getYaw(float yaw) {
        Vector2f from = new Vector2f((float) mc.thePlayer.lastTickPosX, (float) mc.thePlayer.lastTickPosZ),
                to = new Vector2f((float) mc.thePlayer.posX, (float) mc.thePlayer.posZ),
                diff = new Vector2f(to.x - from.x, to.y - from.y);

        double x = diff.x, z = diff.y;
        if (x != 0 && z != 0) {
            yaw = (float) Math.toDegrees((Math.atan2(-x, z) + MathHelper.PI2) % MathHelper.PI2);
        }
        return yaw;
    }


    public static float getScaffoldFixedYaw() {
        float realYaw = mc.thePlayer.rotationYaw;
        if (mc.gameSettings.keyBindBack.isPressed()) {
            realYaw += 180.0f;
            if (mc.gameSettings.keyBindLeft.isPressed()) {
                realYaw += 45.0f;
            }
            else if (mc.gameSettings.keyBindRight.isPressed()) {
                realYaw -= 45.0f;
            }
        }
        else if (mc.gameSettings.keyBindForward.isPressed()) {
            realYaw -= 180.0f;
            if (mc.gameSettings.keyBindLeft.isPressed()) {
                realYaw -= 45.0f;
            }
            else if (mc.gameSettings.keyBindRight.isPressed()) {
                realYaw += 45.0f;
            }
        }
        else if (mc.gameSettings.keyBindRight.isPressed()) {
            realYaw += 90.0f;
        }
        else if (mc.gameSettings.keyBindLeft.isPressed()) {
            realYaw -= 90.0f;
        }
        return realYaw;
    }

    public static float getHypixelNCPYaw() {
        float yaw = MovementUtils.getDirection();
        float upperCandidate;
        float lowerCandidate;
        float snapped = (float)Math.round(yaw / 45.0f) * 45.0f;
        if (Math.abs(snapped % 90.0f) < 0.001f) {
            lowerCandidate = snapped - 115.0f;
            upperCandidate = snapped + 115.0f;
        } else {
            lowerCandidate = snapped - 145.0f;
            upperCandidate = snapped + 145.0f;
        }
        snapped = Math.abs(yaw - lowerCandidate) <= Math.abs(upperCandidate - yaw) ? lowerCandidate : upperCandidate;
        return snapped;
    }

}
