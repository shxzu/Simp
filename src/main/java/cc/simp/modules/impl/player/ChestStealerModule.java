package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Chest Stealer", category = ModuleCategory.PLAYER)
public final class ChestStealerModule extends Module {

    public DoubleProperty delayProperty = new DoubleProperty("Delay", 50, 0, 500, 10);
    public Property<Boolean> hotbarOnlyProperty = new Property<>("Hotbar Only", false);
    public Property<Boolean> closeChestProperty = new Property<>("Close Chest", true);
    public EnumProperty<StealMode> stealModeProperty = new EnumProperty<>("Steal Mode", StealMode.SMART); // Renamed default

    // Customizable Item Selections (visible only in CUSTOM mode)
    public Property<Boolean> stealGapplesProperty = new Property<>("Gapples", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealSwordsProperty = new Property<>("Swords", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealPickaxesProperty = new Property<>("Pickaxes", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealAxesProperty = new Property<>("Axes", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealShovelsProperty = new Property<>("Shovels", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealBowsProperty = new Property<>("Bows", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealRodsProperty = new Property<>("Rods", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealFoodProperty = new Property<>("Food", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealBlocksProperty = new Property<>("Blocks", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealPotionsProperty = new Property<>("Potions", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealArmorProperty = new Property<>("Armor", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);
    public Property<Boolean> stealToolsProperty = new Property<>("Tools (Other)", true, () -> stealModeProperty.getValue() == StealMode.CUSTOM);


    private enum StealMode {
        ALL,
        SMART, // Renamed from BEST_ITEMS
        CUSTOM
    }

    private static final int SMART_ITEM_VALUE_THRESHOLD = 50; // Minimum value for an item to be considered "smart" to steal

    private final Timer delayTimer = new Timer();
    private boolean isStealing = false;
    private List<Integer> slotsToSteal = new ArrayList<>();

    public ChestStealerModule() {
        setSuffixListener(stealModeProperty);
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPost()) { // Run after player movement/rotations
            if (!(mc.currentScreen instanceof GuiChest)) {
                resetState();
                return;
            }

            if (!isStealing) {
                // Initialize stealing process
                isStealing = true;
                collectItemsToSteal();
            }

            // Perform stealing actions with delay
            if (!slotsToSteal.isEmpty() && delayTimer.hasTimeElapsed(delayProperty.getValue().intValue() + MathUtils.getRandomNumberUsingNextInt(1, 10))) {
                int slotIndex = slotsToSteal.remove(0); // Get the next slot to steal
                mc.playerController.windowClick(
                        mc.thePlayer.openContainer.windowId,
                        slotIndex,
                        0, // Left click
                        1, // Shift-click (quick move)
                        mc.thePlayer
                );
                delayTimer.reset();
            } else if (slotsToSteal.isEmpty() && closeChestProperty.getValue()) {
                // All items stolen, close chest
                mc.thePlayer.closeScreen();
                resetState();
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
        isStealing = false;
        slotsToSteal.clear();
        delayTimer.reset();
    }

    private void collectItemsToSteal() {
        slotsToSteal.clear();
        ContainerChest chestContainer = (ContainerChest) mc.thePlayer.openContainer;
        int chestRows = chestContainer.getLowerChestInventory().getSizeInventory() / 9;
        int chestSlots = chestRows * 9;

        List<Slot> potentialItemSlots = new ArrayList<>();
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = chestContainer.getSlot(i);
            if (slot.getHasStack()) {
                potentialItemSlots.add(slot);
            }
        }

        if (stealModeProperty.getValue() == StealMode.ALL) {
            // Add all items from chest to the list
            for (Slot slot : potentialItemSlots) {
                slotsToSteal.add(slot.slotNumber);
            }
        } else if (stealModeProperty.getValue() == StealMode.SMART) { // Changed to SMART
            List<Slot> smartItemSlots = new ArrayList<>();
            for (Slot slot : potentialItemSlots) {
                // Filter items based on a minimum value to consider them "smart" to steal
                if (getItemValue(slot.getStack()) >= SMART_ITEM_VALUE_THRESHOLD) {
                    smartItemSlots.add(slot);
                }
            }
            // Sort the filtered "smart" items by their value
            smartItemSlots.sort((s1, s2) -> getItemValue(s2.getStack()) - getItemValue(s1.getStack()));

            for (Slot slot : smartItemSlots) {
                slotsToSteal.add(slot.slotNumber);
            }
        } else if (stealModeProperty.getValue() == StealMode.CUSTOM) {
            for (Slot slot : potentialItemSlots) {
                if (shouldStealCustom(slot.getStack())) {
                    slotsToSteal.add(slot.slotNumber);
                }
            }
        }

        // If hotbarOnly is enabled, filter out items that can't go to hotbar
        if (hotbarOnlyProperty.getValue()) {
            List<Integer> filteredSlots = new ArrayList<>();
            for (int slotIndex : slotsToSteal) {
                // Check if there's space in hotbar (slots 36-44)
                boolean hasHotbarSpace = false;
                for (int i = 36; i < 45; i++) { // Hotbar slots
                    if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                        hasHotbarSpace = true;
                        break;
                    }
                }
                // If the item can stack and there's an existing stack in hotbar
                ItemStack chestStack = chestContainer.getSlot(slotIndex).getStack();
                if (chestStack != null) {
                    for (int i = 36; i < 45; i++) {
                        ItemStack hotbarStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                        if (hotbarStack != null && hotbarStack.isItemEqual(chestStack) && hotbarStack.stackSize < hotbarStack.getMaxStackSize()) {
                            hasHotbarSpace = true; // Can stack into existing hotbar slot
                            break;
                        }
                    }
                }

                if (hasHotbarSpace) {
                    filteredSlots.add(slotIndex);
                }
            }
            slotsToSteal = filteredSlots;
        }
    }

    /**
     * Assigns a simple heuristic value to an item for comparison and filtering.
     * This is a very basic implementation and can be expanded.
     */
    private int getItemValue(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;

        Item item = stack.getItem();
        int value = 0;

        // Gapples are high priority
        if (item == Items.golden_apple) {
            value += 1000;
        }

        // Swords: Higher attack damage + enchantments
        if (item instanceof ItemSword) {
            value += 500 + ((ItemSword) item).getDamageVsEntity();
            value += EnchantmentHelper.getEnchantmentLevel(16, stack) * 50; // Sharpness
            value += EnchantmentHelper.getEnchantmentLevel(20, stack) * 20; // Fire Aspect
        }
        // Armor: Higher protection + enchantments
        else if (item instanceof ItemArmor) {
            value += 400 + ((ItemArmor) item).damageReduceAmount;
            value += EnchantmentHelper.getEnchantmentLevel(0, stack) * 50; // Protection
        }
        // Tools: Higher efficiency + durability
        else if (item instanceof ItemTool) {
            value += 300;
            value += EnchantmentHelper.getEnchantmentLevel(32, stack) * 30; // Efficiency
            value += stack.getMaxDamage() - stack.getItemDamage(); // Durability
        }
        // Food: Higher food value
        else if (item instanceof ItemFood) {
            value += 100 + ((ItemFood) item).getHealAmount(stack);
        }
        // Potions: Always good
        else if (item instanceof ItemPotion) {
            value += 150;
        }
        // Blocks: Stack size is important
        else if (item instanceof ItemBlock) {
            value += 50 + stack.stackSize;
        }
        // Rods/Bows
        else if (item instanceof ItemFishingRod || item instanceof ItemBow) {
            value += 100;
        }

        // Add value for stack size for general items
        value += stack.stackSize;

        return value;
    }

    /**
     * Determines if an item should be stolen based on custom property settings.
     */
    private boolean shouldStealCustom(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;

        Item item = stack.getItem();

        if (stealGapplesProperty.getValue() && item == Items.golden_apple) return true;
        if (stealSwordsProperty.getValue() && item instanceof ItemSword) return true;
        if (stealPickaxesProperty.getValue() && item instanceof ItemPickaxe) return true;
        if (stealAxesProperty.getValue() && item instanceof ItemAxe) return true;
        if (stealShovelsProperty.getValue() && item instanceof ItemSpade) return true;
        if (stealBowsProperty.getValue() && item instanceof ItemBow) return true;
        if (stealRodsProperty.getValue() && item instanceof ItemFishingRod) return true;
        if (stealFoodProperty.getValue() && item instanceof ItemFood) return true;
        if (stealPotionsProperty.getValue() && item instanceof ItemPotion) return true;
        if (stealArmorProperty.getValue() && item instanceof ItemArmor) return true;
        if (stealBlocksProperty.getValue() && item instanceof ItemBlock) return true;

        // Generic tools (flint and steel, shears, etc.)
        if (stealToolsProperty.getValue() && item instanceof ItemTool && !(item instanceof ItemPickaxe || item instanceof ItemAxe || item instanceof ItemSpade)) return true;

        return false;
    }
}
