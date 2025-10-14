package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.player.ItemSlowdownEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Slow", category = ModuleCategory.MOVEMENT)
public final class NoSlowModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);
    private final NumberProperty amount = new NumberProperty("Amount", 2, () -> mode.getValue() == Mode.Prediction, 2, 5, 1);
    public final Property<Boolean> food = new Property<>("Food", true);
    public final Property<Boolean> potion = new Property<>("Potion", true);
    public final Property<Boolean> sword = new Property<>("Sword", true);
    public final Property<Boolean> bow = new Property<>("Bow", true);

    private enum Mode {
        Vanilla("Vanilla"),
        Prediction("Prediction");

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

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (!e.isPre()) return;

        switch (mode.getValue()) {
            // TODO: Implement other modes.
        }
    };

    @EventLink
    public final Listener<ItemSlowdownEvent> itemSlowdownEventListener = e -> {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem() || mc.thePlayer.inventory.getCurrentItem() == null) {
            isUsing = false;
            return;
        }

        switch (mode.getValue()) {
            case Vanilla:
                isUsing = true;
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
            case Prediction:
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
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        isUsing = false;
    };

    @Override
    public void onDisable() {
        isUsing = false;
        super.onDisable();
    }
}