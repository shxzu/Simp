package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "WTap", category = ModuleCategory.COMBAT)
public class WTapModule extends Module {

    public static final Property<Boolean> onlyWithKillaura = new Property<>("Only With Killaura", false);
    public static final Property<Integer> wtapChance = new Property<>("WTap Chance", 100);

    private boolean shouldWTap;
    private int wtapTicks;

    @EventLink
    public Listener<Render2DEvent> onRender2D = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (shouldWTap && wtapTicks > 0) {
            if (wtapTicks == 2) {
                mc.gameSettings.keyBindForward.setPressed(false);
            } else if (wtapTicks == 1) {
                mc.gameSettings.keyBindForward.setPressed(true);
                shouldWTap = false;
            }
            wtapTicks--;
        }

        if (mc.thePlayer.isSwingInProgress && mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {

            Entity target = mc.objectMouseOver.entityHit;

            if (target instanceof EntityPlayer) {
                int chance = Math.max(0, Math.min(100, wtapChance.getValue()));
                if (mc.theWorld.rand.nextInt(100) < chance) {
                    if (!onlyWithKillaura.getValue() || isKillauraActive()) {
                        triggerWTap();
                    }
                }
            }
        }
    };

    private void triggerWTap() {
        shouldWTap = true;
        wtapTicks = 2;
    }

    private boolean isKillauraActive() {
        Module killaura = getModule("Killaura");
        return killaura != null && killaura.isEnabled();
    }

    private Module getModule(String name) {
        return Simp.INSTANCE.getModuleManager().getModule(name);
    }

    @Override
    public void onEnable() {
        shouldWTap = false;
        wtapTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.keyBindForward.setPressed(true);
        }
        shouldWTap = false;
        wtapTicks = 0;
        super.onDisable();
    }
}