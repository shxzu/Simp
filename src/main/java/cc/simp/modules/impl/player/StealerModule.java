package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.InventoryUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.*;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Stealer", category = ModuleCategory.PLAYER)
public final class StealerModule extends Module {

    private final Timer timer = new Timer();
    private final Timer startTimer = new Timer();
    private final NumberProperty startDelayProperty = new NumberProperty("Start Delay", 50.0, 0.0, 1000.0, 25.0);
    private final NumberProperty minDelayProperty = new NumberProperty("Min Delay", 5.0, 0.0, 1000.0, 25.0);
    private final NumberProperty maxDelayProperty = new NumberProperty("Max Delay", 5.0, 0.0, 1000.0, 25.0);
    private final Property<Boolean> stealTrashItemsProperty = new Property<>("Steal Trash Items", false);
    private final Property<Boolean> autoCloseProperty = new Property<>("Auto Close", true);
    private final Property<Boolean> chestNameProperty = new Property<>("Check Chest Name", false);
    private int decidedTimer = 0;
    private boolean gotItems;
    private int ticksInChest;
    private boolean lastInChest;

    @EventLink
    private final Listener<MotionEvent> motionEventListener = e -> {
        if (!e.isPre()) {
            return;
        }
        if (mc.thePlayer.ticksExisted <= 60) {
            return;
        }
        if (mc.currentScreen instanceof GuiChest && Display.isActive() && (!this.chestNameProperty.getValue() || ((GuiChest)mc.currentScreen).lowerChestInventory.getDisplayName().getUnformattedText().contains("chest"))) {
            mc.mouseHelper.mouseXYChange();
            mc.mouseHelper.ungrabMouseCursor();
            mc.mouseHelper.grabMouseCursor();
        }
        if (mc.currentScreen instanceof GuiChest) {
            ++this.ticksInChest;
            if (this.ticksInChest * 50 > 255) {
                this.ticksInChest = 10;
            }
        } else {
            --this.ticksInChest;
            this.gotItems = false;
            if (this.ticksInChest < 0) {
                this.ticksInChest = 0;
            }
        }
    };

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (mc.thePlayer.ticksExisted <= 60) {
            return;
        }
        if (!this.lastInChest) {
            this.startTimer.reset();
        }
        this.lastInChest = mc.currentScreen instanceof GuiChest;
        if (mc.currentScreen instanceof GuiChest) {
            String name;
            if (this.chestNameProperty.getValue() && !(name = ((GuiChest)mc.currentScreen).lowerChestInventory.getDisplayName().getUnformattedText()).toLowerCase().contains("chest")) {
                return;
            }
            if (!this.startTimer.hasTimeElapsed(this.startDelayProperty.getValue(), false)) {
                return;
            }
            if (this.decidedTimer == 0) {
                int delayFirst = (int)Math.floor(Math.min(this.minDelayProperty.getValue(), this.maxDelayProperty.getValue()));
                int delaySecond = (int)Math.ceil(Math.max(this.minDelayProperty.getValue(), this.maxDelayProperty.getValue()));
                this.decidedTimer = RandomUtils.nextInt(delayFirst, delaySecond);
            }
            if (this.timer.hasTimeElapsed(this.decidedTimer, false)) {
                ContainerChest chest = (ContainerChest)mc.thePlayer.openContainer;
                int i = 0;
                while (i < chest.inventorySlots.size()) {
                    ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
                    if (stack != null && this.itemWhitelisted(stack) && !this.stealTrashItemsProperty.getValue()) {
                        mc.playerController.windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                        this.timer.reset();
                        int delayFirst = (int)Math.floor(Math.min(this.minDelayProperty.getValue(), this.maxDelayProperty.getValue()));
                        int delaySecond = (int)Math.ceil(Math.max(this.minDelayProperty.getValue(), this.maxDelayProperty.getValue()));
                        this.decidedTimer = RandomUtils.nextInt(delayFirst, delaySecond);
                        this.gotItems = true;
                        return;
                    }
                    ++i;
                }
                if (this.gotItems && this.autoCloseProperty.getValue() && this.ticksInChest > 3) {
                    mc.thePlayer.closeScreen();
                }
            }
        }
    };

    private boolean itemWhitelisted(ItemStack itemStack) {
        if (InventoryUtils.isBadStackStealer(itemStack, true, true)) {
            return false;
        }
        ArrayList<Item> whitelistedItems = new ArrayList<Item>(){
            {
                this.add(Items.ender_pearl);
                this.add(Items.iron_ingot);
                this.add(Items.snowball);
                this.add(Items.gold_ingot);
                this.add(Items.redstone);
                this.add(Items.diamond);
                this.add(Items.emerald);
                this.add(Items.quartz);
                this.add(Items.bow);
                this.add(Items.arrow);
                this.add(Items.fishing_rod);
                this.add(Items.egg);
                this.add(Items.water_bucket);
                this.add(Items.lava_bucket);
            }
        };
        Item item = itemStack.getItem();
        String itemName = itemStack.getDisplayName();
        if (itemName.contains("Right Click") || itemName.contains("Click to Use") || itemName.contains("Players Finder")) {
            return true;
        }
        ArrayList<Integer> whitelistedPotions = new ArrayList<Integer>(){
            {
                this.add(6);
                this.add(1);
                this.add(5);
                this.add(8);
                this.add(14);
                this.add(12);
                this.add(10);
                this.add(16);
            }
        };
        if (item instanceof ItemPotion) {
            int potionID = this.getPotionId(itemStack);
            return whitelistedPotions.contains(potionID);
        }
        return item instanceof ItemBlock && !(((ItemBlock)item).getBlock() instanceof BlockTNT) && !(((ItemBlock)item).getBlock() instanceof BlockSlime) && !(((ItemBlock)item).getBlock() instanceof BlockFalling) || item instanceof ItemAnvilBlock || item instanceof ItemSword || item instanceof ItemArmor || item instanceof ItemTool || item instanceof ItemFood || item instanceof ItemSkull || itemName.contains("§") || whitelistedItems.contains(item) && !item.equals(Items.spider_eye);
    }

    private int getPotionId(ItemStack potion) {
        Item item = potion.getItem();
        try {
            if (item instanceof ItemPotion) {
                ItemPotion p = (ItemPotion)item;
                return p.getEffects(potion.getMetadata()).get(0).getPotionID();
            }
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
        return 0;
    }

}