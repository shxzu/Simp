package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Timer", category = ModuleCategory.CLIENT)
public class TimerModule extends Module {
    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Constant);
    public NumberProperty time1 = new NumberProperty("Time", 1, 0, 5, 0.1);
    public NumberProperty time2 = new NumberProperty("Time 2", 1, () -> mode.getValue() == Mode.Pulse, 0, 5, 0.1);

    Timer timer = new Timer();

    public enum Mode {
        Constant,
        Pulse
    }

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        setSuffix(String.valueOf(time1.getValue().intValue()));
        switch (mode.getValue()) {
            case Constant:
                mc.timer.timerSpeed = time1.getValue().floatValue();
                break;
            case Pulse:
                if (timer.getTime() < 100) {
                    mc.timer.timerSpeed = time2.getValue().floatValue();
                } else {
                    if (timer.getTime() > 200) {
                        timer.reset();
                    } else {
                        mc.timer.timerSpeed = time1.getValue().floatValue();
                    }
                }
                break;
            default:
                break;

        }
    };

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }


}
