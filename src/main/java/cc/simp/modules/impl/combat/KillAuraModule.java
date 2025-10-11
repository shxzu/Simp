package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.HitSlowDownEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.NonNull;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector2f;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Adaptive);
    public static ModeProperty<Entities> entities = new ModeProperty<>("Entities", Entities.Optimal);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 4.2, 3, 6, 0.1);
    public static NumberProperty killRange = new NumberProperty("Kill Range", 3, 3, 6, 0.1);
    public static NumberProperty blockingRange = new NumberProperty("Blocking Range", 4.2, 3, 6, 0.1);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, 0.0, 20.0, 0.5);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.Fake);
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Regular);
    private final NumberProperty speed = new NumberProperty("Rotation Speed", 5, 0, 10, 1);
    public static final Property<Boolean> jitter = new Property<>("Jitter Rotations", false);
    public static final Property<Boolean> fix = new Property<>("Move Fix", true);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> legit = new Property<>("Legit", true);
    public static final Property<Boolean> raycast = new Property<>("Ray Cast", true);
    private final Property<Boolean> teams = new Property<>("Teams", false);

    public enum Mode {
        Adaptive,
        Single,
        Switch
    }

    public enum Entities {
        Optimal,
        Players,
        All
    }

    public enum Rotations {
        Regular,
        Snap,
        None
    }

    public enum AutoBlock {
        None,
        Fake,
        Switch,
        Legit,
        Predictive,
        Vanilla
    }

    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    private List<Entity> targetList = new CopyOnWriteArrayList<>();
    private static final Timer attackTimer = new Timer();
    private static final Timer switchTimer = new Timer();
    private int targetIndex;
    static long delay = 0;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        setSuffix(mode.getValue().toString());

        targetList = getTargets();

        if (targetList.isEmpty()) {
            target = null;
            unblock();
            return;
        }

        selectTarget();

        if (target == null) {
            unblock();
            return;
        }

        calculateRotations();
        attack();
    };

    @EventLink
    public final Listener<HitSlowDownEvent> hitSlowDownEventListener = e -> {
        if (sprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        if (autoBlocking) {
            if (ab.getValue() == AutoBlock.Legit || ab.getValue() == AutoBlock.Predictive) {
                mc.gameSettings.keyBindUseItem.setPressed(false);
            } else if (InventoryUtils.isHoldingSword()) {
                PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
            autoBlocking = false;
        }
        canAttack = true;
        target = null;
        targetList.clear();
    };

    private void selectTarget() {
        switch (mode.getValue()) {
            case Single:
                target = (EntityLivingBase) targetList.stream().findFirst().orElse(null);
                break;

            case Switch:
                if (switchTimer.hasTimeElapsed(1000)) {
                    targetIndex = (targetIndex + 1) % targetList.size();
                    switchTimer.reset();
                }
                target = (EntityLivingBase) targetList.get(targetIndex);
                break;

            case Adaptive:
                target = (EntityLivingBase) targetList.stream()
                        .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                        .orElse(null);
                break;
        }
    }

    private void calculateRotations() {
        if (target == null || rotations.getValue() == Rotations.None) return;

        Vector2f rotation = RotationUtils.calculate(target, mode.getValue() == Mode.Adaptive, seekRange.getValue());

        if (jitter.getValue()) {
            rotation.x += (float) ((Math.random() - 0.5) * 2);
            rotation.y += (float) ((Math.random() - 0.5) * 2);
        }

        float targetYaw = rotation.x;
        float targetPitch = rotation.y;

        switch (rotations.getValue()) {
            case Regular:
                /* Smoothing rotations */
                final double minRotationSpeed = this.speed.getValue();
                final double maxRotationSpeed = this.speed.getValue() * Math.random();
                float rotSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;

            case Snap:
                RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), 10, fix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
                break;
        }
    }

    private void autoblock() {
        if (target == null || !InventoryUtils.isHoldingSword()) {
            autoBlocking = false;
            return;
        }

        switch (ab.getValue()) {
            case Legit:
                if (mc.thePlayer.ticksExisted % 4 == 0) {
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                    autoBlocking = true;
                } else {
                    mc.gameSettings.keyBindUseItem.setPressed(false);
                    autoBlocking = false;
                }
                canAttack = !autoBlocking;
                break;
            case Predictive:
                if (mc.thePlayer.hurtTime >= 4 && mc.thePlayer.hurtTime != 10) {
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                    autoBlocking = true;
                } else {
                    mc.gameSettings.keyBindUseItem.setPressed(false);
                    autoBlocking = false;
                }
                canAttack = !autoBlocking;
                break;
            case Vanilla:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case Switch:
                if (mc.thePlayer.ticksExisted % 4 == 0) {
                    PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    autoBlocking = true;
                    PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 8));
                    if (mc.thePlayer.isBlocking()) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem)));
                        autoBlocking = false;
                    }
                }
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) return;

        if (ab.getValue() == AutoBlock.Legit || ab.getValue() == AutoBlock.Predictive) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
        } else if (InventoryUtils.isHoldingSword()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        autoBlocking = false;
    }

    private void attack() {
        if (target == null || !canAttack) return;

        if (!hitTimerDone()) return;

        if (mc.thePlayer.getDistanceToEntity(target) > killRange.getValue()) return;

        if (raycast.getValue()) {
            MovingObjectPosition mop = RayCastUtils.rayCast(RotationProcess.rotations, killRange.getValue());
            if (mop == null || mop.entityHit != target) return;
        }

        if (target.getDistanceToEntity(mc.thePlayer) > killRange.getValue()) return;
        if (!legit.getValue()) {
            if (!canAttack) return;
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
        } else {
            mc.clickMouse();
        }
        if (ab.getValue() != AutoBlock.None) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockingRange.getValue() && InventoryUtils.isHoldingSword()) {
                autoblock();
            }
        }
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (attackTimer.hasTimeElapsed(delay, false)) {
            returnVal = true;
            attackTimer.reset();
            delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.min(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
        }
        return returnVal;
    }

    private List<Entity> getTargets() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> !entity.isDead)
                .filter(entity -> ((EntityLivingBase) entity).getHealth() > 0)
                .filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= seekRange.getValue())
                .filter(this::isValidEntity)
                .collect(Collectors.toList());
    }

    private boolean isValidEntity(Entity entity) {
        if (teams.getValue() && inTeam(mc.thePlayer, entity)) return false;

        return switch (entities.getValue()) {
            case Optimal -> entity instanceof EntityPlayer || entity instanceof EntityMob;
            case Players -> entity instanceof EntityPlayer;
            case All -> true;
        };
    }

    public static boolean inTeam(@NonNull ICommandSender entity0, @NonNull ICommandSender entity1) {
        String s = "\u00a7" + teamColor(entity0);

        return entity0.getDisplayName().getFormattedText().contains(s)
                && entity1.getDisplayName().getFormattedText().contains(s);
    }

    public static @NonNull String teamColor(@NonNull ICommandSender player) {
        Matcher matcher = Pattern.compile("\u00a7(.).*\u00a7r").matcher(player.getDisplayName().getFormattedText());
        return matcher.find() ? matcher.group(1) : "f";
    }

    @Override
    public void onEnable() {
        delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.max(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
        super.onEnable();
    }

    @Override
    public void onDisable() {
        canAttack = true;
        target = null;
        targetList.clear();
        unblock();
        super.onDisable();
    }
}
