package cc.simp.modules.impl.movement;

import cc.simp.event.impl.player.ItemSlowdownEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.render.BlockAnimationsModule;
import cc.simp.property.Property;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "No Slowdown", category = ModuleCategory.MOVEMENT)
public final class NoSlowdownModule extends Module {

    public static EnumProperty<Mode> noSlowModeProperty = new EnumProperty<>("Style", Mode.VANILLA);

    public static enum Mode {
        VANILLA,
        INTAVE
    }

    public NoSlowdownModule() {
        setSuffixListener(noSlowModeProperty);
    }

    public static boolean canNoSlow;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        switch (noSlowModeProperty.getValue()) {
            case INTAVE:
                if (MovementUtils.isMoving() && mc.thePlayer.isUsingItem() && mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemFood) {
                    final BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    mc.getNetHandler().sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, pos, EnumFacing.UP));
                }
                break;
        }
    };

    @EventLink
    public final Listener<ItemSlowdownEvent> itemSlowdownEventListener = event -> {
        if (noSlowModeProperty.getValue() == Mode.INTAVE && mc.thePlayer.isUsingItem() && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFood) {
            canNoSlow = true;
            event.setCancelled();
        } else canNoSlow = false;
        if (noSlowModeProperty.getValue() == Mode.VANILLA && mc.thePlayer.isUsingItem() && !(mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBow)) {
            canNoSlow = true;
            event.setCancelled();
        } else canNoSlow = false;
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        canNoSlow = false;
    };

    @Override
    public void onDisable() {
        canNoSlow = false;
        super.onDisable();
    }

}
