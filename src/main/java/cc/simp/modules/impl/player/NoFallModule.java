package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.InventoryUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Fall", category = ModuleCategory.PLAYER)
public class NoFallModule extends Module {

    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Clutch);

    private enum Mode {
        Vanilla,
        Vulcan,
        Clutch,
        Edit,
        Round
    }

    public Timer timer = new Timer();
    private boolean timered = false;
    public boolean canWork = false;
    public boolean pickup = false;

    @EventLink
    private final Listener<MotionEvent> motionEventListener = e -> {
        setSuffix(mode.getValue().toString());
        if (mode.getValue() == Mode.Vanilla) {
            if (mc.thePlayer.fallDistance >= 3) {
                PacketUtils.sendPacket(new C03PacketPlayer(true));
            }
        }
        if (mode.getValue() == Mode.Edit) {
            e.setOnGround(false);
            e.setPosY(e.getPosY() + Math.random() / 100000000000000000000f);
        }
        if (mode.getValue() == Mode.Round) {
            if (mc.thePlayer.fallDistance >= 3) {
                e.setPosY(e.getPosY() * Math.round(e.getPosY()));
            }
        }
    };

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (mode.getValue() == Mode.Clutch) {
            if (mc.thePlayer.fallDistance > 2.9f) {
                int item = InventoryUtils.getBucketSlot();
                if (item == -1) {
                    item = InventoryUtils.getCobwebSlot();
                }
                if (item == -1) {
                    if (canWork && !pickup) {
                        RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), 10, MovementFix.NORMAL);
                        canWork = false;
                        pickup = false;
                        return;
                    }
                } else {
                    mc.thePlayer.inventory.currentItem = item;
                    RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw, 90.0f), 10, MovementFix.NORMAL);
                    canWork = true;
                    if (!mc.thePlayer.isInWater() && !mc.thePlayer.isInWeb && !pickup && mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 2.0, mc.thePlayer.posZ)).getBlock() != Blocks.water && mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 2.0, mc.thePlayer.posZ)).getBlock() != Blocks.air) {
                        mc.rightClickMouse();
                        pickup = true;
                        timer.reset();
                    }
                }
            } else {
                if (!canWork) {
                    return;
                }
                if (mc.thePlayer.isInWater() && pickup) {
                    mc.rightClickMouse();
                    pickup = false;
                } else {
                    RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), 10, MovementFix.NORMAL);
                    canWork = false;
                    pickup = false;
                }
                if (timer.hasTimeElapsed(150.0, false)) {
                    RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), 10, MovementFix.NORMAL);
                    canWork = false;
                    pickup = false;
                    return;
                }
            }
        }
    };

    @EventLink
    private final Listener<PacketSendEvent> packetSendEventListener = e -> {
        if (mode.getValue() == Mode.Vulcan) {
            if (e.getPacket() instanceof C03PacketPlayer && mc.thePlayer.fallDistance > 3) {
                C03PacketPlayer packet = (C03PacketPlayer) e.getPacket();
                packet.onGround = true;
                mc.thePlayer.fallDistance = 0;
                mc.thePlayer.setVelocity(0, 0, 0);
            }
        }
    };

    @EventLink
    private final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        if (timered) {
            timered = false;
            mc.timer.timerSpeed = 1;
        }
        if(pickup || canWork) {
            pickup = false;
            canWork = false;
        }
    };

    @Override
    public void onDisable() {
        if (timered) {
            timered = false;
            mc.timer.timerSpeed = 1;
        }
        super.onDisable();
    }
}