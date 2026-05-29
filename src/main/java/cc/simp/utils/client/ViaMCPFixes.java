package cc.simp.utils.client;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import com.mojang.authlib.GameProfile;
import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.PlayerLastCursorItem;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viarewind.protocol.v1_9to1_8.storage.BossBarStorage;
import com.viaversion.viarewind.protocol.v1_9to1_8.storage.PlayerPositionTracker;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemSnowball;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.optifine.Log;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static cc.simp.utils.Util.mc;

public class ViaMCPFixes {
    public static float eyeHeight;
    public static float lastEyeHeight;
    public static boolean initialized = false;
    private static boolean forceSneaking = false;
    private static long lastRenderTime = System.nanoTime();

    public static void handlePlayerSize() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (shouldSwimOrCrawl() && mc.thePlayer.isInWater()) {
            double d3 = mc.thePlayer.getLookVec().yCoord;
            double d4 = 0.025D;
            double motionBoost = d3 <= 0.0D || mc.thePlayer.worldObj.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1.0D - 0.64D, mc.thePlayer.posZ)).getBlock().getMaterial() == Material.water ? ((d3 - mc.thePlayer.motionY) * d4) + 0.018 : 0.018;
            mc.thePlayer.motionX *= 1.09F;
            mc.thePlayer.motionZ *= 1.09F;
            mc.thePlayer.motionY += motionBoost;
        }

        float newHeight = shouldSwimOrCrawl() ? 0.6f : mc.thePlayer.isSneaking() && olderThanOrEqualsTo1_8() ? 1.8f : mc.thePlayer.isSneaking() && olderThanOrEqualsTo1_13_2() ? 1.65f : mc.thePlayer.isSneaking() ? 1.5f : 1.8f;

        double d0 = mc.thePlayer.width / 2.0;
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB fixedBB = new AxisAlignedBB(mc.thePlayer.posX - d0, box.minY, mc.thePlayer.posZ - d0, mc.thePlayer.posX + d0, box.minY + mc.thePlayer.height, mc.thePlayer.posZ + d0);
        AxisAlignedBB sneakBB = new AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.8, box.minZ + 0.6);

        mc.thePlayer.setEntityBoundingBox(fixedBB);
        mc.thePlayer.height = newHeight;

        if (newerThanOrEqualsTo1_9() && mc.thePlayer.onGround && !mc.theWorld.getCollisionBoxes(sneakBB).isEmpty() && !shouldSwimOrCrawl()) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            forceSneaking = true;
        } else if (mc.theWorld.getCollisionBoxes(sneakBB).isEmpty() && forceSneaking) {
            if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
                mc.gameSettings.keyBindSneak.setPressed(false);
            forceSneaking = false;
        }
    }

    public static void handleEyeYHeight() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.nanoTime();
        float deltaTime = (now - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = now;
        deltaTime = Math.min(deltaTime, 0.05f);

        float startHeight = 1.62f;

        if (!initialized) {
            eyeHeight = startHeight;
            lastEyeHeight = eyeHeight;
            initialized = true;
            return;
        }

        float endHeight = shouldSwimOrCrawl() ? 0.45f : olderThanOrEqualsTo1_8() ? 1.54f : olderThanOrEqualsTo1_13_2() ? 1.47f : 1.32f;
        float delta = shouldSwimOrCrawl() ? 0.06f : olderThanOrEqualsTo1_8() ? 0.154f : olderThanOrEqualsTo1_13_2() ? 0.147f : 0.132f;

        lastEyeHeight = eyeHeight;

        Float target = shouldSwimOrCrawl() || mc.thePlayer.isSneaking() ? Float.valueOf(endHeight) : eyeHeight < startHeight ? startHeight : null;

        if (target != null) {
            eyeHeight = lerp(eyeHeight, target, 100f * delta * deltaTime);
        }
    }

    public static boolean shouldNotPushout() {
        return shouldSwimOrCrawl() || newerThanOrEqualsTo1_13() && mc.thePlayer.isSneaking();
    }

    public static boolean shouldSwimOrCrawl() {
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB crawlBB = new AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.5, box.minZ + 0.6);

        return newerThanOrEqualsTo1_13() && (canSwim() || !mc.theWorld.getCollisionBoxes(crawlBB).isEmpty());
    }

    private static boolean isUnderWater() {
        final World world = mc.thePlayer.getEntityWorld();
        double eyeBlock = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - 0.25;
        BlockPos blockPos = new BlockPos(mc.thePlayer.posX, eyeBlock, mc.thePlayer.posZ);

        return world.getBlockState(blockPos).getBlock().getMaterial() == Material.water && !(mc.thePlayer.ridingEntity instanceof EntityBoat);
    }

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Queue<PacketWrapper> confirmations = new ConcurrentLinkedQueue<>();

    public static void handleFixedSendPackets(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();

        if (event.isCancelled()) return;

        if (!mc.isSingleplayer() && ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_11) && packet instanceof C08PacketPlayerBlockPlacement && ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockDirection() != 255) {
            event.setCancelled();
            PacketWrapper fixedC08 = PacketWrapper.create(ServerboundPackets1_9.USE_ITEM_ON, Via.getManager().getConnectionManager().getConnections().iterator().next());
            fixedC08.write(Types.BLOCK_POSITION1_8, new BlockPosition(((C08PacketPlayerBlockPlacement) packet).getPosition().getX(), ((C08PacketPlayerBlockPlacement) packet).getPosition().getY(), ((C08PacketPlayerBlockPlacement) packet).getPosition().getZ()));
            fixedC08.write(Types.VAR_INT, ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockDirection());
            fixedC08.write(Types.VAR_INT, 0);
            fixedC08.write(Types.FLOAT, ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockOffsetX());
            fixedC08.write(Types.FLOAT, ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockOffsetY());
            fixedC08.write(Types.FLOAT, ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockOffsetZ());
            fixedC08.sendToServer(Protocol1_11To1_10.class);
        }

        if (!mc.isSingleplayer() && ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_9) && packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity) packet;
            if (((C02PacketUseEntity) packet).getAction() == C02PacketUseEntity.Action.INTERACT_AT) {
                Vec3 hitVec = c02.getHitVec();
                Entity entity = c02.getEntityFromWorld(mc.theWorld);

                if (hitVec == null || entity == null || entity instanceof EntityItemFrame || entity instanceof EntityFireball)
                    return;

                float w = entity.width;
                float h = entity.height;
                ((C02PacketUseEntity) packet).setHitVec(new Vec3(Math.max(-(w / 2.0D), Math.min(w / 2.0D, hitVec.xCoord)), Math.max(0.0D, Math.min(h, hitVec.yCoord)), Math.max(-(w / 2.0D), Math.min(w / 2.0D, hitVec.zCoord))));
            }
        }

        if (!mc.isSingleplayer() && packet instanceof C09PacketHeldItemChange) {
            // do nothing
        } else {
            if (!mc.isSingleplayer() && ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8) && packet instanceof C0APacketAnimation) {
                event.setCancelled();
                PacketWrapper fixedC0A = PacketWrapper.create(ServerboundPackets1_9.SWING, Via.getManager().getConnectionManager().getConnections().iterator().next());
                fixedC0A.write(Types.VAR_INT, 0);
                fixedC0A.sendToServer(Protocol1_9To1_8.class);
            }

            if (!mc.isSingleplayer() && ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_16) && (packet instanceof C08PacketPlayerBlockPlacement && mc.theWorld.getBlockState(((C08PacketPlayerBlockPlacement) packet).getPosition()).getBlock() instanceof BlockAir && (((C08PacketPlayerBlockPlacement) packet).getStack().getItem() instanceof ItemSnowball || ((C08PacketPlayerBlockPlacement) packet).getStack().getItem() instanceof ItemEnderPearl || ((C08PacketPlayerBlockPlacement) packet).getStack().getItem() instanceof ItemEnderEye || ((C08PacketPlayerBlockPlacement) packet).getStack().getItem() instanceof ItemExpBottle || (((C08PacketPlayerBlockPlacement) packet).getStack().getMetadata() & 0x4000) != 0) || packet instanceof C0EPacketClickWindow && (((C0EPacketClickWindow) packet).getMode() == 4 || ((C0EPacketClickWindow) packet).getSlotId() == -999) || packet instanceof C07PacketPlayerDigging && mc.thePlayer.getHeldItem() != null && (((C07PacketPlayerDigging) packet).getStatus() == C07PacketPlayerDigging.Action.DROP_ITEM || ((C07PacketPlayerDigging) packet).getStatus() == C07PacketPlayerDigging.Action.DROP_ALL_ITEMS))) {
                PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_9.SWING, Via.getManager().getConnectionManager().getConnections().iterator().next());
                swingPacket.write(Types.VAR_INT, 0);
                swingPacket.sendToServer(Protocol1_9To1_8.class);
            }
        }
    }

    public static void applyNibblesPatches() {
        ProtocolManager protocolManager = Via.getManager().getProtocolManager();
        Protocol1_9To1_8 protocol1_9To1_8 = protocolManager.getProtocol(Protocol1_9To1_8.class);
        Protocol1_17To1_16_4 protocol1_17To1_16_4 = protocolManager.getProtocol(Protocol1_17To1_16_4.class);
        if (mc.isSingleplayer() || protocol1_9To1_8 == null || protocol1_17To1_16_4 == null) {
            System.out.println("VIA crash!");
            return;
        }

        protocol1_9To1_8.registerClientbound(ClientboundPackets1_9.PLAYER_POSITION, new PacketHandlers() {
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

        protocol1_9To1_8.registerServerbound(ServerboundPackets1_8.MOVE_PLAYER_POS_ROT, wrapper -> {
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

        protocol1_17To1_16_4.registerClientbound(ClientboundPackets1_17.PING, ClientboundPackets1_16_2.CONTAINER_ACK, wrapper -> {}, true);
        protocol1_17To1_16_4.registerServerbound(ServerboundPackets1_16_2.CONTAINER_ACK, ServerboundPackets1_17.PONG, wrapper -> {}, true);

        protocol1_17To1_16_4.registerServerbound(ServerboundPackets1_16_2.CONTAINER_CLICK, ServerboundPackets1_17.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE);
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Types.SHORT);
                    byte button = wrapper.passthrough(Types.BYTE);
                    wrapper.read(Types.SHORT);
                    int mode = wrapper.passthrough(Types.VAR_INT);

                    Item clicked = protocol1_17To1_16_4.getItemRewriter()
                            .handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2));

                    wrapper.write(Types.VAR_INT, 0);

                    PlayerLastCursorItem state = wrapper.user().get(PlayerLastCursorItem.class);
                    if (state == null) {
                        wrapper.write(Types.ITEM1_13_2, clicked);
                        return;
                    }

                    if (mode == 0 && button == 0 && clicked != null) {
                        state.setLastCursorItem(clicked);
                    } else if (mode == 0 && button == 1 && clicked != null) {
                        if (state.isSet()) {
                            state.setLastCursorItem(clicked);
                        } else {
                            state.setLastCursorItem(clicked, (clicked.amount() + 1) / 2);
                        }
                    } else if (!(mode == 5 && (slot == -999 && (button == 0 || button == 4) || (button == 1 || button == 5)))) {
                        state.setLastCursorItem(null);
                    }

                    Item carried = state.getLastCursorItem();
                    wrapper.write(Types.ITEM1_13_2, carried == null ? clicked : carried);
                });
            }
        }, true);

        System.out.println("VIA fixed!");
    }

    private static boolean canSwim() {
        return !mc.thePlayer.isSneaking() && mc.thePlayer.isInWater() && !mc.thePlayer.capabilities.isFlying && mc.thePlayer.isSprinting() && isUnderWater();
    }

    private static boolean olderThanOrEqualsTo1_8() {
        return (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8));
    }

    private static boolean olderThanOrEqualsTo1_13_2() {
        return ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2);
    }

    private static boolean newerThanOrEqualsTo1_13() {
        return ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_13);
    }

    private static boolean newerThanOrEqualsTo1_9() {
        return ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_9);
    }

    public static float lerp(float from, float to, float speed) {
        return from + (to - from) * speed;
    }
}
