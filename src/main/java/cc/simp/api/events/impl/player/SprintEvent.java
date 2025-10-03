package cc.simp.api.events.impl.player;

import cc.simp.api.events.CancellableEvent;
import cc.simp.api.events.Event;

public final class SprintEvent implements Event {

    private boolean sprinting;

    public SprintEvent(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
