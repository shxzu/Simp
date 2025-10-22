package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Logger;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Tick Base", category = ModuleCategory.COMBAT)
public final class TickBaseModule extends Module {

    private final NumberProperty lagRange = new NumberProperty("Range", 8, 1, 15, 0.1);
    private Mode MODE = Mode.NONE;
    private long time, balance;
    private double range, distance;
    Entity target;

    @EventLink
    public Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (MODE.equals(Mode.REDUCING)) {
            return;
        }

        target = KillAuraModule.target;
        if (target == null) return;

        distance = mc.thePlayer.getDistanceToEntity(target);
        double range = distance;

        if (range > 1 && balance >= 50 && MODE.equals(Mode.BASING)) {
            balance -= 50;
            mc.timer.elapsedTicks += 1;
        } else {
            if (balance != 0) {
                Logger.chatPrint("Balance " + balance + " " + range);
            }
            balance = 0;
            MODE = Mode.NONE;
        }

        if ((/*range < 7 && this.range >= 7 || range < 6 && this.range >= 6 ||*/
                range < lagRange.getValue().doubleValue() && this.range >=
                        lagRange.getValue().doubleValue()) && MODE.equals(Mode.NONE)) {
            MODE = Mode.REDUCING;
            time = System.currentTimeMillis();
            balance = 0;
        }

        this.range = range;
    };

    @EventLink
    public Listener<Render3DEvent> onRender3D = event -> {
        if (!MODE.equals(Mode.REDUCING) || target == null) return;

        if (distance <= 4 || System.currentTimeMillis() - time >= ((range / (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.36 : 0.25)) * 25) + 25) {
            mc.timer.timerSpeed = 1;
            MODE = Mode.BASING;
            balance = System.currentTimeMillis() - time;
            return;
        }

        mc.timer.timerSpeed = 0;
    };
    enum Mode {REDUCING, BASING, NONE}
}
