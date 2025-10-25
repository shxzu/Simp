package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Bed Nuker", category = ModuleCategory.PLAYER)
public final class BedNukerModule extends Module {

    private final NumberProperty breakRange = new NumberProperty("Break Range", 4.5, 1.0, 6.0, 0.1);
    private final Property<Boolean> whitelistOwnBed = new Property<>("Whitelist Own Bed", true);
    private final Property<Boolean> moveFix = new Property<>("Movement Fix", true);

    private BlockPos bedPos;
    private boolean rotate = false;
    private int breakTicks;
    private int delayTicks;
    private Vec3 home;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
            if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
                reset(true);
                return;
            }

            getBedPos();

            if (bedPos != null) {
                if (rotate) {
                    float[] rot = getRotationToBlock(bedPos, getEnumFacing(bedPos));
                    RotationProcess.setRotations(new Vector2f(rot[0], rot[1]), 7, moveFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                    rotate = false;
                }
                mine(bedPos);
            } else {
                reset(true);
            }
    };

    private void getBedPos() {
        if (home != null && mc.thePlayer.getDistanceSq(home.xCoord, home.yCoord, home.zCoord) < 35 * 35 && whitelistOwnBed.getValue()) {
            return;
        }
        bedPos = null;
        double range = breakRange.getValue();
        for (double x = mc.thePlayer.posX - range; x <= mc.thePlayer.posX + range; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - range; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + range; y++) {
                for (double z = mc.thePlayer.posZ - range; z <= mc.thePlayer.posZ + range; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(pos).getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                        bedPos = pos;
                        break;
                    }
                }
            }
        }
    }

    private void mine(BlockPos blockPos) {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        if (blockState.getBlock() instanceof BlockAir) {
            return;
        }

        float totalBreakTicks = getBreakTicks(bedPos, mc.thePlayer.inventory.currentItem);
        if (breakTicks == 0) {
            rotate = true;
            mc.thePlayer.swingItem();
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, bedPos, EnumFacing.UP));
        } else if (breakTicks >= totalBreakTicks) {
            rotate = true;
            mc.thePlayer.swingItem();
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, bedPos, EnumFacing.UP));

            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, 1);

            reset(false);
            return;
        } else {
            rotate = true;
            mc.thePlayer.swingItem();
        }

        breakTicks += 1;

        int currentProgress = (int) (((double) breakTicks / totalBreakTicks) * 100);
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, currentProgress / 10);
    }

    private void reset(boolean resetRotate) {
        if (bedPos != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, -1);
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, bedPos, EnumFacing.DOWN));
        }

        breakTicks = 0;
        delayTicks = 5;
        bedPos = null;
        rotate = !resetRotate;
    }

    private float[] getRotationToBlock(BlockPos pos, EnumFacing facing) {
        double x = pos.getX() + 0.5 - mc.thePlayer.posX;
        double y = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = pos.getZ() + 0.5 - mc.thePlayer.posZ;

        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / Math.PI));

        return new float[]{yaw, pitch};
    }

    private EnumFacing getEnumFacing(BlockPos pos) {
        Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        if (pos.getY() > eyesPos.yCoord) {
            if (isReplaceable(pos.add(0, -1, 0))) {
                return EnumFacing.DOWN;
            } else {
                return mc.thePlayer.getHorizontalFacing().getOpposite();
            }
        }

        if (!isReplaceable(pos.add(0, 1, 0))) {
            return mc.thePlayer.getHorizontalFacing().getOpposite();
        }

        return EnumFacing.UP;
    }

    private boolean isReplaceable(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().isReplaceable(mc.theWorld, pos);
    }

    private float getBreakTicks(BlockPos bp, int tool) {
        int oldHeld = mc.thePlayer.inventory.currentItem;

        mc.thePlayer.inventory.currentItem = tool;
        IBlockState bs = mc.theWorld.getBlockState(bp);
        float ticks = 1f / bs.getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, bp);

        mc.thePlayer.inventory.currentItem = oldHeld;
        return ticks;
    }

    @Override
    public void onEnable() {
        rotate = false;
        bedPos = null;
        breakTicks = 0;
        delayTicks = 0;
    }

    @Override
    public void onDisable() {
        reset(true);
    }
}