package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.EnumFacingOffset;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Clutch", category = ModuleCategory.PLAYER)
public class ClutchModule extends Module {

    private final NumberProperty minRotationSpeed = new NumberProperty("Min Rotation Speed", 5, 0, 10, 1);
    private final NumberProperty maxRotationSpeed = new NumberProperty("Max Rotation Speed", 8, 0, 10, 1);
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 10, 1);

    private Vec3 targetBlock;
    private EnumFacingOffset enumFacing;
    private BlockPos blockFace;
    private float targetYaw, targetPitch;
    private int ticksOnAir;
    private int toggle;

    @Override
    public void onEnable() {
        targetYaw = mc.thePlayer.rotationYaw - 180;
        targetPitch = 90;

        targetBlock = null;
    }

    public void calculateRotations() {
        if (ticksOnAir > 0 && !RayCastUtils.overBlock(RotationProcess.rotations, enumFacing.getEnumFacing(), blockFace, true)) {
            getRotations(0);
        }

        /* Smoothing rotations */
        final double minRotationSpeed = this.maxRotationSpeed.getValue();
        final double maxRotationSpeed = this.minRotationSpeed.getValue();
        float rotationSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);

        if (rotationSpeed != 0) {
            RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotationSpeed, MovementFix.NORMAL);
        }
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (mc.thePlayer.ticksExisted <= 50 ||
                BadPacketsProcess.bad() || Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() ||
                (!mc.gameSettings.keyBindSneak.isKeyDown())) return;

        if (mc.thePlayer.offGroundTicks > 3 && !PlayerUtils.isBlockUnder()) {
            toggle = 10;
        }

        if (toggle-- < 0) return;

        // Getting ItemSlot
        if (InventoryUtils.findBlock() != -1) {
            mc.thePlayer.inventory.currentItem = InventoryUtils.findBlock();
        }

        final Vec3i offset = new Vec3i(0, 0, 0);

        //Used to detect when to place a block, if over air, allow placement of blocks
        if (PlayerUtils.blockRelativeToPlayer(offset.getX(), -1 + offset.getY(), offset.getZ()).isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down())) {
            ticksOnAir++;
        } else {
            ticksOnAir = 0;
        }

        // Gets block to place
        targetBlock = PlayerUtils.getPlacePossibility(offset.getX(), offset.getY(), offset.getZ());

        if (targetBlock == null) {
            return;
        }

        //Gets EnumFacing
        enumFacing = PlayerUtils.getEnumFacing(targetBlock);

        if (enumFacing == null) {
            return;
        }

        final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);

        blockFace = position.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);

        if (blockFace == null || enumFacing == null) {
            return;
        }

        this.calculateRotations();

        if (targetBlock == null || enumFacing == null || blockFace == null) {
            return;
        }

        if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
            if (!BadPacketsProcess.bad(false, true, false, false, true) &&
                    ticksOnAir > MathUtils.getRandom(placeDelay.getValue().intValue(), placeDelay.getValue().intValue() * Math.random()) &&
                    (RayCastUtils.overBlock(enumFacing.getEnumFacing(), blockFace, true))) {

                Vec3 hitVec = RayCastUtils.rayCast(RotationProcess.rotations, mc.playerController.getBlockReachDistance()).hitVec;

                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockFace, enumFacing.getEnumFacing(), hitVec)) {
                    PacketUtils.sendPacket(new C0APacketAnimation());
                }

                mc.rightClickDelayTimer = 0;
                ticksOnAir = 0;
            } else if (Math.random() > 0.92 && mc.rightClickDelayTimer <= 0) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
                mc.rightClickDelayTimer = 0;
            }
        }
    };

    public void getRotations(final int yawOffset) {

        EntityPlayer entityPlayer = mc.thePlayer;
        double difference = entityPlayer.posY + entityPlayer.getEyeHeight() - targetBlock.yCoord - 0.1 - Math.random() * 0.8;

        MovingObjectPosition movingObjectPosition;

        for (int offset = -180 + yawOffset; offset <= 180; offset += 45) {
            entityPlayer.setPosition(entityPlayer.posX, entityPlayer.posY - difference, entityPlayer.posZ);
            movingObjectPosition = RayCastUtils.rayCast(new Vector2f(entityPlayer.rotationYaw + offset, 0), 4.5);
            entityPlayer.setPosition(entityPlayer.posX, entityPlayer.posY + difference, entityPlayer.posZ);

            if (movingObjectPosition != null && new BlockPos(blockFace).equals(movingObjectPosition.getBlockPos()) &&
                    enumFacing.getEnumFacing() == movingObjectPosition.sideHit) {
                Vector2f rotations = RotationUtils.calculate(movingObjectPosition.hitVec);

                targetYaw = rotations.x;
                targetPitch = rotations.y;
                return;
            }
        }

        // Backup Rotations
        final Vector2f rotations = RotationUtils.calculate(
                new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacing.getEnumFacing());

        targetYaw = rotations.x;
        targetPitch = rotations.y;
    }
}
