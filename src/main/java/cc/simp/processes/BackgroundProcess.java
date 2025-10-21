package cc.simp.processes;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.MinMotionEvent;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.world.BlockCollisionEvent;
import cc.simp.modules.impl.client.ClickInterfaceModule;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.misc.PlayPongC2SPacket;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import static cc.simp.utils.Util.mc;

public class BackgroundProcess {

    private Timer cfgTimer = new Timer();

    private boolean lastGround;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (this.cfgTimer.hasTimeElapsed(30000, true)) Simp.INSTANCE.getConfigManager().saveConfig("default");
        DraggingProcess.update();

        // Bounds Fix
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            mc.thePlayer.setEntityBoundingBox(new AxisAlignedBB(mc.thePlayer.posX - 0.3, mc.thePlayer.posY,
                    mc.thePlayer.posZ - 0.3, mc.thePlayer.posX + 0.3, mc.thePlayer.posY + 1.8,
                    mc.thePlayer.posZ + 0.3));
        }

        // Lag Fix
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            LagProcess.spoof(1, true, true, false, true);
        }

    };


    @EventLink
    public final Listener<PacketSendEvent> onPacketSend = event -> {

        // Block Placement Fix
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_11)) {
            final Packet<?> packet = event.getPacket();

            if (packet instanceof C08PacketPlayerBlockPlacement) {
                final C08PacketPlayerBlockPlacement wrapper = ((C08PacketPlayerBlockPlacement) packet);
                wrapper.facingX /= 16.0F;
                wrapper.facingY /= 16.0F;
                wrapper.facingZ /= 16.0F;
                event.setPacket(wrapper);
            }
        }

        // Flying Packet Fix
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            final Packet<?> packet = event.getPacket();

            if (packet instanceof C03PacketPlayer) {
                final C03PacketPlayer wrapper = ((C03PacketPlayer) packet);

                if (!wrapper.isMoving() && !wrapper.getRotating() && wrapper.onGround == this.lastGround) {
                    event.setCancelled();
                }

                this.lastGround = wrapper.onGround;
            }
        }

        // Entity Interact Fix
        if (!event.isCancelled() && ViaLoadingBase.getInstance()
                .getTargetVersion().newerThan(ProtocolVersion.v1_8)) {

            if (event.getPacket() instanceof C02PacketUseEntity) {
                C02PacketUseEntity use = ((C02PacketUseEntity) event.getPacket());

                event.setCancelled(event.isCancelled() || !use.getAction().equals(C02PacketUseEntity.Action.ATTACK));
            }
        }

        // Lag Fix Continues
        if (!event.isCancelled() && ViaLoadingBase.getInstance()
                .getTargetVersion().newerThan(ProtocolVersion.v1_8) && event.getPacket() instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport tp = ((S18PacketEntityTeleport) event.getPacket());
            if (tp.getEntityId() == mc.thePlayer.getEntityId()) {
                if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
                    LagProcess.dispatch();
                }
            }
        }

        // Transaction Fixes
        if (!event.isCancelled() && ViaLoadingBase.getInstance()
                .getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            if (event.getPacket() instanceof C0FPacketConfirmTransaction) {
                C0FPacketConfirmTransaction transaction = ((C0FPacketConfirmTransaction) event.getPacket());

                if (false) Minecraft.getMinecraft().addScheduledTask(() -> PacketUtils.sendPacket(
                        new PlayPongC2SPacket(transaction.getUid())));

                PacketUtils.sendPacket(
                        new PlayPongC2SPacket(transaction.getUid()));
                event.setCancelled();
            }
        }

    };

    // Speed Fixes
    @EventLink(value = Priorities.LOW)
    public final Listener<StrafeEvent> onStrafe = event -> {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)) return;

            float[][] friction = {new float[]{0.11999998f, 0.15599997f}, new float[]{0.13999997f, 0.18199998f}};

            int speed = Math.min(mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier(), 1);
            boolean ground = mc.thePlayer.onGround;
            boolean sprinting = mc.thePlayer.isSprinting();

            if (ground) event.setFriction(friction[speed][sprinting ? 1 : 0]);
        }
    };

    // Min Motion Fix
    @EventLink
    public final Listener<MinMotionEvent> onMinimumMotion = event -> {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            event.setMinimumMotion(0.003D);
        }
    };

    // Ladder Fix
    @EventLink
    public final Listener<BlockCollisionEvent> blockCollisionEventListener = event -> {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
            final Block block = event.getBlock();

            if (block instanceof BlockLadder) {
                final BlockPos blockPos = event.getBlockPos();
                final IBlockState iblockstate = mc.theWorld.getBlockState(blockPos);

                if (iblockstate.getBlock() == block) {
                    final float f = 0.125F + 0.0625f;

                    switch (iblockstate.getValue(BlockLadder.FACING)) {
                        case NORTH:
                            event.setBoundingBox(new AxisAlignedBB(0.0F, 0.0F, 1.0F - f, 1.0F, 1.0F, 1.0F)
                                    .offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                            break;

                        case SOUTH:
                            event.setBoundingBox(new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, f)
                                    .offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                            break;

                        case WEST:
                            event.setBoundingBox(new AxisAlignedBB(1.0F - f, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F)
                                    .offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                            break;

                        case EAST:
                        default:
                            event.setBoundingBox(new AxisAlignedBB(0.0F, 0.0F, 0.0F, f, 1.0F, 1.0F)
                                    .offset(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                    }
                }
            }
        }
    };

}
