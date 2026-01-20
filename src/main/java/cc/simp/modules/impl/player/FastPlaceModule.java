package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBlock;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Fast Place", category = ModuleCategory.PLAYER)
public final class FastPlaceModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 1, 0, 3, 1);

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if (mc.thePlayer == null || mc.thePlayer.inventory == null || mc.thePlayer.inventory.getCurrentItem() == null || !event.isPre()) {
            return;
        }

        if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
            mc.rightClickDelayTimer = Math.min(mc.rightClickDelayTimer, this.delay.getValue().intValue());
        }
    };
}
