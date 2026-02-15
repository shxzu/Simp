package cc.simp.processes;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

public class TargetSelectionProcess {

    // made by yours truly -shxzu

    @Getter
    @Setter
    private static EntityLivingBase target;
    @Getter
    private static List<Entity> targetList = new CopyOnWriteArrayList<>();
    private static final Timer switchTimer = new Timer();
    @Getter
    @Setter
    private static Enum mode;
    @Getter
    @Setter
    private static Enum entities;
    @Getter
    @Setter
    private static boolean dontTargetTeams;
    @Getter
    @Setter
    private static float seekRange;
    @Getter
    @Setter
    private static int switchTime;
    private int targetIndex;

    public TargetSelectionProcess() {
        mode = Mode.Adaptive;
        entities = Entities.Optimal;
        dontTargetTeams = false;
        seekRange = 4.2f;
        switchTime = 2;
    }

    public enum Mode {
        Adaptive,
        Switch,
        Single
    }

    public enum Entities {
        Optimal,
        Players,
        All
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        targetList = getTargets();

        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        selectTarget();
    };

    @EventLink
    public final Listener<AttackEvent> attackEventListener = event -> event.target = target;

    private void selectTarget() {
        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        switch (mode) {
            case Mode.Single:
                target = (EntityLivingBase) targetList.getFirst();
                break;

            case Mode.Switch:
                if (targetIndex >= targetList.size()) {
                    targetIndex = 0;
                }

                if (switchTimer.hasTimeElapsed(switchTime * 100)) {
                    targetIndex = (targetIndex + 1) % targetList.size();
                    switchTimer.reset();
                }
                target = (EntityLivingBase) targetList.get(targetIndex);
                break;

            case Mode.Adaptive:
                target = (EntityLivingBase) targetList.stream()
                        .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                        .orElse(null);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.entities);
        }
    }

    private List<Entity> getTargets() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> !entity.isDead)
                .filter(entity -> ((EntityLivingBase) entity).getHealth() > 0)
                .filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= seekRange)
                .filter(entity -> !AntiBotModule.botList.contains(entity))
                .filter(entity -> !cc.simp.modules.impl.client.MCFModule.excludedPlayers.contains(entity))
                .filter(this::isValidEntity)
                .collect(Collectors.toList());
    }

    private boolean isValidEntity(Entity entity) {
        if (dontTargetTeams && inTeam(mc.thePlayer, entity)) return false;

        return switch (entities) {
            case Entities.Optimal -> entity instanceof EntityPlayer || entity instanceof EntityMob;
            case Entities.Players -> entity instanceof EntityPlayer;
            case Entities.All -> true;
            default -> throw new IllegalStateException("Unexpected value: " + entities);
        };
    }

    private static boolean inTeam(@NonNull ICommandSender entity0, @NonNull ICommandSender entity1) {
        String s = "\u00a7" + teamColor(entity0);

        return entity0.getDisplayName().getFormattedText().contains(s)
                && entity1.getDisplayName().getFormattedText().contains(s);
    }

    private static @NonNull String teamColor(@NonNull ICommandSender player) {
        Matcher matcher = Pattern.compile("\u00a7(.).*\u00a7r").matcher(player.getDisplayName().getFormattedText());
        return matcher.find() ? matcher.group(1) : "f";
    }

}
