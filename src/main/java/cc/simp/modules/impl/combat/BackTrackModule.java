package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.LagProcess;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.animations.ContinualAnimation;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT)
public final class BackTrackModule extends Module {

    public static NumberProperty minDelayProperty = new NumberProperty("Min Delay", 50.0, 0.0, 5000.0, 10.0);
    public static NumberProperty maxDelayProperty = new NumberProperty("Max Delay", 200.0, 0.0, 5000.0, 10.0);
    public static NumberProperty activateDist = new NumberProperty("Activate Distance", 0.0, 0.0, 10.0, 0.1);
    public static NumberProperty deactivateDist = new NumberProperty("Deactivate Distance", 10.0, 0.0, 10.0, 0.1);
    public static ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Constant);
    public Property<Boolean> cancelClientPacketsProperty = new Property<>("Cancel Client Packets", true);
    public Property<Boolean> swingCheckProperty = new Property<>("Swing Check", true);
    public Property<Boolean> releaseOnDamageProperty = new Property<>("Release On Damage", true);

    public enum Mode {
        Constant,
        Hit,
        Zero
    }

    public EntityPlayer target;
    public Vec3 realPosition = new Vec3(0, 0, 0);
    private final ContinualAnimation animatedX = new ContinualAnimation();
    private final ContinualAnimation animatedY = new ContinualAnimation();
    private final ContinualAnimation animatedZ = new ContinualAnimation();
    private int ping;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {

        if(e.isPre()) return;

        setSuffix(ping + " ms");

        if (mc.thePlayer.isDead) {
            LagProcess.disable();
            LagProcess.dispatch();
            return;
        }

        if (!(TargetSelectionProcess.getTarget() instanceof EntityPlayer)) {
            LagProcess.disable();
            LagProcess.dispatch();
            return;
        }

        target = (EntityPlayer) TargetSelectionProcess.getTarget();


        if (swingCheckProperty.getValue() && !mc.thePlayer.isSwingInProgress)
            return;

        double realDistance = realPosition.distanceTo(mc.thePlayer);
        double clientDistance = target.getDistanceToEntity(mc.thePlayer);

        boolean on = realDistance > clientDistance && realDistance >= activateDist.getValue() && realDistance <= deactivateDist.getValue() && shouldActive(target) && (!releaseOnDamageProperty.getValue() || mc.thePlayer.hurtTime == 0);

        if (on) {
            if (shouldActive(target)) {
                ping = (int) MathUtils.getRandom(minDelayProperty.getValue().intValue(), maxDelayProperty.getValue().intValue());
                LagProcess.spoof(ping, true, true, true, true, cancelClientPacketsProperty.getValue(), cancelClientPacketsProperty.getValue());
            } else {
                LagProcess.disable();
                LagProcess.dispatch();
            }
        } else {
            LagProcess.disable();
            LagProcess.dispatch();
        }
    };

    @EventLink()
    public final Listener<PacketReceiveEvent> onPacketReceiveEvent = event -> {
        final Packet<?> packet = event.getPacket();

        if (target == null) {
            return;
        }

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity s14PacketEntity = ((S14PacketEntity) packet);

            if (s14PacketEntity.entityId == target.getEntityId()) {
                realPosition.xCoord += s14PacketEntity.getPosX() / 32D;
                realPosition.yCoord += s14PacketEntity.getPosY() / 32D;
                realPosition.zCoord += s14PacketEntity.getPosZ() / 32D;
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport s18PacketEntityTeleport = ((S18PacketEntityTeleport) packet);

            if (s18PacketEntityTeleport.getEntityId() == target.getEntityId()) {
                realPosition = new Vec3(s18PacketEntityTeleport.getX() / 32D, s18PacketEntityTeleport.getY() / 32D, s18PacketEntityTeleport.getZ() / 32D);
            }
        }
    };

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        if (target != null && shouldActive(target) && mc.thePlayer.getDistanceToEntity(target) <= deactivateDist.getValue() && mc.thePlayer.isSwingInProgress) {
            double x = realPosition.xCoord - mc.getRenderManager().viewerPosX;
            double y = realPosition.yCoord - mc.getRenderManager().viewerPosY;
            double z = realPosition.zCoord - mc.getRenderManager().viewerPosZ;

            animatedX.animate((float) x, 5);
            animatedY.animate((float) y, 5);
            animatedZ.animate((float) z, 5);

            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + animatedX.getOutput(), box.minY - mc.thePlayer.posY + animatedY.getOutput(), box.minZ - mc.thePlayer.posZ + animatedZ.getOutput(), box.maxX - mc.thePlayer.posX + animatedX.getOutput(), box.maxY - mc.thePlayer.posY + animatedY.getOutput(), box.maxZ - mc.thePlayer.posZ + animatedZ.getOutput());
            RenderUtils.renderBoundingBox(axis, target.hurtTime != 0 ? Color.RED : Color.GREEN, 160);
        }
    };

    public boolean shouldActive(EntityPlayer target) {
        return modeProperty.getValue() == Mode.Constant || modeProperty.getValue() == Mode.Hit && target.hurtTime != 0 || modeProperty.getValue() == Mode.Zero && target.hurtTime == 0;
    }
}
