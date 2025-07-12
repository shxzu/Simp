package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public final class MotionEvent extends CancellableEvent {

    public double posX;
    public double posY;
    public double posZ;
    public float yaw;
    public float pitch;
    public boolean onGround;
    private boolean pre;

    public MotionEvent(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.pre = true;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isPre() {
        return pre;
    }

    public boolean isPost() {
        if(!pre) return true;
        return false;
    }

    public void setPre() {
        this.pre = true;
    }

    public void setPost() {
        this.pre = false;
    }
}
