package cc.simp.api.events.impl.packet;

import cc.simp.api.events.Event;

public final class DisconnectEvent implements Event {

    private final String reason;

    public DisconnectEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
