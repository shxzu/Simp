package cc.simp.modules.impl.movement;

import cc.simp.event.impl.packet.PacketSendEvent;
import cc.simp.event.impl.player.BlockCollideEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.StrafeEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Flight", category = ModuleCategory.MOVEMENT)
public final class FlightModule extends Module {

    private static final EnumProperty<FlightMode> flightModeProperty = new EnumProperty<>("Mode", FlightMode.MOTION);
    public static DoubleProperty motionSpeedProperty = new DoubleProperty("Motion Speed", 0.9, () -> flightModeProperty.getValue() == FlightMode.MOTION, 0.1, 2, 0.1);
    public final Property<Boolean> fullStopProperty = new Property<>("Stop on Disable", true);
    

    private enum FlightMode {
        MOTION("Motion"),
        HYPIXEL_PREDICTION("Hypixel Prediction"),
        VERUS("Verus"),
        COLLIDE("Collide");

        public final String name;

        FlightMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public FlightModule() {
        setSuffixListener(flightModeProperty);
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        switch (flightModeProperty.getValue()) {
            case MOTION:
                MovementUtils.setSpeed(motionSpeedProperty.getValue());
                mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? 0.5
                        : mc.gameSettings.keyBindSneak.isKeyDown() ? -0.5 : 0;
                break;
            case VERUS:
                if(event.isPre()) {
                    mc.thePlayer.onGround = true;
                    MovementUtils.setSpeed(0.32);
                    event.setOnGround(mc.thePlayer.ticksExisted % 2 == 0);
                    mc.thePlayer.motionY = 0;
                    event.setPosY(Math.round(mc.thePlayer.posY));
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                            new BlockPos(mc.thePlayer.prevPosX, mc.thePlayer.posY - 1, mc.thePlayer.prevPosZ), 1,
                            new ItemStack(Blocks.stone), 1, 1, 1));
                }
                break;
            case HYPIXEL_PREDICTION:
                if(mc.thePlayer.ticksExisted % 4 == 0) {
                    mc.thePlayer.motionY = 0.42;
                    mc.timer.timerSpeed = 0.6f;
                } else {
                    mc.thePlayer.motionY = 0.0;
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
        }
       
    };

    @EventLink
    public final Listener<PacketSendEvent> packetSendEventListener = event -> {
        switch (flightModeProperty.getValue()) {
            case HYPIXEL_PREDICTION:
                if (mc.thePlayer == null || mc.theWorld == null) {
                    return;
                }
//                if (event.getPacket() instanceof C0FPacketConfirmTransaction) {
//                    List<Packet<?>> packetArrayList = new ArrayList<>();
//                    packetArrayList.add(event.getPacket());
//                    if (packetArrayList.size() > MathUtils.getRandomNumberUsingNextInt(1300, 2500)) {
//                        mc.getNetHandler().sendPacket(packetArrayList.get(MathUtils.getRandomNumberUsingNextInt(8, 100)));
//                    }
//                    event.setCancelled(true);
//                }
                break;
        }
    };

    @EventLink
    public final Listener<BlockCollideEvent> blockCollideEventListener = e -> {
        switch (flightModeProperty.getValue()) {
            case COLLIDE:
                if (e.getBlock() instanceof BlockAir && !mc.thePlayer.isSneaking()) {
                    final double x = e.getX(), y = e.getY(), z = e.getZ();

                    if (y < mc.thePlayer.posY) {
                        e.setCollisionBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                    }
                }
                break;
        }
    };

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        if(flightModeProperty.getValue() == FlightMode.HYPIXEL_PREDICTION) {
            mc.thePlayer.capabilities.isFlying = false;
        }
        if(fullStopProperty.getValue()) {
            mc.thePlayer.motionX *= 0;
            mc.thePlayer.motionZ *= 0;
        }
    }

}
