package cc.simp.utils.mc;

import cc.simp.utils.Util;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;

public class InventoryUtils extends Util {

    public static int getBucketSlot() {
        int item = -1;
        for (int i = 36; i < 45; ++i) {
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBucket) {
                Item itemSlot = (ItemBucket)InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                if (itemSlot == Items.water_bucket) {
                    item = i - 36;
                }
            }
        }
        return item;
    }

    public static int getCobwebSlot() {
        int item = -1;
        for (int i = 36; i < 45; ++i) {
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock) {
                final ItemBlock block = (ItemBlock)InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                if (block.getBlock() == Blocks.web) {
                    item = i - 36;
                }
            }
        }
        return item;
    }

    public static int getBlockSlot(Block block) {
        for (int i = 0; i < 9; i++) {
            ItemStack is = mc.thePlayer.inventory.mainInventory[i];
            if (is != null && is.getItem() instanceof ItemBlock && ((ItemBlock) is.getItem()).getBlock() == block) {
                return i;
            }
        }
        return -1;
    }

    public static Item getHeldItem() {
        if (mc.thePlayer == null || mc.thePlayer.getCurrentEquippedItem() == null) return null;
        return mc.thePlayer.getCurrentEquippedItem().getItem();
    }

    public static boolean isHoldingSword() {
        return getHeldItem() instanceof ItemSword;
    }

    public static void click(int slot, int mouseButton, boolean shiftClick) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, mouseButton, shiftClick ? 1 : 0, mc.thePlayer);
    }

    public static void drop(int slot) {
        mc.playerController.windowClick(0, slot, 1, 4, mc.thePlayer);
    }

    public static void swap(int slot, int hSlot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hSlot, 2, mc.thePlayer);
    }

    public static float getSwordStrength(ItemStack stack) {
        if (stack.getItem() instanceof ItemSword) {
            ItemSword sword = (ItemSword) stack.getItem();
            float sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25F;
            float fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 1.5F;
            return sword.getDamageVsEntity() + sharpness + fireAspect;
        }
        return 0;
    }

    public static boolean isItemEmpty(Item item) {
        return item == null || Item.getIdFromItem(item) == 0;
    }
}