package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Hit Selection", category = ModuleCategory.COMBAT)
public class HitSelectionModule extends Module {
    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Stop);
    public static ModeProperty<Type> type = new ModeProperty<>("Type", Type.Reduce);
    private static final NumberProperty chance = new NumberProperty("Chance", 80.0D, 10.0D, 100.0D, 1.0D);
    private static final NumberProperty threshold = new NumberProperty("Threshold", 400.0D, 300.0D, 500.0D, 1.0D);

    public enum Mode {
        Cancel,
        Stop
    }

    public enum Type {
        Movement,
        Reduce,
        Critical
    }

    private long lastAttackTime = -1L;
    private boolean currentShouldAttack = false;

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = e -> {
        resetState();
    };

    @EventLink
    public final Listener<AttackEvent> attackEventListener = e -> {
        if (mode.getValue() == Mode.Cancel && (!currentShouldAttack))
        {
            e.setCancelled(true);
            return;
        }
        if (mode.getValue() == Mode.Cancel || mode.getValue() == Mode.Stop) {
            if (currentShouldAttack) {
                lastAttackTime = System.currentTimeMillis();
            }
        }
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (mc.thePlayer == null) return;

        currentShouldAttack = false;

        if (Math.random() * 100 > chance.getValue()) {
            currentShouldAttack = true;
        } else {
            switch (type.getValue()) {
                case Movement:
                    double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                    double dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                    double speed = Math.sqrt(dx * dx + dz * dz);
                    currentShouldAttack = speed > 0.1;
                    break;

                case Reduce:
                    currentShouldAttack = mc.thePlayer.hurtTime > 0 && !mc.thePlayer.onGround;
                    break;

                case Critical:
                    currentShouldAttack = !mc.thePlayer.onGround && mc.thePlayer.motionY < 0;
                    break;
            }

            if (!currentShouldAttack) {
                currentShouldAttack = System.currentTimeMillis() - lastAttackTime >= threshold.getValue();
            }
        }

        if (mode.getValue() == Mode.Stop) {
            KillAuraModule.canAttack = currentShouldAttack;
        }
    };

    private void resetState()
    {
        lastAttackTime = -1L;
        currentShouldAttack = false;
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

}

