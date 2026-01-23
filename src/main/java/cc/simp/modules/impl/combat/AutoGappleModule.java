package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Gapple", category = ModuleCategory.COMBAT)
public final class AutoGappleModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 50, 0, 100, 5);
    private final Timer stopWatch = new Timer();

    private int attackTicks;
    private long nextEat;
    private boolean eating;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (mc.thePlayer.isPotionActive(Potion.regeneration) && eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null && !KillAuraModule.canAttack) {
                KillAuraModule.canAttack = true;
            }
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextEat) || attackTicks < 10 || Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() || mc.thePlayer.isPotionActive(Potion.regeneration)) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemAppleGold && mc.thePlayer.getHealth() <= this.health.getValue().floatValue()) {
                mc.thePlayer.inventory.currentItem = i;

                mc.playerController.syncCurrentPlayItem();
                mc.gameSettings.keyBindUseItem.setPressed(true);
                eating = true;
                if (Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null) KillAuraModule.canAttack = false;
                this.nextEat = delay.getValue().longValue() * 10;
                stopWatch.reset();
                break;
            }
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null && !KillAuraModule.canAttack)
                KillAuraModule.canAttack = true;
        }
    };

    @EventLink
    public final Listener<AttackEvent> onAttack = event -> this.attackTicks = 0;

    @Override
    public void onDisable() {
        if (eating) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            eating = false;
            if (Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null && !KillAuraModule.canAttack)
                KillAuraModule.canAttack = true;
        }
        super.onDisable();
    }
}
