package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Trigger Bot", category = ModuleCategory.COMBAT)
public class TriggerBotModule extends Module {

    static long delay = 0;
    private static final Timer attackTimer = new Timer();
    static int elapsedTicks = 0;

    public static final Property<Boolean> newCombat = new Property<>("New Combat Delays", false);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, () -> !newCombat.getValue(), 0.0, 20.0, 0.5);

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.ENTITY) {
            attack();
        }
    };

    @EventLink
    public final Listener<TickEvent> tickEventListener = e -> {
        elapsedTicks++;
    };

    private void attack() {
        if (!hitTimerDone()) return;

        mc.clickMouse();
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (!newCombat.getValue()) {
            if (attackTimer.hasTimeElapsed(delay, false)) {
                returnVal = true;
                attackTimer.reset();
                delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.min(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
            }
        } else {
            if (elapsedTicks >= getNewCombatDelay()) {
                elapsedTicks = 0;
                returnVal = true;
            }
        }
        return returnVal;
    }

    private static int getNewCombatDelay() {
        int toolDelay = 3;
        if (mc.thePlayer.inventory.getCurrentItem() == null) {
            return toolDelay;
        } else {
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
                toolDelay = 12;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemTool) {
                toolDelay = 20;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemPickaxe) {
                toolDelay = 16;
            }
            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemAxe) {
                toolDelay = 25;
            }
        }
        return toolDelay;
    }

    @Override
    public void onEnable() {
        delay = (long) (1000 / MathUtils.getRandom(max.getValue().floatValue(), Math.max(min.getValue().floatValue(), max.getValue().floatValue() - 1)));
        super.onEnable();
    }
}