package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.managers.RotationManager;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.Representation;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Client Rotations", category = ModuleCategory.PLAYER)
public final class ClientRotationsModule extends Module {

    private float prevHeadPitch = 0f;
    private float headPitch = 0f;

    public static DoubleProperty rotSpeed = new DoubleProperty("Rotation Speed", 60.0, 0.0, 180.0, 5.0, Representation.INT);

    public ClientRotationsModule() {
        toggle();
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        this.resetState();
    };

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if(event.isPost()) {
            final RotationManager rotationManager = Simp.INSTANCE.getRotationManager();
            if (!rotationManager.isRotating()) {
                if (mc.thePlayer == null) {
                    this.resetState();
                }
                else {
                    this.prevHeadPitch = this.headPitch;
                    this.headPitch = mc.thePlayer.rotationPitch;
                }
                return;
            }
            this.prevHeadPitch = this.headPitch;
            this.headPitch = rotationManager.getClientPitch();
        }
    };

    public float getHeadPitch() {
        return this.headPitch;
    }

    public float getPrevHeadPitch() {
        return this.prevHeadPitch;
    }

    private void resetState() {
        this.prevHeadPitch = 0.0f;
        this.headPitch = 0.0f;
    }

    @Override
    public void onDisable() {
        this.resetState();
        super.onDisable();
    }

}
