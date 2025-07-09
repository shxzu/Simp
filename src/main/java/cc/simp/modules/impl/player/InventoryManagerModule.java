package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Inventory Manager", category = ModuleCategory.PLAYER)
public final class InventoryManagerModule extends Module {

    public DoubleProperty delayProperty = new DoubleProperty("Delay", 50, 0, 500, 10);
    public Property<Boolean> equipBestArmorProperty = new Property<>("Equip Best Armor", true);
    public Property<Boolean> equipBestSwordProperty = new Property<>("Equip Best Sword", true);
    public Property<Boolean> equipBestPickaxeProperty = new Property<>("Equip Best Pickaxe", true);
    public Property<Boolean> equipBestAxeProperty = new Property<>("Equip Best Axe", true);
    public Property<Boolean> equipBestShovelProperty = new Property<>("Equip Best Shovel", true);
    public Property<Boolean> dropUselessItemsProperty = new Property<>("Drop Useless Items", false);

    private final Timer delayTimer = new Timer();
    private boolean isManaging = false;
    private List<Runnable> managementActions = new ArrayList<>(); // List of actions to perform

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPost()) { // Run after player movement/rotations
            if (!(mc.currentScreen instanceof GuiInventory)) {
                resetState();
                return;
            }

            // Removed the redundant autoManageProperty check here

            if (!isManaging) {
                isManaging = true;
                collectManagementActions();
            }

            // Perform management actions with delay
            if (!managementActions.isEmpty() && delayTimer.hasTimeElapsed(delayProperty.getValue().intValue() + MathUtils.getRandomNumberUsingNextInt(1, 10))) {
                Runnable action = managementActions.remove(0);
                action.run();
                delayTimer.reset();
            }
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        isManaging = false;
        managementActions.clear();
        delayTimer.reset();
    }

    private void collectManagementActions() {
        managementActions.clear();
        ContainerPlayer playerContainer = (ContainerPlayer) mc.thePlayer.inventoryContainer;

        // --- Armor Management ---
        if (equipBestArmorProperty.getValue()) {
            // Armor slots: 5 (helmet), 6 (chest), 7 (legs), 8 (boots)
            // Inventory slots: 9-35 (main inventory), 36-44 (hotbar)

            // Find best armor pieces in inventory
            ItemStack bestHelmet = getBestArmorPiece(0);
            ItemStack bestChestplate = getBestArmorPiece(1);
            ItemStack bestLeggings = getBestArmorPiece(2);
            ItemStack bestBoots = getBestArmorPiece(3);

            // Equip if better
            addEquipAction(bestHelmet, 5); // Helmet slot
            addEquipAction(bestChestplate, 6); // Chestplate slot
            addEquipAction(bestLeggings, 7); // Leggings slot
            addEquipAction(bestBoots, 8); // Boots slot
        }

        // --- Weapon Management ---
        if (equipBestSwordProperty.getValue()) {
            ItemStack bestSword = getBestWeapon(ItemSword.class);
            if (bestSword != null && !isCurrentHotbarItem(bestSword)) {
                int bestSwordSlot = findItemSlot(bestSword);
                if (bestSwordSlot != -1) {
                    // Move to hotbar slot 0 (first slot)
                    managementActions.add(() -> clickSlot(bestSwordSlot, 0, 2)); // Pick up
                    managementActions.add(() -> clickSlot(36, 0, 2)); // Put down in hotbar slot 0
                }
            }
        }

        if (equipBestPickaxeProperty.getValue()) {
            ItemStack bestPickaxe = getBestWeapon(ItemPickaxe.class);
            if (bestPickaxe != null && !isCurrentHotbarItem(bestPickaxe)) {
                int bestPickaxeSlot = findItemSlot(bestPickaxe);
                if (bestPickaxeSlot != -1) {
                    managementActions.add(() -> clickSlot(bestPickaxeSlot, 0, 2));
                    managementActions.add(() -> clickSlot(37, 0, 2)); // Example: hotbar slot 1
                }
            }
        }

        if (equipBestAxeProperty.getValue()) {
            ItemStack bestAxe = getBestWeapon(ItemAxe.class);
            if (bestAxe != null && !isCurrentHotbarItem(bestAxe)) {
                int bestAxeSlot = findItemSlot(bestAxe);
                if (bestAxeSlot != -1) {
                    managementActions.add(() -> clickSlot(bestAxeSlot, 0, 2));
                    managementActions.add(() -> clickSlot(38, 0, 2)); // Example: hotbar slot 2
                }
            }
        }

        if (equipBestShovelProperty.getValue()) {
            ItemStack bestShovel = getBestWeapon(ItemSpade.class);
            if (bestShovel != null && !isCurrentHotbarItem(bestShovel)) {
                int bestShovelSlot = findItemSlot(bestShovel);
                if (bestShovelSlot != -1) {
                    managementActions.add(() -> clickSlot(bestShovelSlot, 0, 2));
                    managementActions.add(() -> clickSlot(39, 0, 2)); // Example: hotbar slot 3
                }
            }
        }

        // --- Drop Useless Items ---
        if (dropUselessItemsProperty.getValue()) {
            // This is a complex feature that requires defining "useless".
            // For simplicity, I'll just add a placeholder.
            // A full implementation would involve checking if an item is:
            // 1. Not a "best" item (armor, sword, tool)
            // 2. Not food, potions, blocks, etc. that the user wants to keep
            // 3. Not currently equipped or in hotbar (unless being replaced)
            // 4. Not a stack that can be combined
            // This would require a comprehensive "isUseful" method.
            // For now, this will be a placeholder.
            // Example: Drop all cobblestone if you have too much (very simplified)
            for (int i = 9; i < 45; i++) { // Main inventory and hotbar
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack() && slot.getStack().getItem() instanceof ItemBlock && slot.getStack().stackSize > 64) {
                    // managementActions.add(() -> clickSlot(i, 1, 4)); // Drop stack
                }
            }
        }
    }

    /**
     * Adds an action to equip an armor piece if it's better than the currently equipped one.
     * @param bestPiece The best armor piece found in inventory.
     * @param armorSlot The corresponding armor slot index (5-8).
     */
    private void addEquipAction(ItemStack bestPiece, int armorSlot) {
        if (bestPiece == null) return;

        ItemStack equippedPiece = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();
        if (equippedPiece == null || getItemProtectionValue(bestPiece) > getItemProtectionValue(equippedPiece)) {
            int bestPieceSlot = findItemSlot(bestPiece);
            if (bestPieceSlot != -1) {
                managementActions.add(() -> clickSlot(bestPieceSlot, 0, 1)); // Shift-click to equip
            }
        }
    }

    /**
     * Finds the best armor piece of a specific type (helmet, chest, legs, boots).
     * @param armorType 0=helmet, 1=chest, 2=legs, 3=boots
     * @return The best ItemStack found, or null if none.
     */
    private ItemStack getBestArmorPiece(int armorType) {
        ItemStack bestPiece = null;
        double bestValue = -1;

        // Iterate through main inventory and hotbar (slots 9-44)
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack() && slot.getStack().getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) slot.getStack().getItem();
                if (armor.armorType == armorType) {
                    double currentValue = getItemProtectionValue(slot.getStack());
                    if (currentValue > bestValue) {
                        bestValue = currentValue;
                        bestPiece = slot.getStack();
                    }
                }
            }
        }
        return bestPiece;
    }

    /**
     * Finds the best weapon/tool of a given class in the inventory.
     * @param itemClass The class of the item (e.g., ItemSword.class).
     * @return The best ItemStack found, or null.
     */
    private <T extends Item> ItemStack getBestWeapon(Class<T> itemClass) {
        ItemStack bestWeapon = null;
        double bestValue = -1;

        for (int i = 9; i < 45; i++) { // Main inventory and hotbar
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack() && itemClass.isInstance(slot.getStack().getItem())) {
                double currentValue = getItemAttackValue(slot.getStack());
                if (currentValue > bestValue) {
                    bestValue = currentValue;
                    bestWeapon = slot.getStack();
                }
            }
        }
        return bestWeapon;
    }

    /**
     * Gets the protection value of an armor piece, considering enchantments.
     */
    private double getItemProtectionValue(ItemStack armorStack) {
        if (armorStack == null || !(armorStack.getItem() instanceof ItemArmor)) return 0;
        ItemArmor armor = (ItemArmor) armorStack.getItem();
        double value = armor.damageReduceAmount;
        value += EnchantmentHelper.getEnchantmentLevel(0, armorStack) * 1.25; // Protection enchantment
        value += EnchantmentHelper.getEnchantmentLevel(1, armorStack) * 1.0; // Fire Protection etc.
        value += (armorStack.getMaxDamage() - armorStack.getItemDamage()) / (double) armorStack.getMaxDamage() * 0.1; // Durability factor
        return value;
    }

    /**
     * Gets the attack value of a weapon/tool, considering enchantments.
     */
    private double getItemAttackValue(ItemStack weaponStack) {
        if (weaponStack == null || !(weaponStack.getItem() instanceof ItemTool || weaponStack.getItem() instanceof ItemSword)) return 0;
        double value = 0;
        if (weaponStack.getItem() instanceof ItemSword) {
            value += ((ItemSword) weaponStack.getItem()).getDamageVsEntity();
            value += EnchantmentHelper.getEnchantmentLevel(16, weaponStack) * 1.25; // Sharpness
            value += EnchantmentHelper.getEnchantmentLevel(20, weaponStack) * 0.5; // Fire Aspect
        } else if (weaponStack.getItem() instanceof ItemTool) {
            // For tools, prioritize efficiency or base damage if applicable
            value += EnchantmentHelper.getEnchantmentLevel(32, weaponStack) * 1.0; // Efficiency
        }
        value += (weaponStack.getMaxDamage() - weaponStack.getItemDamage()) / (double) weaponStack.getMaxDamage() * 0.1; // Durability factor
        return value;
    }

    /**
     * Checks if an item is currently in the player's hotbar.
     */
    private boolean isCurrentHotbarItem(ItemStack stack) {
        if (stack == null) return false;
        for (int i = 36; i < 45; i++) { // Hotbar slots
            Slot hotbarSlot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (hotbarSlot.getHasStack() && hotbarSlot.getStack() == stack) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the inventory slot index of a given ItemStack.
     */
    private int findItemSlot(ItemStack stack) {
        if (stack == null) return -1;
        for (int i = 0; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack() && slot.getStack() == stack) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Helper to perform a window click action.
     * @param slotId The slot to click.
     * @param mouseButton The mouse button (0=left, 1=right).
     * @param clickType The click type (1=shift-click, 2=swap, 0=normal, 4=drop).
     */
    private void clickSlot(int slotId, int mouseButton, int clickType) {
        mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                slotId,
                mouseButton,
                clickType,
                mc.thePlayer
        );
    }
}
