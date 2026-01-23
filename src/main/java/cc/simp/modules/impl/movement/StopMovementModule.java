package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.PostStrafeEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vector3d;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Stop Movement", category = ModuleCategory.MOVEMENT)
public final class StopMovementModule extends Module {
    private Vector3d motion;

    @Override
    public void onEnable() {
        motion = new Vector3d(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.motionX = motion.x;
        mc.thePlayer.motionY = motion.y;
        mc.thePlayer.motionZ = motion.z;
        super.onDisable();
    }

    @EventLink
    public final Listener<PostStrafeEvent> onPostStrafe = event -> {
        MovementUtils.stop();
        mc.thePlayer.motionY = 0;
    };

    @EventLink
    public final Listener<PacketSendEvent> onPacketSend = event -> {
        Packet<?> packet = event.getPacket();

        if (packet instanceof C03PacketPlayer) {
            event.setCancelled();
        }
    };
}
