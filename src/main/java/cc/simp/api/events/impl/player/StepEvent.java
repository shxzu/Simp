package cc.simp.api.events.impl.player;

import cc.simp.api.events.Event;

public final class StepEvent implements Event {

    private float stepHeight;
    private double heightStepped;
    private boolean pre;

    public StepEvent(float stepHeight) {
        this.stepHeight = stepHeight;
        pre = true;
    }

    public double getHeightStepped() {
        return heightStepped;
    }

    public void setHeightStepped(double heightStepped) {
        this.heightStepped = heightStepped;
    }

    public boolean isPre() {
        return pre;
    }

    public void setPost() {
        pre = false;
    }

    public float getStepHeight() {
        return stepHeight;
    }

    public void setStepHeight(float stepHeight) {
        this.stepHeight = stepHeight;
    }

}
