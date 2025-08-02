package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.Timer;
import cc.simp.utils.mc.InventoryUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No-Fall Damage", category = ModuleCategory.PLAYER)
public class NoFallDamageModule extends Module {

    public EnumProperty<Mode> modeProperty = new EnumProperty<>("Mode", Mode.CLUTCH);

    private enum Mode {
        VANILLA,
        CLUTCH,
        EDIT
    }

    public NoFallDamageModule() {
        setSuffixListener(modeProperty);
    }

    public Timer timer = new Timer();
    private boolean timered = false;
    public boolean canWork = false;
    public boolean pickup = false;

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {

        if (modeProperty.getValue() == Mode.VANILLA) {
            if (mc.thePlayer.fallDistance >= 3) {
                mc.getNetHandler().sendPacket(new C03PacketPlayer(true));
            }
        }

        if (modeProperty.getValue() == Mode.CLUTCH) {
            if (mc.thePlayer.fallDistance > 2.9f) {
                int item = InventoryUtils.getBucketSlot();
                if (item == -1) {
                    item = InventoryUtils.getCobwebSlot();
                }
                if (item == -1) {
                    if (canWork && !pickup) {
                        Simp.INSTANCE.getRotationManager().resetRotationsInstantly();
                        canWork = false;
                        pickup = false;
                        return;
                    }
                } else {
                    mc.thePlayer.inventory.currentItem = item;
                    Simp.INSTANCE.getRotationManager().rotateToward(mc.thePlayer.rotationYaw, 90.0f, 60f);
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
                    Simp.INSTANCE.getRotationManager().resetRotationsInstantly();
                    canWork = false;
                    pickup = false;
                }
                if (timer.hasTimeElapsed(150.0, false)) {
                    Simp.INSTANCE.getRotationManager().resetRotationsInstantly();
                    canWork = false;
                    pickup = false;
                    return;
                }
            }
        }

        if (modeProperty.getValue() == Mode.EDIT) {
            if (mc.thePlayer.fallDistance >= 3) {
                event.setOnGround(false);
            }
        }

    };

    @EventLink
    private final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
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

    private boolean isOverVoid() {
        return mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                new Vec3(mc.thePlayer.posX, mc.thePlayer.posY - 40, mc.thePlayer.posZ), true, true, false) == null;
    }

}