package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C03PacketPlayer;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Fast Use", category = ModuleCategory.PLAYER)
public final class FastUseModule extends Module {

    public final NumberProperty speed = new NumberProperty("Speed", 15, 1, 100, 1);

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if(!event.isPre()) return;
        setSuffix(String.valueOf(speed.getValue().intValue()));
        if (mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUseCount() == 31) {
            for (int i = 0; i <= speed.getValue().intValue(); i++) {
                PacketUtils.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            }
        }
    };

}
