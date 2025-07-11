package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public class JumpEvent extends CancellableEvent {

    private float yaw;
    private double motionY;

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(final float yaw) {
        this.yaw = yaw;
    }

    public JumpEvent(final float yaw, final double motionY) {
        this.yaw = yaw;
        this.motionY = motionY;
    }

    public double getMotionY() {
        return this.motionY;
    }

    public void setMotionY(final double motionY) {
        this.motionY = motionY;
    }

}
