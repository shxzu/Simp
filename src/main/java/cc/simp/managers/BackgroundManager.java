package cc.simp.managers;

import cc.simp.Simp;
import cc.simp.event.impl.packet.PacketReceiveEvent;
import cc.simp.event.impl.packet.PacketSendEvent;
import cc.simp.event.impl.player.PreUpdateEvent;
import cc.simp.event.impl.render.Render2DEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.font.FontManager;
import cc.simp.modules.impl.client.FontManagerModule;
import cc.simp.utils.Timer;
import com.viaversion.viabackwards.api.entities.storage.PlayerPositionStorage;
import com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viarewind.protocol.v1_9to1_8.storage.BossBarStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.ViaMCP;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import com.viaversion.viarewind.protocol.v1_9to1_8.storage.PlayerPositionTracker;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static cc.simp.utils.Util.mc;

public class BackgroundManager {

    private int rotLockTick;
    private boolean worldFullLoaded;
    private boolean loadingWorld;
    private Timer cfgTimer = new Timer();
    private static final Queue<PacketWrapper> confirmations = new ConcurrentLinkedQueue<>();

    public BackgroundManager() {
        this.worldFullLoaded = true;
        this.loadingWorld = false;
        this.rotLockTick = 0;
    }

    public boolean isWorldFullLoaded() {
        return this.worldFullLoaded && !this.loadingWorld;
    }

    public boolean canRotation() {
        return this.rotLockTick == 0;
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        this.worldFullLoaded = false;
        this.loadingWorld = true;
        this.rotLockTick = 0;
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {

        if (this.cfgTimer.hasTimeElapsed(30000, true)) Simp.INSTANCE.getConfigManager().saveConfig("default");

        if (this.rotLockTick > 0) {
            --this.rotLockTick;
        }

        if (this.loadingWorld && mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.ticksExisted > 10.0f && mc.currentScreen == null) {
            this.worldFullLoaded = true;
            this.loadingWorld = false;
        }

        // W.I.P. VIA VERSION FIX!! (Someone Should Fix This For Me.)

        /*if(Via.getManager().getProtocolManager().getProtocol(Protocol1_9To1_8.class) != null) {

            Via.getManager().getProtocolManager().getProtocol(Protocol1_9To1_8.class).registerServerbound(ServerboundPackets1_8.MOVE_PLAYER_POS_ROT, wrapper -> {
                PacketWrapper c = confirmations.poll();
                if (c != null) {
                    c.sendToServer(Protocol1_9To1_8.class);
                }

                double x = wrapper.passthrough(Types.DOUBLE);
                double y = wrapper.passthrough(Types.DOUBLE);
                double z = wrapper.passthrough(Types.DOUBLE);
                float yaw = wrapper.passthrough(Types.FLOAT);
                float pitch = wrapper.passthrough(Types.FLOAT);
                boolean onGround = wrapper.passthrough(Types.BOOLEAN);
                PlayerPositionTracker tracker = wrapper.user().get(PlayerPositionTracker.class);
                if (tracker != null) {
                    tracker.sendAnimations();
                    if (tracker.getConfirmId() != -1) {
                        if (
                                tracker.getPosX() == x &&
                                        tracker.getPosY() == y &&
                                        tracker.getPosZ() == z &&
                                        tracker.getYaw() == yaw &&
                                        tracker.getPitch() == pitch
                        ) {
                            tracker.setConfirmId(-1);
                        }
                    } else {
                        tracker.setPos(x, y, z);
                        tracker.setYaw(yaw);
                        tracker.setPitch(pitch);
                        tracker.setOnGround(onGround);
                        BossBarStorage storage = wrapper.user().get(BossBarStorage.class);
                        if (storage != null) {
                            storage.updateLocation();
                        }
                    }
                }
            });

        Via.getManager().getProtocolManager().getProtocol(Protocol1_9To1_8.class).registerClientbound(ClientboundPackets1_9.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.BYTE);
                handler(wrapper -> {
                    int id = wrapper.read(Types.VAR_INT);
                    PacketWrapper c = PacketWrapper.create(ServerboundPackets1_9.ACCEPT_TELEPORTATION, wrapper.user());
                    c.write(Types.VAR_INT, id);
                    confirmations.offer(c);

                    PlayerPositionTracker tracker = wrapper.user().get(PlayerPositionTracker.class);
                    if (tracker != null) {
                        tracker.setConfirmId(id);

                        byte flags = wrapper.get(Types.BYTE, 0);
                        double x = wrapper.get(Types.DOUBLE, 0);
                        double y = wrapper.get(Types.DOUBLE, 1);
                        double z = wrapper.get(Types.DOUBLE, 2);
                        float yaw = wrapper.get(Types.FLOAT, 0);
                        float pitch = wrapper.get(Types.FLOAT, 1);

                        wrapper.set(Types.BYTE, 0, (byte) 0);

                        if (flags != 0) {
                            if ((flags & 0x01) != 0) {
                                x += tracker.getPosX();
                                wrapper.set(Types.DOUBLE, 0, x);
                            }
                            if ((flags & 0x02) != 0) {
                                y += tracker.getPosY();
                                wrapper.set(Types.DOUBLE, 1, y);
                            }
                            if ((flags & 0x04) != 0) {
                                z += tracker.getPosZ();
                                wrapper.set(Types.DOUBLE, 2, z);
                            }
                            if ((flags & 0x08) != 0) {
                                yaw += tracker.getYaw();
                                wrapper.set(Types.FLOAT, 0, yaw);
                            }
                            if ((flags & 0x10) != 0) {
                                pitch += tracker.getPitch();
                                wrapper.set(Types.FLOAT, 1, pitch);
                            }
                        }

                        tracker.setPos(x, y, z);
                        tracker.setYaw(yaw);
                        tracker.setPitch(pitch);
                    }
                });
            }
        });
        } else System.out.println("skibidi");*/
    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {

        if (mc.thePlayer == null) {
            return;
        }

        // Reset Rotations Obviously

        final Packet<?> packet = event.getPacket();
        if (packet instanceof S08PacketPlayerPosLook) {
            Simp.INSTANCE.getRotationManager().resetRotationsInstantly();
            this.rotLockTick = 3;
        }
    };

    @EventLink
    public final Listener<Render2DEvent> r2d = event -> {
        String currentFont = FontManager.getCurrentFont().getNameFontTTF().toLowerCase();
        String desiredFont = FontManagerModule.fontTypeProperty.getValue().toString().toLowerCase();

        if (!currentFont.equals(desiredFont)) {
            FontManager.setCurrentFont(desiredFont);
        }

    };

}
