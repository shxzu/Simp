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
import net.minecraft.item.ItemStack;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Armor", category = ModuleCategory.PLAYER)
public final class AutoArmorModule extends Module {
    public Timer startTimer = new Timer();
    public Timer timer = new Timer();
    public ModeProperty modeProperty = new ModeProperty<>("Mode", Mode.Open);
    public Property<Boolean> stop = new Property<>("Stop", true, () -> this.modeProperty.getValue() == Mode.Spoof);
    public NumberProperty startDelay = new NumberProperty("Start Delay", 150.0, 0.0, 1000.0, 1.0);
    public NumberProperty speed = new NumberProperty("Speed", 150.0, 0.0, 1000.0, 1.0);

    private enum Mode {
        Open,
        Spoof
    }

    @EventLink
    private final Listener<MoveEvent> moveEventListener = event -> {
        KillAuraModule ka = (KillAuraModule) Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);
        EntityLivingBase target = ka.target;

        if (this.modeProperty.getValue() != Mode.Spoof || mc.currentScreen == null && target == null) {
            if (this.modeProperty.getValue() == Mode.Open) {
                if (mc.currentScreen == null) {
                    this.startTimer.reset();
                }

                if (!this.startTimer.hasTimeElapsed(this.startDelay.getValue(), false)) {
                    return;
                }
            }

            if (InventoryUtils.timer.hasTimeElapsed(this.speed.getValue(), false)) {
                if (this.modeProperty.getValue() == Mode.Open && !(mc.currentScreen instanceof GuiInventory)) {
                    return;
                }

                for (int type = 1; type < 5; type++) {
                    if (mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack()) {
                        ItemStack is = mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack();
                        if (!InventoryUtils.isBestArmor(is, type)) {
                            InventoryUtils.openInv(true);
                            InventoryUtils.drop(4 + type);
                            if (!this.stop.getValue()) {
                                InventoryUtils.closeInv(true);
                            }

                            InventoryUtils.timer.reset();
                            if (this.speed.getValue() != 0.0) {
                                break;
                            }
                        }
                    }
                }

                for (int typex = 1; typex < 5; typex++) {
                    if ((double) InventoryUtils.timer.getTime() > this.speed.getValue()) {
                        for (int i = 9; i < 45; i++) {
                            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                                if (InventoryUtils.getProtection(is) > 0.0F && InventoryUtils.isBestArmor(is, typex) && !InventoryUtils.isBadStack(is, true, true)) {
                                    InventoryUtils.openInv(true);
                                    InventoryUtils.shiftClick(i);
                                    if (!this.stop.getValue()) {
                                        InventoryUtils.closeInv(true);
                                    }

                                    InventoryUtils.timer.reset();
                                    if (this.speed.getValue() != 0.0) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (InventoryUtils.timer.hasTimeElapsed(55.0, false)) {
                InventoryUtils.closeInv(true);
            }

            if (InventoryUtils.isInventoryOpen && this.stop.getValue()) {
                event.setForward(0);
                event.setStrafe(0);
                event.setJump(false);
                event.setSneak(false);
            }
        } else {
            InventoryUtils.closeInv(true);
        }
    };
}