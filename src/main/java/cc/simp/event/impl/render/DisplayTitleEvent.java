package cc.simp.event.impl.render;

import cc.simp.event.CancellableEvent;

public final class DisplayTitleEvent extends CancellableEvent {

    private final String title;

    public DisplayTitleEvent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
