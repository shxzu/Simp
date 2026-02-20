package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.Getter;
import lombok.Setter;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Free Look", category = ModuleCategory.VISUALS)
public final class FreeLookModule extends Module {

    @Getter
    @Setter
    private float cameraYaw, cameraPitch;
    private int prevThirdPersonView;
    private boolean enableLook = false;

    @Override
    public void onDisable() {
        enableLook = false;
        mc.gameSettings.thirdPersonView = prevThirdPersonView;
        super.onDisable();
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        enableLook = false;
    };

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (mc.thePlayer != null && !enableLook) {
            prevThirdPersonView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            enableLook = true;
        }
    };

    public boolean freeLooked() {
        return enableLook;
    }
}
