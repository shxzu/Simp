package cc.simp.modules.impl.movement;

import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.BlockCollideEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Flight", category = ModuleCategory.MOVEMENT)
public final class FlightModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Motion);
    private final NumberProperty motionSpeed = new NumberProperty("Motion Speed", 0.9, () -> mode.getValue() == Mode.Motion, 0.1, 2.0, 0.1);
    private final NumberProperty teleportDelay = new NumberProperty("Teleport Delay", 5, () -> mode.getValue() == Mode.Packet, 1, 20, 1);
    private final NumberProperty teleportLength = new NumberProperty("Teleport Length", 5, () -> mode.getValue() == Mode.Packet, 1, 20, 1);
    private final NumberProperty timerAmount = new NumberProperty("Timer Amount", 1, 0.1, 3, 0.1);
    private final Property<Boolean> fullStop = new Property<>("Stop on Disable", true);

    private enum Mode {
        Motion("Motion"),
        Verus("Verus"),
        Collide("Collide"),
        Packet("Packet");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (!e.isPre()) return;

        mc.timer.timerSpeed = timerAmount.getValue().floatValue();

        switch (mode.getValue()) {
            case Motion:
                MovementUtils.setSpeed(motionSpeed.getValue());
                mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? 0.5
                        : mc.gameSettings.keyBindSneak.isKeyDown() ? -0.5 : 0;
                break;

            case Verus:
                mc.thePlayer.onGround = true;
                MovementUtils.setSpeed(0.32);
                e.setOnGround(mc.thePlayer.ticksExisted % 2 == 0);
                mc.thePlayer.motionY = 0;
                e.setPosY(Math.round(mc.thePlayer.posY));
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                        new BlockPos(mc.thePlayer.prevPosX, mc.thePlayer.posY - 1, mc.thePlayer.prevPosZ),
                        1, new ItemStack(Blocks.stone), 1, 1, 1));
                break;
            case Packet:
                    mc.thePlayer.motionY = 0;
                    if(MovementUtils.isMoving() && mc.thePlayer.ticksExisted % teleportDelay.getValue().intValue() == 0) {
                        final double x = e.getPosX() + -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)) * teleportLength.getValue().intValue();
                        final double z = e.getPosZ() + Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)) * teleportLength.getValue().intValue();
                        PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, mc.thePlayer.posY, z, false));
                        mc.thePlayer.setPosition(x, mc.thePlayer.posY, z);
                    }
                break;
        }
    };

    @EventLink
    public final Listener<BlockCollideEvent> blockCollideEventListener = e -> {
        if (mode.getValue() == Mode.Collide) {
            if (e.getBlock() instanceof BlockAir && !mc.thePlayer.isSneaking()) {
                final double x = e.getX(), y = e.getY(), z = e.getZ();

                if (y < mc.thePlayer.posY) {
                    e.setCollisionBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                }
            }
        }
    };

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;

        if (fullStop.getValue()) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }
    }
}