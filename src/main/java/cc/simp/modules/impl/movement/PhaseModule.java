package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.BlockCollisionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.PlayerUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Phase", category = ModuleCategory.MOVEMENT)
public final class PhaseModule extends Module {

    private boolean phasing;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if(!event.isPre()) return;
        this.phasing = false;

        final double rotation = Math.toRadians(mc.thePlayer.rotationYaw);

        final double x = Math.sin(rotation);
        final double z = Math.cos(rotation);

        if (mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.setPosition(mc.thePlayer.posX - x * 0.005, mc.thePlayer.posY, mc.thePlayer.posZ + z * 0.005);
            this.phasing = true;
        } else if (PlayerUtils.insideBlock()) {
            PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX - x * 1.5, mc.thePlayer.posY, mc.thePlayer.posZ + z * 1.5, false));

            mc.thePlayer.motionX *= 0.3D;
            mc.thePlayer.motionZ *= 0.3D;

            this.phasing = true;
        }
    };

    @EventLink
    public final Listener<BlockCollisionEvent> blockCollisionEventListener = event -> {
        if (event.getBlock() instanceof BlockAir && phasing) {
            final double x = event.getBlockPos().getX(), y = event.getBlockPos().getY(), z = event.getBlockPos().getZ();

            if (y < mc.thePlayer.posY) {
                event.setBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
            }
        }
    };

}
