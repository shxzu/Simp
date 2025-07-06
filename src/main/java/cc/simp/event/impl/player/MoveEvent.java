package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public final class MoveEvent extends CancellableEvent {

    private double x;
    private double y;
    private double z;
    private boolean pre = true;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public boolean isPre() { return pre;}

    public boolean isPost() { return !pre;}

    public void setPost() { pre = false; }

}
