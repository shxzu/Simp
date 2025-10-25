package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.util.MovingObjectPosition;

import java.util.concurrent.ThreadLocalRandom;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Clicker", category = ModuleCategory.COMBAT)
public final class AutoClickerModule extends Module {

    private final Property<Boolean> left = new Property<>("Left Click", true);
    private final NumberProperty lminCPS = new NumberProperty("Left Min CPS", 10.0, () -> left.getValue(), 1.0, 20.0, 1.0);
    private final NumberProperty lmaxCPS = new NumberProperty("Left Max CPS", 12.0, () -> left.getValue(), 1.0, 20.0, 1.0);
    private final Property<Boolean> breakBlocks = new Property<>("Break Blocks", true, () -> left.getValue());

    private final Property<Boolean> right = new Property<>("Right Click", true);
    private final NumberProperty rminCPS = new NumberProperty("Right Min CPS", 10.0, () -> right.getValue(), 1.0, 20.0, 1.0);
    private final NumberProperty rmaxCPS = new NumberProperty("Right Max CPS", 12.0, () -> right.getValue(), 1.0, 20.0, 1.0);

    private long leftLastClick = 0;
    private long rightLastClick = 0;

    @EventLink
    public final Listener<MoveEvent> moveEventListener = e -> {
            handleRightClick();
            handleLeftClick();
    };

    private void handleRightClick() {
        if (right.getValue() && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            long currentTime = System.currentTimeMillis();
            int minCPS = rminCPS.getValue().intValue();
            int maxCPS = rmaxCPS.getValue().intValue();
            int cps = ThreadLocalRandom.current().nextInt(minCPS, maxCPS + 1);
            long delay = 1000 / cps;

            if (currentTime - rightLastClick >= delay) {
                mc.rightClickMouse();
                rightLastClick = currentTime;
            }
        }
    }

    private void handleLeftClick() {
        if (left.getValue() && mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (breakBlocks.getValue() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            int minCPS = lminCPS.getValue().intValue();
            int maxCPS = lmaxCPS.getValue().intValue();
            int cps = ThreadLocalRandom.current().nextInt(minCPS, maxCPS + 1);
            long delay = 1000 / cps;

            if (currentTime - leftLastClick >= delay) {
                mc.clickMouse();
                leftLastClick = currentTime;
            }
        }
    }

    @Override
    public void onEnable() {
        leftLastClick = 0;
        rightLastClick = 0;
    }
}