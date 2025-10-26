package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.InventoryUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Manager", category = ModuleCategory.PLAYER)
public final class ManagerModule extends Module {
    public ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Open);
    public Property<Boolean> stopProperty = new Property<>("Stop", true, () -> this.modeProperty.getValue() == Mode.Spoof);
    public Property<Boolean> throwGarbageProperty = new Property<>("Throw Garbage", true);
    public NumberProperty startDelayProperty = new NumberProperty("Start Delay", 150.0, 0.0, 1000.0, 1.0);
    public NumberProperty speedProperty = new NumberProperty("Speed", 150.0, 0.0, 1000.0, 1.0);
    public Property<Boolean> swordProperty = new Property<>("Sword", true);
    public NumberProperty swordSlotProperty = new NumberProperty("Sword Slot", 1.0, swordProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> axeProperty = new Property<>("Axe", true);
    public NumberProperty axeSlotProperty = new NumberProperty("Axe Slot", 2.0, axeProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> pickaxeProperty = new Property<>("Pickaxe", true);
    public NumberProperty pickaxeSlotProperty = new NumberProperty("Pickaxe", 3.0, pickaxeProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> shovelProperty = new Property<>("Shovel", false);
    public NumberProperty shovelSlotProperty = new NumberProperty("Shovel Slot", 4.0, shovelProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> bowProperty = new Property<>("Bow", false);
    public NumberProperty bowSlotProperty = new NumberProperty("Bow Slot", 5.0, bowProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> blocksProperty = new Property<>("Blocks", true);
    public NumberProperty blockSlotProperty = new NumberProperty("Block Slot", 6.0, blocksProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> projectilesProperty = new Property<>("Projectiles", true);
    public NumberProperty projectileSlotProperty = new NumberProperty("Projectile Slot", 7.0, projectilesProperty::getValue, 1.0, 9.0, 1.0);
    public Property<Boolean> waterBucketProperty = new Property<>("Water Bucket", true);
    public NumberProperty waterBucketSloProperty = new NumberProperty("Water Bucket Slot", 8.0, waterBucketProperty::getValue, 1.0, 9.0, 1.0);

    public Timer startTimer = new Timer();
    public Timer timer = new Timer();

    public enum Mode {
        Open,
        Spoof
    }

    @Override
    public void onDisable() {
        InventoryUtils.closeInv(true);
        super.onDisable();
    }

    @EventLink
    private final Listener<MoveEvent> moveEventListener = event -> {
        this.setSuffix(modeProperty.getValue().toString());
        if (this.modeProperty.getValue() == Mode.Spoof) {
            if (mc.currentScreen == null) {
                this.startTimer.reset();
            }
            if (!this.startTimer.hasTimeElapsed(this.startDelayProperty.getValue(), false)) {
                return;
            }
        }
        KillAuraModule ka = (KillAuraModule) Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);
        EntityLivingBase target = ka.target;
        if (this.modeProperty.getValue() == Mode.Spoof && (mc.currentScreen != null || target != null)) {
            InventoryUtils.closeInv(true);
            return;
        }
        if (this.modeProperty.getValue() == Mode.Open && !(mc.currentScreen instanceof GuiInventory)) {
            return;
        }
        int i = 9;
        while (i < 45) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (InventoryUtils.timer.hasTimeElapsed(this.speedProperty.getValue(), false)) {
                    if (this.swordSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemSword && is == InventoryUtils.bestSword() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.bestSword()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.swordSlotProperty.getValue())).getStack() != is && this.swordProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.swordSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.bowSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemBow && is == InventoryUtils.bestBow() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.bestBow()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.bowSlotProperty.getValue())).getStack() != is && this.bowProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.bowSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.pickaxeSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemPickaxe && is == InventoryUtils.bestPick() && is != InventoryUtils.bestWeapon() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.bestPick()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.pickaxeSlotProperty.getValue())).getStack() != is && this.pickaxeProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.pickaxeSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.axeSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemAxe && is == InventoryUtils.bestAxe() && is != InventoryUtils.bestWeapon() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.bestAxe()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.axeSlotProperty.getValue())).getStack() != is && this.axeProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.axeSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.shovelSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemSpade && is == InventoryUtils.bestShovel() && is != InventoryUtils.bestWeapon() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.bestShovel()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.shovelSlotProperty.getValue())).getStack() != is && this.shovelProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.shovelSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.blockSlotProperty.getValue() != 0.0 && is.getItem() instanceof ItemBlock && is == InventoryUtils.getBlockSlotInventory() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.getBlockSlotInventory()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.blockSlotProperty.getValue())).getStack() != is && this.blocksProperty.getValue()) {
                        if (mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.blockSlotProperty.getValue())).getStack() != null && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.blockSlotProperty.getValue())).getStack().getItem() instanceof ItemBlock && !InventoryUtils.invalidBlocks.contains(((ItemBlock) mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.blockSlotProperty.getValue())).getStack().getItem()).getBlock())) {
                            return;
                        }
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.blockSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.projectileSlotProperty.getValue() != 0.0 && is == InventoryUtils.getProjectileSlotInventory() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.getProjectileSlotInventory()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.projectileSlotProperty.getValue())).getStack() != is && this.projectilesProperty.getValue()) {
                        if (mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.projectileSlotProperty.getValue())).getStack() != null && (mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.projectileSlotProperty.getValue())).getStack().getItem() instanceof ItemSnowball || mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.projectileSlotProperty.getValue())).getStack().getItem() instanceof ItemEgg || mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.projectileSlotProperty.getValue())).getStack().getItem() instanceof ItemFishingRod)) {
                            return;
                        }
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.projectileSlotProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (this.waterBucketSloProperty.getValue() != 0.0 && is.getItem() == Items.water_bucket && is == InventoryUtils.getBucketSlotInventory() && mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtils.getBucketSlotInventory()) && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.shovelSlotProperty.getValue())).getStack() != is && this.waterBucketProperty.getValue()) {
                        if (mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.waterBucketSloProperty.getValue())).getStack() != null && mc.thePlayer.inventoryContainer.getSlot((int) (35.0 + this.waterBucketSloProperty.getValue())).getStack().getItem() == Items.water_bucket) {
                            return;
                        }
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, (int) (this.waterBucketSloProperty.getValue() - 1.0), 2, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) {
                            break;
                        }
                    } else if (InventoryUtils.isBadStack(is, true, true) && this.throwGarbageProperty.getValue()) {
                        InventoryUtils.openInv(true);
                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, 1, 4, mc.thePlayer);
                        if (!this.stopProperty.getValue()) {
                            InventoryUtils.closeInv(true);
                        }
                        InventoryUtils.timer.reset();
                        if (this.speedProperty.getValue() != 0.0) break;
                    }
                    if (InventoryUtils.timer.hasTimeElapsed(55.0, false)) {
                        InventoryUtils.closeInv(true);
                    }
                }
            }
            ++i;
        }
        if (InventoryUtils.isInventoryOpen && this.stopProperty.getValue()) {
            event.setForward(0);
            event.setStrafe(0);
            event.setJump(false);
            event.setSneak(false);
        }
    };
}
