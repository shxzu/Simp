package cc.simp.event.impl.world;

import cc.simp.event.Event;

public final class ScoreboardModeChangeEvent implements Event {

    private final String mode;

    public ScoreboardModeChangeEvent(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

}
