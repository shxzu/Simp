package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.LagProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.Priorities;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Velocity", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {

    public static final ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Edit);

    private ModeProperty<LegitMode> legitMode = new ModeProperty<>("Legit Mode", LegitMode.Tick, () -> modeProperty.getValue() == Mode.Legit);
    private Property<Boolean> onlyCombat = new Property<>("Enable only during combat", true, () -> modeProperty.getValue() == Mode.Legit);
    private NumberProperty chance = new NumberProperty("Chance", 100, () -> modeProperty.getValue() == Mode.Legit, 0, 100, 1);
    private NumberProperty tickTicks = new NumberProperty("Ticks", 0, () -> modeProperty.getValue() == Mode.Legit && legitMode.getValue() == LegitMode.Tick, 0, 20, 1);
    private NumberProperty hitHits = new NumberProperty("Hits", 0, () -> modeProperty.getValue() == Mode.Legit && legitMode.getValue() == LegitMode.Hit, 0, 20, 1);

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

    public enum LegitMode {
        Tick, Hit
    }

    boolean delayed = false;
    private boolean realVelocity, velocity;
    private int limit = 0;
    private boolean reset = false;
    private int counter;

    @Override
    public void onEnable() {
        stop();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        stop();
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

        if (modeProperty.getValue() == Mode.Legit) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (legitMode.getValue() == LegitMode.Tick || legitMode.getValue() == LegitMode.Hit) {
                        double direction = Math.atan2(p.getMotionX(), p.getMotionZ());
                        double degreePlayer = MovementUtils.direction();
                        double degreePacket = Math.floorMod((int) Math.toDegrees(direction), 360);
                        double angle = Math.abs(degreePacket + degreePlayer);
                        double threshold = 120.0;
                        angle = Math.floorMod((int) angle, 360);
                        boolean inRange = angle >= 180 - threshold / 2 && angle <= 180 + threshold / 2;
                        if (inRange) {
                            reset = true;
                        }
                    }
                }
            }
        }
    };

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        setSuffix(modeProperty.getValue().toString());

        if (modeProperty.getValue() == Mode.Delay) {
            if (mc.thePlayer.hurtTime != 0 && !mc.thePlayer.isBurning()) {
                LagProcess.spoof(delay.getValue().intValue() * 10, legit.getValue(), true, legit.getValue(), false);
                delayed = true;
            }
        }

        if (modeProperty.getValue() == Mode.Delay && delayed && !event.isPre()) {
            if (mc.thePlayer.hurtTime == 0 || mc.thePlayer.isBurning()) {
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
        if (modeProperty.getValue() != Mode.Grim) return;
        final Packet<?> packet = event.getPacket();
        if (event.isCancelled()) return;

        if (packet instanceof S19PacketEntityStatus) {
            final S19PacketEntityStatus wrapper = (S19PacketEntityStatus) event.getPacket();

            if (wrapper.getEntity(mc.theWorld) != mc.thePlayer || wrapper.getOpCode() != 2) {
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

    @EventLink
    private final Listener<StrafeEvent> strafeEventListener = event -> {
        if (checkLiquids() || !applyChance())
            return;

        if (legitMode.getValue() == LegitMode.Tick || legitMode.getValue() == LegitMode.Hit && reset) {
            if (!mc.gameSettings.keyBindJump.isKeyDown() && shouldJump() && mc.thePlayer.isSprinting()
                    && mc.thePlayer.hurtTime == 9
                    || (!onlyCombat.getValue() && mc.gameSettings.keyBindAttack.isKeyDown())
                    || mc.thePlayer.onGround) {
                mc.gameSettings.keyBindJump.setPressed(true);
                limit = 0;
            }
            reset = false;
            return;
        }

        switch (legitMode.getValue()) {
            case Tick: {
                limit++;
            }
            break;

            case Hit: {
                if (mc.thePlayer.hurtTime == 9) {
                    limit++;
                }
            }
            break;
        }
    };

    private boolean shouldJump() {
        if (modeProperty.getValue() == Mode.Legit) {
            return switch (legitMode.getValue()) {
                case Tick -> {
                    double random = MathUtils.getRandom(tickTicks.getValue(), tickTicks.getValue() + 0.1);
                    yield limit >= random;
                }
                case Hit -> {
                    double random = MathUtils.getRandom(hitHits.getValue(), hitHits.getValue() + 0.1);
                    yield limit >= random;
                }
            };
        }
        return false;
    }

    private boolean checkLiquids() {
        if (modeProperty.getValue() == Mode.Legit) {
            if (mc.thePlayer == null || mc.theWorld == null) {
                return false;
            }
            return Stream.<Supplier<Boolean>>of(mc.thePlayer::isInLava, mc.thePlayer::isBurning, mc.thePlayer::isInWater,
                    () -> mc.thePlayer.isInWeb).map(Supplier::get).anyMatch(Boolean.TRUE::equals);
        }
        return false;
    }

    private void stop() {
        if (modeProperty.getValue() == Mode.Legit) {
            limit = 0;
            reset = false;
            counter = 0;
        }
    }

    private boolean applyChance() {
        if (modeProperty.getValue() == Mode.Legit) {
            Supplier<Boolean> chanceCheck = () -> {
                return chance.getValue() != 100.0D && Math.random() >= chance.getValue() / 100.0D;
            };

            return Stream.of(chanceCheck).map(Supplier::get).anyMatch(Boolean.TRUE::equals);
        }
        return false;
    }
}