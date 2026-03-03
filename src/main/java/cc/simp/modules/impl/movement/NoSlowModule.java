package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.ItemSlowdownEvent;
import cc.simp.api.events.impl.player.TeleportEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.BlinkProcess;
import cc.simp.utils.mc.InventoryUtils;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Slow", category = ModuleCategory.MOVEMENT)
public final class NoSlowModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);
    private final NumberProperty amount = new NumberProperty("Amount", 2, () -> mode.getValue() == Mode.Ticks, 2, 5, 1);
    public final Property<Boolean> food = new Property<>("Food", true, () -> mode.getValue() != Mode.Blink && mode.getValue() != Mode.Hypixel);
    public final Property<Boolean> potion = new Property<>("Potion", true, () -> mode.getValue() != Mode.Blink && mode.getValue() != Mode.Hypixel);
    public final Property<Boolean> sword = new Property<>("Sword", true, () -> mode.getValue() != Mode.Blink && mode.getValue() != Mode.Hypixel);
    public final Property<Boolean> bow = new Property<>("Bow", true, () -> mode.getValue() != Mode.Blink && mode.getValue() != Mode.Hypixel);

    private enum Mode {
        Vanilla("Vanilla"),
        UpdatedNCP("Updated NCP"),
        Hypixel("Hypixel"),
        Blink("Blink"),
        Switch("Switch"),
        Ticks("Ticks");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static boolean isUsing;
    private int disable;
    public int tick;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        setSuffix(mode.getValue().toString());

        this.disable++;

        if (mode.getValue() == Mode.Blink) {
            if (mc.thePlayer.isUsingItem()) {
                if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFood) {
                    BlinkProcess.enable();
                    isUsing = true;
                }
            } else if (isUsing) {
                BlinkProcess.disable();
                isUsing = false;
            }
        }

        if (mc.thePlayer.isUsingItem()) {
            if (mode.getValue() == Mode.Hypixel && this.tick == 0 && InventoryUtils.isHoldingSword()) {
                PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                ++this.tick;
            }
        } else {
            this.tick = 0;
        }

    };

    @EventLink
    public final Listener<ItemSlowdownEvent> itemSlowdownEventListener = e -> {
        switch (mode.getValue()) {
            case Vanilla:
                if (food.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
                    e.setCancelled();
                }
                if (potion.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
                    e.setCancelled();
                }
                if (sword.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    e.setCancelled();
                }
                if (bow.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                    e.setCancelled();
                }
                break;
            case Hypixel:
                if (sword.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    e.setCancelled();
                }
                break;
            case Blink:
                if (mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
                    e.setCancelled();
                }
                break;
            case Ticks:
                if (mc.thePlayer.onGroundTicks % amount.getValue() != 0 && MovementUtils.isOnGround()) {
                    if (food.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
                        e.setCancelled();
                    }
                    if (potion.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
                        e.setCancelled();
                    }
                    if (sword.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                        e.setCancelled();
                    }
                    if (bow.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                        e.setCancelled();
                    }
                }
                break;
            case Switch:
                if (food.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
                    e.setCancelled();
                    if (!BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                }
                if (potion.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
                    e.setCancelled();
                    if (!BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                }
                if (sword.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    e.setCancelled();
                    if (!BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                }
                if (bow.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                    e.setCancelled();
                    if (!BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                }
                break;
            case UpdatedNCP:
                if (food.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
                    e.setCancelled();
                    if (this.disable > 10 && !BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    }
                }
                if (potion.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
                    e.setCancelled();
                    if (this.disable > 10 && !BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    }
                }
                if (sword.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    e.setCancelled();
                    if (this.disable > 10 && !BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    }
                }
                if (bow.getValue() && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                    e.setCancelled();
                    if (this.disable > 10 && !BadPacketsProcess.bad(false, true, true, false, false)) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    }
                }
                break;
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        isUsing = false;
        disable = 0;
    };

    @EventLink
    public final Listener<TeleportEvent> teleportEventListener = e -> {
        isUsing = false;
        disable = 0;
    };

    @Override
    public void onDisable() {
        isUsing = false;
        super.onDisable();
    }
}