package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.LagProcess;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Velocity", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {

    public static final ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Edit);

    public NumberProperty horizontal = new NumberProperty("Horizontal", 100, () -> modeProperty.getValue() == Mode.Edit, 0, 100, 1);
    public NumberProperty vertical = new NumberProperty("Vertical", 100, () -> modeProperty.getValue() == Mode.Edit, 0, 100, 1);

    public NumberProperty reduceX = new NumberProperty("Reduce X", 100, () -> modeProperty.getValue() == Mode.Reduce, 0, 100, 1);
    public NumberProperty reduceZ = new NumberProperty("Reduce Z", 100, () -> modeProperty.getValue() == Mode.Reduce, 0, 100, 1);

    private final NumberProperty delay = new NumberProperty("Delay", 10, () -> modeProperty.getValue() == Mode.Delay, 1, 50, 1);
    private final Property<Boolean> legit = new Property<>("Legit Lag", true, () -> modeProperty.getValue() == Mode.Delay);

    public enum Mode {
        Legit,
        Edit,
        Grim,
        Cancel,
        Reverse,
        Reduce,
        Delay
    }

    boolean delayed = false;
    private boolean realVelocity, velocity;

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (modeProperty.getValue() == Mode.Edit) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    p.setMotionX((int) (p.getMotionX() * horizontal.getValue() / 100.0));
                    p.setMotionZ((int) (p.getMotionZ() * horizontal.getValue() / 100.0));
                    p.setMotionY((int) (p.getMotionY() * vertical.getValue() / 100.0));
                }
            }
        }

        if (modeProperty.getValue() == Mode.Reverse) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    p.setMotionX(p.getMotionX() * -1);
                    p.setMotionZ(p.getMotionZ() * -1);
                    p.setMotionY(p.getMotionY() * -1);
                }
            }
        }

        if (modeProperty.getValue() == Mode.Cancel) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                   event.setCancelled();
                }
            }
        }

        if (modeProperty.getValue() == Mode.Delay) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    delayed = true;
                }
            }
        }
    };

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        setSuffix(modeProperty.getValue().toString());
        if (modeProperty.getValue() == Mode.Legit) {
            if (mc.thePlayer.hurtTime >= 8) {
                mc.gameSettings.keyBindJump.setPressed(true);
            }
            if (mc.thePlayer.hurtTime >= 4) {
                mc.gameSettings.keyBindJump.setPressed(false);
            } else if (mc.thePlayer.hurtTime > 1) {
                mc.gameSettings.keyBindJump.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
            }
        }

        if (modeProperty.getValue() == Mode.Delay && mc.thePlayer.hurtTime > 0) {
            if (mc.thePlayer.hurtTime >= 8) {
                mc.gameSettings.keyBindJump.setPressed(true);
            }
            if (mc.thePlayer.hurtTime >= 4) {
                mc.gameSettings.keyBindJump.setPressed(false);
            } else if (mc.thePlayer.hurtTime > 1) {
                mc.gameSettings.keyBindJump.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
            }
        }

        if (modeProperty.getValue() == Mode.Delay && delayed && !event.isPre()) {
            LagProcess.spoof(delay.getValue().intValue() * 10, legit.getValue(), true, legit.getValue(), false);
            if (mc.thePlayer.hurtTime == 0) {
                LagProcess.disable();
                LagProcess.dispatch();
                delayed = false;
            }
        }
    };

    @EventLink(value = Priorities.VERY_LOW)
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (modeProperty.getValue() == Mode.Grim) {
            if (velocity && !BadPacketsProcess.bad()) {
                PacketUtils.sendSilentPacket(new C07PacketPlayerDigging((mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK),
                        new BlockPos(mc.thePlayer), EnumFacing.UP));
                velocity = false;
            }
        }
    };
    @EventLink(value = Priorities.VERY_LOW)
    public final Listener<PacketReceiveEvent> onReceiveLow = event -> {
        if(modeProperty.getValue() != Mode.Grim) return;
        final Packet<?> packet = event.getPacket();
        if (event.isCancelled()) return;

        if(packet instanceof S19PacketEntityStatus) {
            final S19PacketEntityStatus wrapper = (S19PacketEntityStatus) event.getPacket();

            if(wrapper.getEntity(mc.theWorld) != mc.thePlayer || wrapper.getOpCode() != 2) {
                return;
            }

            realVelocity = true;
        }

        if (packet instanceof S12PacketEntityVelocity && realVelocity) {
            final S12PacketEntityVelocity wrapper = (S12PacketEntityVelocity) packet;

            if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                event.setCancelled();

                realVelocity = false;
                velocity = true;
            }
        }
    };

    @EventLink
    private final Listener<AttackEvent> attackEventListener = event -> {
        if (modeProperty.getValue() == Mode.Reduce) {
            if (event.target instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
                mc.thePlayer.motionX *= reduceX.getValue() / 100.0;
                mc.thePlayer.motionZ *= reduceZ.getValue() / 100.0;
            }
        }
    };
}