package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public class AttackSlowdownEvent extends CancellableEvent {
    private boolean sprint;
    private double slowDown;

    public AttackSlowdownEvent(final boolean sprint, final double slowDown) {
        this.slowDown = 0.6;
        this.sprint = sprint;
        this.slowDown = slowDown;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public void setSprint(final boolean sprint) {
        this.sprint = sprint;
    }

    public double getSlowDown() {
        return this.slowDown;
    }

    public void setSlowDown(final double slowDown) {
        this.slowDown = slowDown;
    }
}