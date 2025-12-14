package cc.simp.utils.mc;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import org.lwjgl.input.Keyboard;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import cc.simp.utils.Util;
import cc.simp.utils.client.Timer;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockTNT;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.Arrays;
import java.util.List;

public class InventoryUtils extends Util {
    public static final List<Block> blacklist = Arrays.asList(Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest,
            Blocks.trapped_chest, Blocks.anvil, Blocks.sand, Blocks.web, Blocks.torch,
            Blocks.crafting_table, Blocks.furnace, Blocks.waterlily, Blocks.dispenser,
            Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate, Blocks.noteblock,
            Blocks.dropper, Blocks.tnt, Blocks.standing_banner, Blocks.wall_banner, Blocks.redstone_torch);

    public static Timer timer = new Timer();
    public static boolean isInventoryOpen;
    static KeyBinding[] moveKeys = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak};
    public static List<Block> invalidBlocks = Arrays.asList(Blocks.enchanting_table, Blocks.carpet, Blocks.glass_pane, Blocks.ladder, Blocks.web, Blocks.stained_glass_pane, Blocks.iron_bars, Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.ladder, Blocks.soul_sand, Blocks.ice, Blocks.packed_ice, Blocks.sand, Blocks.flowing_lava, Blocks.snow_layer, Blocks.chest, Blocks.ender_chest, Blocks.torch, Blocks.anvil, Blocks.trapped_chest, Blocks.noteblock, Blocks.jukebox, Blocks.wooden_pressure_plate, Blocks.stone_pressure_plate, Blocks.light_weighted_pressure_plate, Blocks.heavy_weighted_pressure_plate, Blocks.stone_button, Blocks.tnt, Blocks.wooden_button, Blocks.lever, Blocks.crafting_table, Blocks.furnace, Blocks.stone_slab, Blocks.wooden_slab, Blocks.stone_slab2, Blocks.brown_mushroom, Blocks.red_mushroom, Blocks.gold_block, Blocks.red_flower, Blocks.yellow_flower, Blocks.flower_pot);

    public static int getBucketSlot() {
        int item = -1;
        for (int i = 36; i < 45; ++i) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBucket) {
                Item itemSlot = (ItemBucket) mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
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
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock) {
                final ItemBlock block = (ItemBlock) mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                if (block.getBlock() == Blocks.web) {
                    item = i - 36;
                }
            }
        }
        return item;
    }

    public static ItemStack getBlockSlotInventory() {
        ItemStack item = null;
        int stacksize = 0;
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock && !invalidBlocks.contains(((ItemBlock) mc.thePlayer.getHeldItem().getItem()).getBlock())) {
            return mc.thePlayer.getHeldItem();
        }
        int i = 9;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock && !invalidBlocks.contains(((ItemBlock) mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem()).getBlock()) && mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize >= stacksize) {
                item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                stacksize = mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize;
            }
            ++i;
        }
        return item;
    }


    public static int getEmptyBucketSlot() {
        int item = -1;
        int stacksize = 0;
        int i = 36;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() == Items.bucket) {
                item = i - 36;
                stacksize = mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize;
            }
            ++i;
        }
        return item;
    }

    public static ItemStack getBucketSlotInventory() {
        ItemStack item = null;
        int stacksize = 0;
        int i = 9;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() == Items.water_bucket) {
                item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                stacksize = mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize;
            }
            ++i;
        }
        return item;
    }

    public static int getProjectileSlot() {
        int item = -1;
        int stacksize = 0;
        int i = 36;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemSnowball || mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemEgg || mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemFishingRod) && mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize >= stacksize) {
                item = i - 36;
                stacksize = mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize;
            }
            ++i;
        }
        return item;
    }

    public static ItemStack getProjectileSlotInventory() {
        ItemStack item = null;
        int stacksize = 0;
        int i = 9;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemSnowball || mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemEgg || mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemFishingRod) && mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize >= stacksize) {
                item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                stacksize = mc.thePlayer.inventoryContainer.getSlot((int) i).getStack().stackSize;
            }
            ++i;
        }
        return item;
    }

    public static float getProtection(ItemStack stack) {
        float prot = 0.0f;
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            prot = (float) ((double) (prot + (float) armor.damageReduceAmount) + (double) ((100 - armor.damageReduceAmount) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) * 0.0075);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 50.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) / 100.0);
        }
        return prot;
    }

    public static boolean isBestArmor(ItemStack stack, int type) {
        float prot = InventoryUtils.getProtection(stack);
        String strType = "";
        if (type == 1) {
            strType = "helmet";
        } else if (type == 2) {
            strType = "chestplate";
        } else if (type == 3) {
            strType = "leggings";
        } else if (type == 4) {
            strType = "boots";
        }
        if (!stack.getUnlocalizedName().contains(strType)) {
            return false;
        }
        int i = 5;
        while (i < 45) {
            ItemStack is;
            if (Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getHasStack() && InventoryUtils.getProtection(is = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getStack()) > prot && is.getUnlocalizedName().contains(strType)) {
                return false;
            }
            ++i;
        }
        return true;
    }

    public static void drop(int slot) {
        Minecraft.getMinecraft().playerController.windowClick(Minecraft.getMinecraft().thePlayer.inventoryContainer.windowId, slot, 1, 4, Minecraft.getMinecraft().thePlayer);
    }

    public static void shiftClick(int slot) {
        Minecraft.getMinecraft().playerController.windowClick(Minecraft.getMinecraft().thePlayer.inventoryContainer.windowId, slot, 0, 1, Minecraft.getMinecraft().thePlayer);
    }

    public static boolean isBadStack(ItemStack is, boolean preferSword, boolean keepTools) {
        int type = 1;
        while (type < 5) {
            String strType = "";
            if (type == 1) {
                strType = "helmet";
            } else if (type == 2) {
                strType = "chestplate";
            } else if (type == 3) {
                strType = "leggings";
            } else if (type == 4) {
                strType = "boots";
            }
            if (is.getItem() instanceof ItemArmor && !InventoryUtils.isBestArmor(is, type) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack() && InventoryUtils.isBestArmor(InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack(), type) && InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack().getUnlocalizedName().contains(strType) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            ++type;
        }
        if (is.getItem() instanceof ItemSword && is != InventoryUtils.bestWeapon() && !preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemSword && is != InventoryUtils.bestSword() && preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemBow && is != InventoryUtils.bestBow()) {
            return true;
        }
        if (keepTools) {
            if (is.getItem() instanceof ItemAxe && is != InventoryUtils.bestAxe() && (preferSword || is != InventoryUtils.bestWeapon())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && is != InventoryUtils.bestPick() && (preferSword || is != InventoryUtils.bestWeapon())) {
                return true;
            }
            if (is.getItem() instanceof ItemSpade && is != InventoryUtils.bestShovel()) {
                return true;
            }
        } else {
            if (is.getItem() instanceof ItemAxe && (preferSword || is != InventoryUtils.bestWeapon())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && (preferSword || is != InventoryUtils.bestWeapon())) {
                return true;
            }
            if (is.getItem() instanceof ItemSpade) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBadStackStealer(ItemStack is, boolean preferSword, boolean keepTools) {
        int type = 1;
        while (type < 5) {
            String strType = "";
            if (type == 1) {
                strType = "helmet";
            } else if (type == 2) {
                strType = "chestplate";
            } else if (type == 3) {
                strType = "leggings";
            } else if (type == 4) {
                strType = "boots";
            }
            if (is.getItem() instanceof ItemArmor && !InventoryUtils.isBestArmor(is, type) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack() && InventoryUtils.isBestArmor(InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack(), type) && InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack().getUnlocalizedName().contains(strType) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            ++type;
        }
        if (is.getItem() instanceof ItemSword && InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill() && !preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemSword && InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestSwordSkill() && preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemBow && InventoryUtils.getBowSkill(is) <= InventoryUtils.bestBowSkill()) {
            return true;
        }
        if (keepTools) {
            if (is.getItem() instanceof ItemAxe && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestAxeSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestPickSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemSpade && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestShovelSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
        } else {
            if (is.getItem() instanceof ItemAxe && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemSpade && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
        }
        return false;
    }

    public static float getWeaponSkill(ItemStack is) {
        return InventoryUtils.getItemDamage(is);
    }

    public static float getBowSkill(ItemStack is) {
        return InventoryUtils.getBowDamage(is);
    }

    public static float getToolSkill(ItemStack is) {
        return InventoryUtils.getToolRating(is);
    }

    public static float bestWeaponSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            float toolDamage;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (toolDamage = InventoryUtils.getItemDamage(is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack())) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestSwordSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            float swordDamage;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSword && (swordDamage = InventoryUtils.getItemDamage(is)) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestBowSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            float bowDamage;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemBow && (bowDamage = InventoryUtils.getBowDamage(is)) >= itemDamage) {
                itemDamage = InventoryUtils.getBowDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestAxeSkill() {
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemAxe && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static float bestPickSkill() {
        Object bestTool = null;
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemPickaxe && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static float bestShovelSkill() {
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSpade && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static ItemStack bestWeapon() {
        ItemStack bestWeapon = null;
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolDamage;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && ((is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSword || is.getItem() instanceof ItemAxe || is.getItem() instanceof ItemPickaxe) && (toolDamage = InventoryUtils.getItemDamage(is)) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
                bestWeapon = is;
            }
            ++i;
        }
        return bestWeapon;
    }

    public static ItemStack bestSword() {
        ItemStack bestSword = null;
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            float swordDamage;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSword && (swordDamage = InventoryUtils.getItemDamage(is)) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
                bestSword = is;
            }
            ++i;
        }
        return bestSword;
    }

    public static ItemStack bestBow() {
        ItemStack bestBow = null;
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            float bowDamage;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemBow && (bowDamage = InventoryUtils.getBowDamage(is)) >= itemDamage) {
                itemDamage = InventoryUtils.getBowDamage(is);
                bestBow = is;
            }
            ++i;
        }
        return bestBow;
    }

    public static ItemStack bestAxe() {
        ItemStack bestTool = null;
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemAxe && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
                bestTool = is;
            }
            ++i;
        }
        return bestTool;
    }

    public static ItemStack bestPick() {
        ItemStack bestTool = null;
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemPickaxe && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
                bestTool = is;
            }
            ++i;
        }
        return bestTool;
    }

    public static ItemStack bestShovel() {
        ItemStack bestTool = null;
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            float toolSkill;
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSpade && (toolSkill = InventoryUtils.getToolRating(is)) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
                bestTool = is;
            }
            ++i;
        }
        return bestTool;
    }

    public static float getToolRating(ItemStack itemStack) {
        float damage = InventoryUtils.getToolMaterialRating(itemStack, false);
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack) * 2.0f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.1f;
        return damage += (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 1.0E-12f;
    }

    public static float getItemDamage(ItemStack itemStack) {
        float damage = InventoryUtils.getToolMaterialRating(itemStack, true);
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.01f;
        damage += (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 1.0E-12f;
        if (itemStack.getItem() instanceof ItemSword) {
            damage = (float) ((double) damage + 0.2);
        }
        return damage;
    }

    public static float getBowDamage(ItemStack itemStack) {
        float damage = 5.0f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack) * 1.25f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack) * 0.75f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.1f;
        return damage += (float) itemStack.getMaxDamage() - (float) itemStack.getItemDamage() * 0.001f;
    }

    public static float getToolMaterialRating(ItemStack itemStack, boolean checkForDamage) {
        float rating;
        block78:
        {
            Item is;
            block81:
            {
                block79:
                {
                    block76:
                    {
                        is = itemStack.getItem();
                        rating = 0.0f;
                        if (!(is instanceof ItemSword)) break block76;
                        switch (((ItemSword) is).getToolMaterialName()) {
                            case "WOOD": {
                                rating = 4.0f;
                                break;
                            }
                            case "GOLD": {
                                rating = 4.0f;
                                break;
                            }
                            case "STONE": {
                                rating = 5.0f;
                                break;
                            }
                            case "IRON": {
                                rating = 6.0f;
                                break;
                            }
                            case "EMERALD": {
                                rating = 7.0f;
                            }
                        }
                        break block78;
                    }
                    if (!(is instanceof ItemPickaxe)) break block79;
                    switch (((ItemPickaxe) is).getToolMaterialName()) {
                        case "WOOD": {
                            rating = 2.0f;
                            break;
                        }
                        case "GOLD": {
                            rating = 2.0f;
                            break;
                        }
                        case "STONE": {
                            rating = 3.0f;
                            break;
                        }
                        case "IRON": {
                            rating = checkForDamage ? 4 : 40;
                            break;
                        }
                        case "EMERALD": {
                            rating = checkForDamage ? 5 : 50;
                        }
                    }
                    break block78;
                }
                if (!(is instanceof ItemAxe)) break block81;
                switch (((ItemAxe) is).getToolMaterialName()) {
                    case "WOOD": {
                        rating = 3.0f;
                        break;
                    }
                    case "GOLD": {
                        rating = 3.0f;
                        break;
                    }
                    case "STONE": {
                        rating = 4.0f;
                        break;
                    }
                    case "IRON": {
                        rating = 5.0f;
                        break;
                    }
                    case "EMERALD": {
                        rating = 6.0f;
                    }
                }
                break block78;
            }
            if (!(is instanceof ItemSpade)) break block78;
            switch (((ItemSpade) is).getToolMaterialName()) {
                case "WOOD": {
                    rating = 1.0f;
                    break;
                }
                case "GOLD": {
                    rating = 1.0f;
                    break;
                }
                case "STONE": {
                    rating = 2.0f;
                    break;
                }
                case "IRON": {
                    rating = 3.0f;
                    break;
                }
                case "EMERALD": {
                    rating = 4.0f;
                }
            }
        }
        return rating;
    }

    public static void swap(int slot, int hSlot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hSlot, 2, mc.thePlayer);
    }

    public static void openInv(boolean silent) {
        if (silent && !isInventoryOpen && !(mc.currentScreen instanceof GuiInventory)) {
            PacketUtils.sendSilentPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            isInventoryOpen = true;
        }
    }

    public static void closeInv(boolean silent) {
        if (silent && isInventoryOpen && !(mc.currentScreen instanceof GuiInventory)) {
            PacketUtils.sendSilentPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            KeyBinding[] keyBindingArray = moveKeys;
            int n = moveKeys.length;
            int n2 = 0;
            while (n2 < n) {
                KeyBinding bind = keyBindingArray[n2];
                KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown((int) bind.getKeyCode()));
                ++n2;
            }
            isInventoryOpen = false;
        }
    }

    public static int findBlock() {
        for (int i = 36; i < 45; i++) {
            final ItemStack item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (item != null && item.getItem() instanceof ItemBlock && item.stackSize > 0) {
                final Block block = ((ItemBlock) item.getItem()).getBlock();
                if ((block.isFullBlock() || block instanceof BlockGlass || block instanceof BlockStainedGlass || block instanceof BlockTNT) && !blacklist.contains(block)) {
                    return i - 36;
                }
            }
        }

        return -1;
    }

    public static boolean isHoldingSword() {
        final ItemStack stack;
        return (stack = mc.thePlayer.getCurrentEquippedItem()) != null && stack.getItem() instanceof ItemSword;
    }

    public static boolean isContainerBlock(ItemBlock itemBlock) {
        Block block = itemBlock.getBlock();
        if (PlayerUtils.isInteractable(block)) return false;
        return PlayerUtils.isSolid(block);
    }

    public static boolean isBlock(ItemStack itemStack) {
        if (itemStack == null || itemStack.stackSize < 1) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof ItemBlock) {
            return isContainerBlock((ItemBlock) item);
        }
        return false;
    }
}
