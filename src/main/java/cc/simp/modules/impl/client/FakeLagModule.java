package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.LagProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

@ModuleInfo(label = "Fake Lag", category = ModuleCategory.CLIENT)
public class FakeLagModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 200, 50, 2000, 5);
    private final Property<Boolean> teleports = new Property<>("Delay Teleports", false);
    private final Property<Boolean> velocity = new Property<>("Delay Velocity", false);
    private final Property<Boolean> entities = new Property<>("Delay Entity Movements", false);

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (event.isPre()) return;
        LagProcess.spoof(delay.getValue().intValue(), true, velocity.getValue(),
                teleports.getValue(), entities.getValue());
    };
}
