package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.ItemSlowdownEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "NoSlow", category = ModuleCategory.COMBAT)
public final class NoSlowModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    private final Property<Boolean> foods = new Property<>("Foods", true);
    private final Property<Boolean> bows = new Property<>("Bows", false);

    private enum Mode {
        VANILLA("Vanilla"),
        INTAVE("Intave");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static boolean canNoSlow;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (!e.isPre()) return;

        switch (mode.getValue()) {
            case INTAVE:
                if (MovementUtils.isMoving() && mc.thePlayer.isUsingItem() &&
                        mc.thePlayer.getCurrentEquippedItem() != null &&
                        mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemFood &&
                        foods.getValue()) {
                    final BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    mc.getNetHandler().sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, pos, EnumFacing.UP));
                }
                break;
        }
    };

    @EventLink
    public final Listener<ItemSlowdownEvent> itemSlowdownEventListener = e -> {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem() || mc.thePlayer.inventory.getCurrentItem() == null) {
            canNoSlow = false;
            return;
        }

        switch (mode.getValue()) {
            case INTAVE:
                if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFood && foods.getValue()) {
                    canNoSlow = true;
                    e.setCancelled(true);
                } else {
                    canNoSlow = false;
                }
                break;

            case VANILLA:
                boolean isBow = mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBow;
                if ((!isBow || bows.getValue()) && (!isBow || foods.getValue() || !(mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFood))) {
                    canNoSlow = true;
                    e.setCancelled(true);
                } else {
                    canNoSlow = false;
                }
                break;
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        canNoSlow = false;
    };

    @Override
    public void onDisable() {
        canNoSlow = false;
        super.onDisable();
    }
}