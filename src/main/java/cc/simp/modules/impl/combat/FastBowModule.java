package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemBow;
import net.minecraft.network.play.client.C03PacketPlayer;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Fast Bow", category = ModuleCategory.COMBAT)
public final class FastBowModule extends Module {

    public static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Vanilla);

    public enum Mode {
        Vanilla,
        NCP
    }

    @EventLink
    public final Listener<TickEvent> tickEventListener = event -> {
        setSuffix(String.valueOf(mode.getValue()));
        if (mc.thePlayer.getItemInUse() == null || mc.thePlayer.inventory.getCurrentItem() == null) return;
        if (mc.thePlayer.getItemInUseDuration() >= 15 || mode.getValue() == Mode.Vanilla) {
            if (mc.thePlayer.getItemInUse().getItem() instanceof ItemBow) {
                for (int i = 0; i < (mode.getValue() == Mode.Vanilla ? 20 : 8); ++i)
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                mc.playerController.onStoppedUsingItem(mc.thePlayer);
            }
        }
    };

}
