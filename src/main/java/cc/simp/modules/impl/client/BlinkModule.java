package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.LagProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

@ModuleInfo(label = "Blink", category = ModuleCategory.CLIENT)
public class BlinkModule extends Module {

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        LagProcess.blink();
    };

    @Override
    public void onDisable() {
        LagProcess.dispatch();
        LagProcess.disable();
        super.onDisable();
    }

}
