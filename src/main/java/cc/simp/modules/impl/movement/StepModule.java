package cc.simp.modules.impl.movement;


import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Step", category = ModuleCategory.MOVEMENT)
public class StepModule extends Module {

    public static NumberProperty stepHeight = new NumberProperty("Step Height", 1, 1, 10, 0.5);

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        setSuffix(stepHeight.getValue().toString());
        mc.thePlayer.stepHeight = stepHeight.getValue().floatValue();
    };

    public void onDisable() {
        mc.thePlayer.stepHeight = 0.5f;
        super.onDisable();
    }

}
