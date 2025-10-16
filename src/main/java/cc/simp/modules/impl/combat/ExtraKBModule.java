package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Extra KB", category = ModuleCategory.COMBAT)
public class ExtraKBModule extends Module {

    public ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Legit);

    private enum Mode {
        Legit,
        Packet,
        Silent
    }

    @EventLink
    private final Listener<AttackEvent> attackEventListener = event -> {
        switch (modeProperty.getValue()) {
            case Legit:
                if (!mc.gameSettings.keyBindForward.isKeyDown() || mc.thePlayer.isSneaking()) {
                    return;
                }
                if (event.target.ticksExisted != 7) return;
                mc.gameSettings.keyBindForward.setPressed(false);
                mc.gameSettings.keyBindForward.setPressed(true);
            case Silent:
                if (!mc.gameSettings.keyBindForward.isKeyDown() || mc.thePlayer.isSneaking()) {
                    return;
                }
                if (event.target.ticksExisted != 7) return;
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.serverSprintState = true;
                    mc.thePlayer.setSprinting(true);
                }
                break;
            case Packet:
                if (!mc.gameSettings.keyBindForward.isKeyDown() || mc.thePlayer.isSneaking()) {
                    return;
                }
                if (event.target.ticksExisted != 7) return;
                PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                break;
        }
    };
}
