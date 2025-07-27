package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.CancellableEvent;
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
import cc.simp.utils.Timer;
import cc.simp.utils.render.RenderUtils;
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
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT)
public class BackTrackModule extends Module {

    public static DoubleProperty delayProperty = new DoubleProperty("Delayed Position Time", 400.0, 0.0, 1000.0, 10.0);
    public Property<Boolean> legitProperty = new Property<>("Legit", false);
    public Property<Boolean> releaseOnHitProperty = new Property<>("Release Upon Hit", true, legitProperty::isAvailable);
    public DoubleProperty hitRangeProperty = new DoubleProperty("Hit Range", 3.0, 0.0, 10.0, 0.1);
    public Property<Boolean> onlyIfNeedProperty = new Property<>("Only If Needed", true);

    public static final ArrayList<Packet> incomingPackets = new ArrayList<>();

    public static final ArrayList<Packet> outgoingPackets = new ArrayList<>();

    public double lastRealX;

    public double lastRealY;

    public double lastRealZ;

    private WorldClient lastWorld;

    private EntityLivingBase entity;

    public Timer timer = new Timer();
    private final Map<UUID, Deque<Vec3>> backtrackPositions = new HashMap<UUID, Deque<Vec3>>();

    @Override
    public void onEnable() {
        incomingPackets.clear();
        outgoingPackets.clear();
    }

    @EventLink
    private final Listener<PacketReceiveEvent> packetReceiveEventListener = e -> {
        EntityLivingBase entityLivingBase;
        Entity packetEntity;
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

        this.entity = KillAuraModule.target;

        if (e.getPacket() instanceof S14PacketEntity) {
            final S14PacketEntity packet = (S14PacketEntity) e.getPacket();
            packetEntity = mc.theWorld.getEntityByID(packet.entityId);
            if (packetEntity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase) packetEntity;
                entityLivingBase.realPosX += packet.func_149062_c();
                entityLivingBase.realPosY += packet.func_149061_d();
                entityLivingBase.realPosZ += packet.func_149064_e();
            }
        }
        if (e.getPacket() instanceof S18PacketEntityTeleport) {
            final S18PacketEntityTeleport packet2 = (S18PacketEntityTeleport) e.getPacket();
            packetEntity = mc.theWorld.getEntityByID(packet2.getEntityId());
            if (packetEntity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase) packetEntity;
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
        if (this.entity == null || onlyIfNeedProperty.getValue() && mc.thePlayer.getDistanceToEntity(this.entity) < 3.0f) {
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
        this.entity = KillAuraModule.target;
        if (mc.theWorld != null && lastWorld != mc.theWorld) {
            resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            lastWorld = mc.theWorld;
            return;
        }
        if (this.entity == null || onlyIfNeedProperty.getValue() && mc.thePlayer.getDistanceToEntity(this.entity) < 3.0f) {
            resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
        } else {
            addOutgoingPackets(e.getPacket(), e);
        }
    };

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (this.entity != null && this.entity.getEntityBoundingBox() != null && mc.thePlayer != null && mc.theWorld != null && this.entity.realPosX != 0.0 && this.entity.realPosY != 0.0 && this.entity.realPosZ != 0.0 && this.entity.width != 0.0f && this.entity.height != 0.0f && this.entity.posX != 0.0 && this.entity.posY != 0.0 && this.entity.posZ != 0.0) {
            double realX = this.entity.realPosX / 32.0;
            double realY = this.entity.realPosY / 32.0;
            double realZ = this.entity.realPosZ / 32.0;
            if (!onlyIfNeedProperty.getValue()) {
                if (mc.thePlayer.getDistanceToEntity(this.entity) > 3.0f && mc.thePlayer.getDistance(this.entity.posX, this.entity.posY, this.entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ)) {
                    resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                    resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                }
            } else if (mc.thePlayer.getDistance(this.entity.posX, this.entity.posY, this.entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ) || mc.thePlayer.getDistance(realX, realY, realZ) < mc.thePlayer.getDistance(lastRealX, lastRealY, lastRealZ)) {
                resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            if (legitProperty.getValue() && releaseOnHitProperty.getValue() && this.entity.hurtTime <= 1) {
                resetIncomingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
                resetOutgoingPackets(mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRangeProperty.getValue() || timer.hasTimeElapsed(delayProperty.getValue(), true)) {
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
        if (this.entity == null || this.entity.getEntityBoundingBox() == null || mc.thePlayer == null || mc.theWorld == null || this.entity.realPosX == 0.0 || this.entity.realPosY == 0.0 || this.entity.realPosZ == 0.0 || this.entity.width == 0.0f || this.entity.height == 0.0f || this.entity.posX == 0.0 || this.entity.posY == 0.0 || this.entity.posZ == 0.0)
            return;

        boolean render = true;
        double realX = this.entity.realPosX / 32.0;
        double realY = this.entity.realPosY / 32.0;
        double realZ = this.entity.realPosZ / 32.0;

        if (!onlyIfNeedProperty.getValue()) {
            if (mc.thePlayer.getDistanceToEntity(this.entity) > 3.0f && mc.thePlayer.getDistance(this.entity.posX, this.entity.posY, this.entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ)) {
                render = false;
            }
        } else if (mc.thePlayer.getDistance(this.entity.posX, this.entity.posY, this.entity.posZ) >= mc.thePlayer.getDistance(realX, realY, realZ) || mc.thePlayer.getDistance(realX, realY, realZ) < mc.thePlayer.getDistance(lastRealX, lastRealY, lastRealZ)) {
            render = false;
        }

        if (legitProperty.getValue() && releaseOnHitProperty.getValue() && this.entity.hurtTime <= 1) {
            render = false;
        }
        if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRangeProperty.getValue() || timer.hasTimeElapsed(delayProperty.getValue(), false)) {
            render = false;
        }

        if (this.entity == null || this.entity == mc.thePlayer || this.entity.isInvisible() || !render)
            return;

        if (this.entity.width == 0.0f || this.entity.height == 0.0f) {
            return;
        }

        Color color = Color.WHITE;
        int alpha = 145;

        double x = (this.entity.realPosX / 32.0) - RenderManager.renderPosX;
        double y = (this.entity.realPosY / 32.0) - RenderManager.renderPosY;
        double z = (this.entity.realPosZ / 32.0) - RenderManager.renderPosZ;

        GlStateManager.pushMatrix();
        RenderUtils.start3D();
        RenderUtils.renderBoundingBox(new AxisAlignedBB(x - (double) (this.entity.width / 2.0f), y, z - (double) (this.entity.width / 2.0f), x + (double) (this.entity.width / 2.0f), y + (double) this.entity.height, z + (double) (this.entity.width / 2.0f)), color, alpha);
        RenderUtils.stop3D();
        GlStateManager.popMatrix();
    };

    private void resetIncomingPackets(INetHandler netHandler) {
        if (!incomingPackets.isEmpty()) {
            while (!incomingPackets.isEmpty()) {
                final Packet packet = incomingPackets.get(0);
                try {
                    if (Simp.INSTANCE.getModuleManager().getModule(AntiKnockbackModule.class).isEnabled()) {
                        if (!(packet instanceof S12PacketEntityVelocity) && !(packet instanceof S27PacketExplosion)) {
                            mc.getNetHandler().sendSilentPacket(incomingPackets.get(0));
                        }
                    } else {
                        mc.getNetHandler().sendSilentPacket(incomingPackets.get(0));
                    }
                } catch (ThreadQuickExitException ignored) {
                    // Ignored exception
                }
                incomingPackets.remove(0);
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
                    mc.getNetHandler().sendSilentPacket(outgoingPackets.get(0));
                } catch (ThreadQuickExitException ignored) {
                    // Ignored exception
                }
                outgoingPackets.remove(0);
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
        return packet instanceof S03PacketTimeUpdate || packet instanceof S00PacketKeepAlive || packet instanceof S12PacketEntityVelocity || packet instanceof S27PacketExplosion || packet instanceof S32PacketConfirmTransaction || packet instanceof S08PacketPlayerPosLook || packet instanceof S01PacketPong || this.isEntityPacket(packet);
    }

    private boolean blockPacketsOutgoing(Packet packet) {
        if (!this.legitProperty.getValue()) {
            return false;
        }
        return packet instanceof C03PacketPlayer || packet instanceof C02PacketUseEntity || packet instanceof C0FPacketConfirmTransaction || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C09PacketHeldItemChange || packet instanceof C07PacketPlayerDigging || packet instanceof C0APacketAnimation || packet instanceof C01PacketPing || packet instanceof C00PacketKeepAlive || packet instanceof C0BPacketEntityAction;
    }
}