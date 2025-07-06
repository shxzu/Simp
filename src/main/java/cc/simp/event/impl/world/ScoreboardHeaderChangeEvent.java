package cc.simp.event.impl.world;

import cc.simp.event.Event;

public final class ScoreboardHeaderChangeEvent implements Event {
    private final String header;

    public ScoreboardHeaderChangeEvent(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
