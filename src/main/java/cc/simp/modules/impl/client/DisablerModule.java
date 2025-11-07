package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Disabler", category = ModuleCategory.CLIENT)
public final class DisablerModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VerusCombat);

    private enum Mode {
        VerusCombat("Verus Combat"),
        VulcanReach("Vulcan Reach");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean transaction;

    @EventLink
    private final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (mode.getValue() == Mode.VerusCombat) {
            if (event.getPacket() instanceof S32PacketConfirmTransaction) {
                event.setCancelled(true);
                PacketUtils.sendSilentPacket(new C0FPacketConfirmTransaction((transaction ? 1 : -1), (short) (transaction ? 1 : -1), transaction));
                transaction = !transaction;
            }
        }
    };

    @EventLink
    private final Listener<PacketSendEvent> packetSendEventListener = e -> {
        if (mode.getValue() == Mode.VulcanReach) {
            if (e.getPacket() instanceof C02PacketUseEntity useEntity) {
                Entity victim = mc.theWorld.getEntityByID(useEntity.entityId);
                if (victim == null) return;

                if (useEntity.getAction().equals(C02PacketUseEntity.Action.ATTACK)) {
                    double playerX = mc.thePlayer.posX;
                    double playerY = mc.thePlayer.posY;
                    double playerZ = mc.thePlayer.posZ;

                    double targetX = victim.posX;
                    double targetY = victim.posY;
                    double targetZ = victim.posZ;

                    double directionX = targetX - playerX;
                    double directionY = targetY - playerY;
                    double directionZ = targetZ - playerZ;

                    double distance = Math.sqrt(
                            (targetX - playerX) * (targetX - playerX) +
                                    (targetY - playerY) * (targetY - playerY) +
                                    (targetZ - playerZ) * (targetZ - playerZ)
                    );

                    if (distance < 3.0) return;

                    double length = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
                    double normalizedX = directionX / length;
                    double normalizedZ = directionZ / length;

                    double moveDistance = Math.min(0.21, length);

                    double newX = playerX + normalizedX * moveDistance;
                    double newZ = playerZ + normalizedZ * moveDistance;

                    mc.thePlayer.motionX = 0.08f;

                    mc.thePlayer.setPosition(newX, mc.thePlayer.posY, newZ);
                }
            }
        }
    };

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        setSuffix(mode.getValue().toString());
    };

}
