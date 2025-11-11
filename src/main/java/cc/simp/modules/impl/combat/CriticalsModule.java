package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Criticals", category = ModuleCategory.COMBAT)
public final class CriticalsModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Edit);

    private enum Mode {
        Visual,
        Edit,
        NCP
    }

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (mode.getValue() == Mode.Edit) {
            if (e.getPacket() instanceof C03PacketPlayer packet) {
                packet.onGround = false;
            }
        }
    };

    @EventLink
    public final Listener<AttackEvent> attackEventListener = e -> {
        if (mode.getValue() == Mode.NCP) {
            boolean willCritLegit = mc.thePlayer.fallDistance > 0.0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding();

            if (willCritLegit) {
                return;
            }

            PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.000000271875, mc.thePlayer.posZ, false));
            PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0., mc.thePlayer.posZ, false));
        }

        if(mode.getValue() == Mode.Visual) {
            mc.thePlayer.onCriticalHit(e.target);
        }
    };
}
