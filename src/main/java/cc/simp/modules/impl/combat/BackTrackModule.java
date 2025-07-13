package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.CancellableEvent;
import cc.simp.event.Event;
import cc.simp.event.impl.packet.PacketReceiveEvent;
import cc.simp.event.impl.packet.PacketSendEvent;
import cc.simp.event.impl.player.PreUpdateEvent;
import cc.simp.event.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.*;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT)
public class BackTrackModule extends Module {

    public static DoubleProperty delay = new DoubleProperty("Delayed Position Time", 400.0, 0.0, 1000.0, 10.0);

    public Property<Boolean> legit = new Property<>("Legit", true);

    public Property<Boolean> releaseOnHit = new Property<>("Release Upon Hit", true, legit::isAvailable);

    public DoubleProperty hitRange = new DoubleProperty("Hit Range", 3.0, 0.0, 10.0, 0.1);

    public Property<Boolean> onlyIfNeed = new Property<>("Only If Needed", true);

    public static final ArrayList<Packet> incomingPackets = new ArrayList<>();

    public static final ArrayList<Packet> outgoingPackets = new ArrayList<>();

    public double lastRealX;

    public double lastRealY;

    public double lastRealZ;

    private WorldClient lastWorld;

    private EntityLivingBase entity;

    public Timer timer = new Timer();

    private final Map<UUID, Deque<Vec3>> backtrackPositions = new HashMap<UUID, Deque<Vec3>>();

    @EventLink
    private final Listener<PacketReceiveEvent> packetReceiveEventListener = e -> {
        EntityLivingBase entityLivingBase;
        Entity entity;
        Packet<INetHandlerPlayClient> packet;
        if (mc.thePlayer == null || mc.theWorld == null || !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() || mc.getNetHandler().getNetworkManager().getNetHandler() == null) {
            incomingPackets.clear();
            return;
        }
        if (Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            incomingPackets.clear();
            return;
        }

        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            incomingPackets.clear();
            return;
        }

        entity = KillAuraModule.target;

        if (e.getPacket() instanceof S14PacketEntity) {
            packet = (S14PacketEntity) e.getPacket();
            entity = mc.theWorld.getEntityByID(((S14PacketEntity) packet).entityId);
            if (entity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase) entity;
                entityLivingBase.realPosX += ((S14PacketEntity) packet).func_149062_c();
                entityLivingBase.realPosY += ((S14PacketEntity) packet).func_149061_d();
                entityLivingBase.realPosZ += ((S14PacketEntity) packet).func_149064_e();
            }
        }
        if (e.getPacket() instanceof S18PacketEntityTeleport) {
            final S18PacketEntityTeleport packet2 = (S18PacketEntityTeleport) e.getPacket();
            final Entity entity1 = mc.theWorld.getEntityByID(packet2.getEntityId());
            if (entity1 instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase) entity1;
                entityLivingBase.realPosX = packet2.getX();
                entityLivingBase.realPosY = packet2.getY();
                entityLivingBase.realPosZ = packet2.getZ();
            }
        }

        if (mc.theWorld != null && lastWorld != mc.theWorld) {
            resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            lastWorld = mc.theWorld;
            return;
        }
        if (entity == null || onlyIfNeed.getValue() && mc.thePlayer.getDistanceToEntity(entity) < 3.0f) {
            resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
        } else {
            addIncomingPackets(e.getPacket(), e);
        }
    };

    @EventLink
    private final Listener<PacketSendEvent> packetSendEventListener = e -> {
        if (mc.thePlayer == null || mc.theWorld == null || !Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() || mc.getNetHandler().getNetworkManager().getNetHandler() == null) {
            outgoingPackets.clear();
            return;
        }
        if (Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            outgoingPackets.clear();
            return;
        }
        entity = KillAuraModule.target;
        if (mc.theWorld != null && lastWorld != mc.theWorld) {
            resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            lastWorld = mc.theWorld;
            return;
        }
        if (entity == null || onlyIfNeed.getValue() && mc.thePlayer.getDistanceToEntity(entity) < 3.0f) {
            resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            outgoingPackets.clear();
        } else {
            addOutgoingPackets(e.getPacket(), e);
        }
    };

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (entity != null && entity.getEntityBoundingBox() != null && mc.thePlayer != null && mc.theWorld != null && entity.realPosX != 0.0 && entity.realPosY != 0.0 && entity.realPosZ != 0.0 && entity.width != 0.0f && entity.height != 0.0f && entity.posX != 0.0 && entity.posY != 0.0 && entity.posZ != 0.0) {
            double realX = entity.realPosX / 32.0;
            double realY = entity.realPosY / 32.0;
            double realZ = entity.realPosZ / 32.0;
            if (!onlyIfNeed.getValue()) {
                if (mc.thePlayer.getDistanceToEntity(entity) > 3.0f && mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ)) {
                    resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                    resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                }
            } else if (mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ) || mc.thePlayer.getDistance(realX, realY, realZ) < mc.thePlayer.getDistance(lastRealX, lastRealY, lastRealZ)) {
                resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            if (legit.getValue() && releaseOnHit.getValue() && entity.hurtTime <= 1) {
                resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRange.getValue() || timer.hasTimeElapsed(delay.getValue(), true)) {
                resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            lastRealX = realX;
            lastRealY = realY;
            lastRealZ = realZ;
        }
    };

    @EventLink
    private final Listener<Render3DEvent> render3DEventListener = event -> {
        block17:
        {
            if (entity == null || entity.getEntityBoundingBox() == null || mc.thePlayer == null || mc.theWorld == null || entity.realPosX == 0.0 || entity.realPosY == 0.0 || entity.realPosZ == 0.0 || entity.width == 0.0f || entity.height == 0.0f || entity.posX == 0.0 || entity.posY == 0.0 || entity.posZ == 0.0)
                break block17;
            boolean render = true;
            double realX = entity.realPosX / 32.0;
            double realY = entity.realPosY / 32.0;
            double realZ = entity.realPosZ / 32.0;
            if (!onlyIfNeed.getValue()) {
                if (mc.thePlayer.getDistanceToEntity(entity) > 3.0f && mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ)) {
                    render = false;
                }
            } else if (mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ) || mc.thePlayer.getDistance(realX, realY, realZ) < mc.thePlayer.getDistance(lastRealX, lastRealY, lastRealZ)) {
                render = false;
            }
            if (legit.getValue() && releaseOnHit.getValue() && entity.hurtTime <= 1) {
                render = false;
            }
            if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRange.getValue() || timer.hasTimeElapsed(delay.getValue(), false)) {
                render = false;
            }
            if (entity == null || entity == mc.thePlayer || entity.isInvisible() || !render)
                break block17;
            if (entity == null || entity.width == 0.0f || entity.height == 0.0f) {
                return;
            }
            Color color = Color.WHITE;
            double d = entity.realPosX / 32.0;
            double x = d - RenderManager.renderPosX;
            double d2 = entity.realPosY / 32.0;
            double y = d2 - RenderManager.renderPosY;
            double d3 = entity.realPosZ / 32.0;
            double z = d3 - RenderManager.renderPosZ;
            GlStateManager.pushMatrix();
            RenderUtils.start3D();
            RenderUtils.renderFilledBoundingBox(new AxisAlignedBB(x - (double) (entity.width / 2.0f), y, z - (double) (entity.width / 2.0f), x + (double) (entity.width / 2.0f), y + (double) entity.height, z + (double) (entity.width / 2.0f)), color, 0.7f);
            RenderUtils.stop3D();
            GlStateManager.popMatrix();
        }
    };

    private void resetIncomingPackets(INetHandler netHandler) {
        if (!incomingPackets.isEmpty()) {
            while (!incomingPackets.isEmpty()) {
                final Packet packet = incomingPackets.get(0);
                try {
                    if (Simp.INSTANCE.getModuleManager().getModule(AntiKnockbackModule.class).isEnabled()) {
                        if (!(incomingPackets.get(0) instanceof S12PacketEntityVelocity)) {
                            if (!(incomingPackets.get(0) instanceof S27PacketExplosion)) {
                                incomingPackets.get(0).processPacket(netHandler);
                            }
                        }
                    } else {
                        incomingPackets.get(0).processPacket(netHandler);
                    }
                } catch (ThreadQuickExitException ignored) {

                }
                incomingPackets.remove((0));
            }
        }
        timer.reset();
    }

    private void addIncomingPackets(Packet packet, CancellableEvent event) {
        if (event != null && packet != null) {
            synchronized (incomingPackets) {
                if (blockPacketIncoming(packet)) {
                    incomingPackets.add(packet);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void resetOutgoingPackets(INetHandler netHandler) {
        if (!outgoingPackets.isEmpty()) {
            while (!outgoingPackets.isEmpty()) {
                final Packet packet = outgoingPackets.get(0);
                try {
                    outgoingPackets.get(0).processPacket(netHandler);
                } catch (ThreadQuickExitException ignored) {

                }
                outgoingPackets.remove((0));
            }
        }
        timer.reset();
    }

    private void addOutgoingPackets(Packet packet, CancellableEvent event) {
        if (event != null && packet != null) {
            synchronized (outgoingPackets) {
                if (blockPacketsOutgoing(packet)) {
                    outgoingPackets.add(packet);
                    event.setCancelled(true);
                }

            }
        }
    }

    private boolean isEntityPacket(Packet packet) {
        return packet instanceof S14PacketEntity || packet instanceof S19PacketEntityHeadLook || packet instanceof S18PacketEntityTeleport || packet instanceof S0FPacketSpawnMob;
    }

    private boolean blockPacketIncoming(Packet packet) {
        return packet instanceof S00PacketKeepAlive || packet instanceof S12PacketEntityVelocity || packet instanceof S27PacketExplosion || packet instanceof S32PacketConfirmTransaction || packet instanceof S08PacketPlayerPosLook || packet instanceof S01PacketPong || isEntityPacket(packet);
    }

    private boolean blockPacketsOutgoing(Packet packet) {
        return packet instanceof C03PacketPlayer || packet instanceof C02PacketUseEntity || packet instanceof C0FPacketConfirmTransaction || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C09PacketHeldItemChange || packet instanceof C07PacketPlayerDigging || packet instanceof C0APacketAnimation || packet instanceof C01PacketPing || packet instanceof C00PacketKeepAlive || packet instanceof C0BPacketEntityAction;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        incomingPackets.clear();
        outgoingPackets.clear();
        if (mc.theWorld != null && mc.thePlayer != null) {
            for (final Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase) {
                    final EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                    entityLivingBase.realPosX = entityLivingBase.serverPosX;
                    entityLivingBase.realPosZ = entityLivingBase.serverPosZ;
                    entityLivingBase.realPosY = entityLivingBase.serverPosY;
                }
            }
        }
        super.onEnable();
    }
}
