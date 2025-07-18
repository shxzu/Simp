package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.event.impl.player.AttackSlowdownEvent;
import cc.simp.event.impl.player.ClickEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.property.impl.Representation;
import cc.simp.utils.Timer;
import cc.simp.utils.mc.MovementUtils;
import cc.simp.utils.mc.PlayerUtils;
import cc.simp.utils.mc.RaytraceUtils;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.NonNull;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT)
public final class KillAuraModule extends Module {

    private final DoubleProperty minAttackDelayProperty = new DoubleProperty("Min Attack Delay", 40.0, 0.0, 400.0, 1);
    private final DoubleProperty maxAttackDelayProperty = new DoubleProperty("Max Attack Delay", 40.0, 0.0, 400.0, 1);
    public static DoubleProperty attackRangeProperty = new DoubleProperty("Attack Range", 3, 3, 6, 0.1);
    public static DoubleProperty rotationSpeedProperty = new DoubleProperty("Rotation Speed", 60.0, 0.0, 180.0, 5.0);
    private final EnumProperty<TargetType> targetTypeProperty = new EnumProperty<>("Targets Type", TargetType.ADAPTIVE);
    private final Property<Boolean> teamCheckProperty = new Property<>("Team Check", true);
    public static EnumProperty<AutoBlockType> autoBlockTypeProperty = new EnumProperty<>("Auto Block", AutoBlockType.FAKE);
    private final Property<Boolean> legitTimingsProperty = new Property<>("Legit Timings", true);
    public static final Property<Boolean> keepSprintProperty = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> advancedSettingsProperty = new Property<>("Advanced Settings", false);
    public static final Property<Boolean> sprintResetProperty = new Property<>("Reset Sprint", false, advancedSettingsProperty::getValue);
    public static DoubleProperty succeededHitsRateProperty = new DoubleProperty("Succeeded Hits Rate", 100, advancedSettingsProperty::getValue, 0, 100, 1);
    public static final Property<Boolean> noiseProperty = new Property<>("Noise", false, advancedSettingsProperty::getValue);
    public static DoubleProperty noiseValueProperty = new DoubleProperty("Noise Value", 2, () -> advancedSettingsProperty.getValue() && noiseProperty.getValue(), 0, 5, 0.5);
    public static final Property<Boolean> hitSelectProperty = new Property<>("Hit Select", false, advancedSettingsProperty::getValue);
    public static DoubleProperty hitSelectHurtTicksProperty = new DoubleProperty("Hit Select Hurt Ticks", 5, () -> advancedSettingsProperty.getValue() && hitSelectProperty.getValue(), 0, 10, 1);

    public enum TargetType {
        ADAPTIVE,
        PLAYERS,
        ALL
    }

    public enum AutoBlockType {
        NONE,
        FAKE,
        VANILLA
    }

    public KillAuraModule() {
        setSuffixListener(targetTypeProperty);
    }

    public static EntityLivingBase target;
    private long randomDelay = 100L;
    private boolean rotated = false;
    private Timer hitTimeHelper = new Timer();
    public static boolean autoBlocking = false;
    private static boolean canAttack;
    private static boolean activated = false;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {

        if (target == null) {
            if(canAttack) canAttack = false;
            if(activated) activated = false;
        }

        if (target == null && autoBlocking) {
            autoBlocking = false;
        }

        updateTarget();

        if (!targetIsValid()) {
            target = null;
            return;
        }

        if (event.isPre()) {
            if (!noiseProperty.getValue() && !advancedSettingsProperty.getValue()) {
                Simp.INSTANCE.getRotationManager().faceEntity(target, rotationSpeedProperty.getValue().floatValue());
            } else {
                Simp.INSTANCE.getRotationManager().faceEntity(target, rotationSpeedProperty.getValue().floatValue(), noiseValueProperty.getValue().floatValue());
            }
            rotated = true;

            if (!legitTimingsProperty.getValue() && target != null) {
                this.autoBlock();
                this.attack();
            }

            if (sprintResetProperty.getValue()) {
                if (mc.thePlayer.hurtTime >= 7) {
                    mc.gameSettings.keyBindForward.setPressed(true);
                } else if (mc.thePlayer.hurtTime >= 4) {
                    mc.gameSettings.keyBindForward.setPressed(false);
                } else if (mc.thePlayer.hurtTime > 1) {
                    mc.gameSettings.keyBindForward.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindForward));
                }
            }
        }
    };

    @EventLink
    public final Listener<ClickEvent> clickEventListener = event -> {
        if (legitTimingsProperty.getValue() && target != null) {

            if (!Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() && mc.currentScreen == null && rotated) {
                event.setCancelled(true);
            }

            this.autoBlock();
            this.legitAttack();
        }
    };

    @EventLink
    public final Listener<AttackSlowdownEvent> attackSlowdownEventListener = event -> {
        if (keepSprintProperty.getValue()) {
            event.setSprint(true);
            event.setSlowDown(1.0);
        }
    };


    private void updateTarget() {
        target = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase) entity)
                .filter(this::isValidTarget)
                .min(Comparator.comparingDouble(entity ->
                        mc.thePlayer.getDistanceToEntity(entity)))
                .orElse(null);
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity.isDead ||
                entity.getDistanceToEntity(mc.thePlayer) > attackRangeProperty.getValue())
            return false;

        if (teamCheckProperty.getValue() && inTeam(mc.thePlayer, entity)) return false;

        switch (targetTypeProperty.getValue()) {
            case ADAPTIVE:
                return entity instanceof EntityPlayer || entity instanceof EntityMob;
            case PLAYERS:
                return entity instanceof EntityPlayer;
            case ALL:
                return true;
            default:
                return false;
        }
    }

    private boolean targetIsValid() {
        return target != null && !target.isDead &&
                target.getDistanceToEntity(mc.thePlayer) <= attackRangeProperty.getValue();
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

    private boolean hitChance(final int hitChance) {
        final int randomNumber = ThreadLocalRandom.current().nextInt(0, 99);
        return randomNumber <= hitChance;
    }

    private void setRandomDelay() {
        if (this.minAttackDelayProperty.getValue() == 0.0 && this.maxAttackDelayProperty.getValue() == 0.0) {
            this.randomDelay = 0L;
        } else if (Math.abs(this.minAttackDelayProperty.getValue() - this.maxAttackDelayProperty.getValue()) > 0.0) {
            this.randomDelay = (long) MathUtils.nextSecureInt(this.minAttackDelayProperty.getValue().intValue(), this.maxAttackDelayProperty.getValue().intValue());
        } else {
            this.randomDelay = this.minAttackDelayProperty.getValue().longValue();
        }
    }

    private void attack() {
        if (legitTimingsProperty.getValue()) return;
        if (!this.hitChance(succeededHitsRateProperty.getValue().intValue()) && advancedSettingsProperty.getValue()) return;
        if (hitSelectProperty.getValue() && !hasTargetAttacked()) return;
        if (this.hitTimeHelper.hasTimeElapsed(this.randomDelay)) {
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
        }

        this.setRandomDelay();
        this.hitTimeHelper.reset();
    }

    private void legitAttack() {
        if (!legitTimingsProperty.getValue()) return;
        if (!this.hitChance(succeededHitsRateProperty.getValue().intValue()) && advancedSettingsProperty.getValue()) return;
        if (hitSelectProperty.getValue() && !hasTargetAttacked()) return;

        if (this.hitTimeHelper.hasTimeElapsed(this.randomDelay)) {
            mc.clickMouse();
        }

        this.setRandomDelay();
        this.hitTimeHelper.reset();
    }

    public void autoBlock() {

        if (target == null || !PlayerUtils.isHoldingSword()) {
            autoBlocking = false;
            return;
        }

        switch (autoBlockTypeProperty.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case VANILLA:
                mc.thePlayer.setItemInUse(mc.thePlayer.inventory.getCurrentItem(), 32678);
                autoBlocking = true;
                break;
        }

    }

    private static boolean hasTargetAttacked() {
        if (target instanceof EntityPlayer) {
            if (mc.thePlayer.hurtTime == hitSelectHurtTicksProperty.getValue().intValue() && !activated) {
                canAttack = true;
                activated = true;
            } else {
                canAttack = false;
            }
        } else {
            activated = false;
            canAttack = true;
        }

        if (activated && mc.thePlayer.hurtTime != hitSelectHurtTicksProperty.getValue().intValue()) {
            canAttack = true;
        }
        return canAttack;
    }

    @Override
    public void onDisable() {
        canAttack = true;
        hitTimeHelper.reset();
        if (autoBlocking) {
            autoBlocking = false;
            mc.thePlayer.clearItemInUse();
        }
        rotated = false;
        target = null;
    }
}