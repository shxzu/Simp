package cc.simp.event.impl.packet;

import cc.simp.event.Event;

public final class DisconnectEvent implements Event {

    private final String reason;

    public DisconnectEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
