package cc.simp.handlers;

import cc.simp.event.impl.player.MotionEvent;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

public class RotationHandler {
    private float serverYaw, serverPitch;

    public float getServerYaw() {
        return serverYaw;
    }

    public float getServerPitch() {
        return serverPitch;
    }

    public float getPrevServerYaw() {
        return prevServerYaw;
    }

    public float getPrevServerPitch() {
        return prevServerPitch;
    }

    public float getRealYaw() {
        return realYaw;
    }

    public float getRealPitch() {
        return realPitch;
    }

    private float prevServerYaw, prevServerPitch;
    private float realYaw, realPitch;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        serverYaw = event.getYaw();
        serverPitch = event.getPitch();
        prevServerYaw = event.getPrevYaw();
        prevServerPitch = event.getPrevPitch();
        realYaw = mc.thePlayer.rotationYaw;
        realPitch = mc.thePlayer.rotationPitch;
    };

}
