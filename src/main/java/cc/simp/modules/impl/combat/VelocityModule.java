package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.LagProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.misc.MovementFix;
import com.sun.jdi.BooleanValue;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Velocity", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {

    public static final ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Edit);

    public final NumberProperty chance = new NumberProperty("Chance", 100, () -> modeProperty.getValue() == Mode.Legit, 0, 100, 1);
    public final Property<Boolean> legitTiming = new Property<Boolean>("Legit Timing",  false, () -> modeProperty.getValue() == Mode.Legit);

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
    private boolean velocity;
    private boolean jump;

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

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

        if (modeProperty.getValue() == Mode.Legit) {
            if (mc.thePlayer == null) {
                return;
            }

            if (!mc.thePlayer.onGround) {
                return;
            }

            final Packet<?> p = event.getPacket();

            if (p instanceof S12PacketEntityVelocity) {
                final S12PacketEntityVelocity wrapper = (S12PacketEntityVelocity) p;

                if (wrapper.getEntityID() == mc.thePlayer.getEntityId() && wrapper.getMotionY() > 0 && (!legitTiming.getValue() || mc.thePlayer.ticksSinceVelocity <= 14 || mc.thePlayer.onGroundTicks <= 1)) {
                    jump = true;
                }
            }
        }
    };

    @EventLink
    public final Listener<MoveEvent> onMove = event -> {
        if (jump && MovementUtils.isMoving() && Math.random() * 100 < chance.getValue().doubleValue()) {
            event.setJump(true);
        }
    };


    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        setSuffix(modeProperty.getValue().toString());

        if (event.isPre() && modeProperty.getValue() == Mode.Legit) {
            jump = false;
        }

        if (!event.isPre() && modeProperty.getValue() == Mode.Delay) {
            if (mc.thePlayer.hurtTime > 0 && !mc.thePlayer.isBurning()) {
                LagProcess.spoof(delay.getValue().intValue() * 10, legit.getValue(), true, legit.getValue(), false);
                delayed = true;
            }
        }

        if (modeProperty.getValue() == Mode.Delay && delayed && !event.isPre()) {
            if (mc.thePlayer.hurtTime == 0 || mc.thePlayer.isBurning()) {
                LagProcess.dispatch();
                LagProcess.disable();
                delayed = false;
            }
        }
    };

    @EventLink(value = Priorities.VERY_HIGH)
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (modeProperty.getValue() == Mode.Grim) {
            if (velocity) {
                RotationProcess.setRotations(new Vector2f(mc.thePlayer.rotationYaw, 90), 10, MovementFix.NORMAL);
                if (mc.objectMouseOver != null) {
                    mc.clickMouse();
                }
                velocity = false;
            }
        }
    };
    @EventLink(value = Priorities.VERY_LOW)
    public final Listener<PacketReceiveEvent> onReceiveLow = event -> {
        if (modeProperty.getValue() != Mode.Grim) return;
        if (event.getPacket() instanceof S12PacketEntityVelocity wrapper) {
            if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                event.setCancelled(true);
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