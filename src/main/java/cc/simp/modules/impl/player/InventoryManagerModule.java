package cc.simp.modules.impl.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.mc.InventoryUtils;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.mc.ScaffoldUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemGlassBottle;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Inventory Manager", category = ModuleCategory.PLAYER)
public class InventoryManagerModule extends Module {

    // woah I pasted from tenacity again!!

    private static final DoubleProperty delay = new DoubleProperty("Delay", 75, 0, 500, 25);
    private static final Property<Boolean> onlyWhileNotMoving = new Property<>("Only Not While Moving", true);
    private static final Property<Boolean> inventoryOnly = new Property<>("Inv Only", true);
    private static final Property<Boolean> inventoryPackets = new Property<>("Send Inv Packets", false, inventoryOnly::getValue);
    private static final Property<Boolean> swapBlocks = new Property<>("Swap Blocks", true);
    private static final Property<Boolean> dropArchery = new Property<>("Drop Archery", false);
    private static final Property<Boolean> moveArrows = new Property<>("Move Arrows", false);
    private static final Property<Boolean> dropFood = new Property<>("Drop Food", false);
    private static final Property<Boolean> dropShears = new Property<>("Drop Shears", true);
    private static final DoubleProperty slotWeapon = new DoubleProperty("Weapon Slot", 1, 1, 9, 1);
    private static final DoubleProperty slotPick = new DoubleProperty("Pickaxe Slot", 3, 1, 9, 1);
    private static final DoubleProperty slotAxe = new DoubleProperty("Axe Slot", 5, 1, 9, 1);
    private static final DoubleProperty slotShovel = new DoubleProperty("Shovel Slot", 7, 1, 9, 1);
    private static final DoubleProperty slotBow = new DoubleProperty("Bow Slot", 6, 1, 9, 1);
    private static final DoubleProperty slotBlock = new DoubleProperty("Block Slot", 2, 1, 9, 1);
    private static final DoubleProperty slotGapple = new DoubleProperty("Gapple Slot", 4, 1, 9, 1);


    private final String[] blacklist = {"tnt", "stick", "egg", "string", "cake", "mushroom", "flint", "compass", "dyePowder", "feather", "bucket", "chest", "snow", "fish", "enchant", "exp", "anvil", "torch", "seeds", "leather", "reeds", "skull", "record", "snowball", "piston"};
    private final String[] serverItems = {"selector", "tracking compass", "(right click)", "tienda ", "perfil", "salir", "shop", "collectibles", "game", "profil", "lobby", "show all", "hub", "friends only", "cofre", "(click", "teleport", "play", "exit", "hide all", "jeux", "gadget", " (activ", "emote", "amis", "bountique", "choisir", "choose ", "recipe book", "click derecho", "todos", "teletransportador", "configuraci", "jugar de nuevo"};
    private final List<Integer> badPotionIDs = new ArrayList<>(Arrays.asList(Potion.moveSlowdown.getId(), Potion.weakness.getId(), Potion.poison.getId(), Potion.harm.getId()));

    private final Timer timer = new Timer();
    public static boolean isInvOpen;

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {

        if (event.isPre()) {
            if (!inventoryOnly.getValue()) {
                if (!mc.thePlayer.isUsingItem() && (mc.currentScreen == null || mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiIngameMenu)) {
                    if (isReady()) {
                        Slot slot = ItemType.WEAPON.getSlot();
                        if (!slot.getHasStack() || !isBestWeapon(slot.getStack())) {
                            getBestWeapon();
                        }
                    }
                    getBestPickaxe();
                    getBestAxe();
                    getBestShovel();
                    dropItems();
                    swapBlocks();
                    getBestBow();
                    moveArrows();
                    moveFood();
                    for (int armorSlot = 5; armorSlot < 9; armorSlot++) {
                        if (equipBest(armorSlot)) {
                            break;
                        }
                    }
                }
            } else {

                if (!mc.thePlayer.isUsingItem() && mc.currentScreen != null && mc.currentScreen instanceof GuiInventory) {
                    if (isReady()) {
                        Slot slot = ItemType.WEAPON.getSlot();
                        if (!slot.getHasStack() || !isBestWeapon(slot.getStack())) {
                            getBestWeapon();
                        }
                    }
                    getBestPickaxe();
                    getBestAxe();
                    getBestShovel();
                    dropItems();
                    swapBlocks();
                    getBestBow();
                    moveArrows();
                    moveFood();
                    for (int armorSlot = 5; armorSlot < 9; armorSlot++) {
                        if (equipBest(armorSlot)) {
                            break;
                        }
                    }
                }

            }
        }
    };


    private boolean isReady() {
        return timer.hasTimeElapsed(delay.getValue().longValue());
    }

    public static float getDamageScore(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;

        float damage = 0;
        Item item = stack.getItem();

        if (item instanceof ItemSword) {
            damage += ((ItemSword) item).getDamageVsEntity();
        } else if (item instanceof ItemTool) {
            damage += item.getMaxDamage();
        }

        damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25F +
                EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.1F;

        return damage;
    }

    public static float getProtScore(ItemStack stack) {
        float prot = 0;
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            prot += armor.damageReduceAmount + ((100 - armor.damageReduceAmount) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) * 0.0075F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 25.F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, stack) / 100F;
        }
        return prot;
    }

    private void dropItems() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            if (canContinue()) return;
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            ItemStack is = slot.getStack();
            if (is != null && isBadItem(is, i, false)) {
                InventoryUtils.drop(i);
                timer.reset();
                break;
            }
        }
    }

    private boolean isBestWeapon(ItemStack is) {
        if (is == null) return false;
        float damage = getDamageScore(is);
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is2 = slot.getStack();
                if (getDamageScore(is2) > damage && is2.getItem() instanceof ItemSword) {
                    return false;
                }
            }
        }
        return is.getItem() instanceof ItemSword;
    }

    private void getBestWeapon() {
        for (int i = 9; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() instanceof ItemSword && isBestWeapon(is) && getDamageScore(is) > 0) {
                swap(i, ItemType.WEAPON.getDesiredSlot() - 36);
                break;
            }
        }
    }

    // stealing is true when called from the ChestStealer module because returning true = ignore, but in invmanager returning true = drop
    public boolean isBadItem(ItemStack stack, int slot, boolean stealing) {
        Item item = stack.getItem();
        String stackName = stack.getDisplayName().toLowerCase(), ulName = item.getUnlocalizedName();
        if (Arrays.stream(serverItems).anyMatch(stackName::contains)) return stealing;

        if (item instanceof ItemBlock) {
            return !ScaffoldUtils.isBlockValid(((ItemBlock) item).getBlock());
        }

        if (stealing) {
            if (isBestWeapon(stack) || isBestAxe(stack) || isBestPickaxe(stack) || isBestBow(stack) || isBestShovel(stack)) {
                return false;
            }
            if (item instanceof ItemArmor) {
                for (int type = 1; type < 5; type++) {
                    ItemStack is = mc.thePlayer.inventoryContainer.getSlot(type + 4).getStack();
                    if (is != null) {
                        String typeStr = "";
                        switch (type) {
                            case 1:
                                typeStr = "helmet";
                                break;
                            case 2:
                                typeStr = "chestplate";
                                break;
                            case 3:
                                typeStr = "leggings";
                                break;
                            case 4:
                                typeStr = "boots";
                                break;
                        }
                        if (stack.getUnlocalizedName().contains(typeStr) && getProtScore(is) > getProtScore(stack)) {
                            continue;
                        }
                    }
                    if (isBestArmor(stack, type)) {
                        return false;
                    }
                }
            }
        }

        int weaponSlot = ItemType.WEAPON.getDesiredSlot(), pickaxeSlot = ItemType.PICKAXE.getDesiredSlot(),
                axeSlot = ItemType.AXE.getDesiredSlot(), shovelSlot = ItemType.SHOVEL.getDesiredSlot();

        if (stealing || (slot != weaponSlot || !isBestWeapon(ItemType.WEAPON.getStackInSlot()))
                && (slot != pickaxeSlot || !isBestPickaxe(ItemType.PICKAXE.getStackInSlot()))
                && (slot != axeSlot || !isBestAxe(ItemType.AXE.getStackInSlot()))
                && (slot != shovelSlot || !isBestShovel(ItemType.SHOVEL.getStackInSlot()))) {
            if (!stealing && item instanceof ItemArmor) {
                for (int type = 1; type < 5; type++) {
                    ItemStack is = mc.thePlayer.inventoryContainer.getSlot(type + 4).getStack();
                    if (is != null && isBestArmor(is, type)) {
                        continue;
                    }
                    if (isBestArmor(stack, type)) {
                        return false;
                    }
                }
            }

            if ((item == Items.wheat) || item == Items.spawn_egg
                    || (item instanceof ItemFood && dropFood.getValue() && !(item instanceof ItemAppleGold))
                    || (item instanceof ItemPotion && isBadPotion(stack))) {
                return true;
            } else if (!(item instanceof ItemSword) && !(item instanceof ItemTool) && !(item instanceof ItemHoe) && !(item instanceof ItemArmor)) {
                if (dropArchery.getValue() && (item instanceof ItemBow || item == Items.arrow)) {
                    return true;
                } else {
                    return (dropShears.getValue() && ulName.contains("shears")) || item instanceof ItemGlassBottle || Arrays.stream(blacklist).anyMatch(ulName::contains);
                }
            }
            return true;
        }

        return false;
    }

    private void getBestPickaxe() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestPickaxe(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.PICKAXE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestPickaxe(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                    }
                }
            }
        }
    }

    private void getBestAxe() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestAxe(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.AXE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestAxe(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        timer.reset();
                    }
                }
            }
        }
    }

    private void getBestShovel() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestShovel(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.SHOVEL.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestShovel(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        timer.reset();
                    }
                }
            }
        }
    }

    private void getBestBow() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                String stackName = is.getDisplayName().toLowerCase();
                if (Arrays.stream(serverItems).anyMatch(stackName::contains) || !(is.getItem() instanceof ItemBow))
                    continue;
                if (isBestBow(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.BOW.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestBow(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                    }
                }
            }
        }
    }

    private void moveArrows() {
        if (dropArchery.getValue() || !moveArrows.getValue() || !isReady()) return;
        for (int i = 36; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() == Items.arrow) {
                for (int j = 0; j < 36; j++) {
                    if (mc.thePlayer.inventoryContainer.getSlot(j).getStack() == null) {
                        fakeOpen();
                        InventoryUtils.click(i, 0, true);
                        fakeClose();
                        timer.reset();
                        break;
                    }
                }
            }
        }
    }

    private void moveFood() {
        if (dropFood.getValue() || !isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (hasMostGapples(is)) {
                    int desiredSlot = ItemType.GAPPLE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !hasMostGapples(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                    }
                }
            }
        }
    }

    private boolean hasMostGapples(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof ItemAppleGold)) {
            return false;
        } else {
            int value = stack.stackSize;
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemAppleGold && is.stackSize > value) {
                        return false;
                    }
                }
            }
            return true;
        }

    }

    private int getMostBlocks() {
        int stack = 0;
        int biggestSlot = -1;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            ItemStack is = slot.getStack();
            if (is != null && is.getItem() instanceof ItemBlock && is.stackSize > stack && Arrays.stream(serverItems).noneMatch(is.getDisplayName().toLowerCase()::contains)) {
                stack = is.stackSize;
                biggestSlot = i;
            }
        }
        return biggestSlot;
    }

    private void swapBlocks() {
        if (!swapBlocks.getValue() || !isReady()) return;
        int mostBlocksSlot = getMostBlocks();
        int desiredSlot = ItemType.BLOCK.getDesiredSlot();
        if (mostBlocksSlot != -1 && mostBlocksSlot != desiredSlot) {
            // only switch if the hotbar slot doesn't already have blocks of the same quantity to prevent an inf loop
            Slot dss = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
            ItemStack dsis = dss.getStack();
            if (!(dsis != null && dsis.getItem() instanceof ItemBlock && dsis.stackSize >= mc.thePlayer.inventoryContainer.getSlot(mostBlocksSlot).getStack().stackSize && Arrays.stream(serverItems).noneMatch(dsis.getDisplayName().toLowerCase()::contains))) {
                swap(mostBlocksSlot, desiredSlot - 36);
            }
        }
    }

    private boolean isBestPickaxe(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemPickaxe)) {
            return false;
        } else {
            float value = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemPickaxe && getToolScore(is) > value) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestShovel(ItemStack stack) {
        if (stack == null) return false;
        if (!(stack.getItem() instanceof ItemSpade)) {
            return false;
        } else {
            float score = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemSpade && getToolScore(is) > score) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestAxe(ItemStack stack) {
        if (stack == null) return false;
        if (!(stack.getItem() instanceof ItemAxe)) {
            return false;
        } else {
            float value = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (getToolScore(is) > value && is.getItem() instanceof ItemAxe && !isBestWeapon(is)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestBow(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBow)) {
            return false;
        } else {
            float value = getBowScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (getBowScore(is) > value && is.getItem() instanceof ItemBow && !isBestWeapon(stack)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private float getBowScore(ItemStack stack) {
        float score = 0;
        Item item = stack.getItem();
        if (item instanceof ItemBow) {
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 0.5F;
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.1F;
        }
        return score;
    }

    private float getToolScore(ItemStack stack) {
        float score = 0;
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            ItemTool tool = (ItemTool) item;
            String name = item.getUnlocalizedName().toLowerCase();
            if (item instanceof ItemPickaxe) {
                score = tool.getStrVsBlock(stack, Blocks.stone) - (name.contains("gold") ? 5 : 0);
            } else if (item instanceof ItemSpade) {
                score = tool.getStrVsBlock(stack, Blocks.dirt) - (name.contains("gold") ? 5 : 0);
            } else {
                if (!(item instanceof ItemAxe)) return 1;
                score = tool.getStrVsBlock(stack, Blocks.log) - (name.contains("gold") ? 5 : 0);
            }
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.0075F;
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 100F;
        }
        return score;
    }

    private boolean isBadPotion(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
            if (effects != null) {
                for (PotionEffect effect : effects) {
                    if (badPotionIDs.contains(effect.getPotionID())) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isBestArmor(ItemStack stack, int type) {
        String typeStr = "";
        switch (type) {
            case 1:
                typeStr = "helmet";
                break;
            case 2:
                typeStr = "chestplate";
                break;
            case 3:
                typeStr = "leggings";
                break;
            case 4:
                typeStr = "boots";
                break;
        }
        if (stack.getUnlocalizedName().contains(typeStr)) {
            float prot = getProtScore(stack);
            for (int i = 5; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getUnlocalizedName().contains(typeStr) && getProtScore(is) > prot) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean equipBest(int armorSlot) {
        if(isReady()) {
        int equipSlot = -1, currProt = -1;
        ItemArmor currItem = null;
        ItemStack slotStack = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();
        if (slotStack != null && slotStack.getItem() instanceof ItemArmor) {
            currItem = (ItemArmor) slotStack.getItem();
            currProt = currItem.damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(
                    Enchantment.protection.effectId, mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack());
        }
        // find best piece
        for (int i = 9; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() instanceof ItemArmor) {
                int prot = ((ItemArmor) is.getItem()).damageReduceAmount
                        + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, is);
                if ((currItem == null || currProt < prot) && isValidPiece(armorSlot, (ItemArmor) is.getItem())) {
                    currItem = (ItemArmor) is.getItem();
                    equipSlot = i;
                    currProt = prot;
                }
            }
        }
        // equip best piece (if there is a better one)
        if (equipSlot != -1) {
            if (slotStack != null) {
                InventoryUtils.drop(armorSlot);
                timer.reset();
            } else {
                InventoryUtils.click(equipSlot, 0, true);
                timer.reset();
            }
            return true;
        }
        }
        return false;
    }

    private boolean isValidPiece(int armorSlot, ItemArmor item) {
        String unlocalizedName = item.getUnlocalizedName();
        return armorSlot == 5 && unlocalizedName.startsWith("item.helmet")
                || armorSlot == 6 && unlocalizedName.startsWith("item.chestplate")
                || armorSlot == 7 && unlocalizedName.startsWith("item.leggings")
                || armorSlot == 8 && unlocalizedName.startsWith("item.boots");
    }

    private void fakeOpen() {
        if (!isInvOpen) {
            timer.reset();
            if (!inventoryOnly.getValue() && inventoryPackets.getValue())
                mc.getNetHandler().sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            isInvOpen = true;
        }
    }

    private void fakeClose() {
        if (isInvOpen) {
            if (!inventoryOnly.getValue() && inventoryPackets.getValue())
                mc.getNetHandler().sendPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            isInvOpen = false;
        }
    }

    private void swap(int slot, int hSlot) {
        fakeOpen();
        InventoryUtils.swap(slot, hSlot);
        fakeClose();
        timer.reset();
    }

    private boolean canContinue() {
        return (inventoryOnly.getValue() && !(mc.currentScreen instanceof GuiInventory)) || (onlyWhileNotMoving.getValue() && MovementUtils.isMoving());
    }

    private enum ItemType {
        WEAPON(slotWeapon),
        PICKAXE(slotPick),
        AXE(slotAxe),
        SHOVEL(slotShovel),
        BLOCK(slotBlock),
        BOW(slotBow),
        GAPPLE(slotGapple);

        private final DoubleProperty setting;

        ItemType(DoubleProperty name) {
            this.setting = name;
        }


        public int getDesiredSlot() {
            return setting.getValue().intValue() + 35;
        }

        public Slot getSlot() {
            return mc.thePlayer.inventoryContainer.getSlot(getDesiredSlot());
        }

        public ItemStack getStackInSlot() {
            return getSlot().getStack();
        }
    }
}
