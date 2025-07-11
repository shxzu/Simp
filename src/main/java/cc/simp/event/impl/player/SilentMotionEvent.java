package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public class SilentMotionEvent extends CancellableEvent {

    private boolean silent;
    private float yaw;
    private boolean advanced;

    public SilentMotionEvent(final float yaw) {
        this.yaw = yaw;
    }

    public boolean isSilent() {
        return this.silent;
    }

    public void setSilent(final boolean silent) {
        this.silent = silent;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(final float yaw) {
        this.yaw = yaw;
    }

    public boolean isAdvanced() {
        return this.advanced;
    }

    public void setAdvanced(final boolean advanced) {
        this.advanced = advanced;
    }

}
