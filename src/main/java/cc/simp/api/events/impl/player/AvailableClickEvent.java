package cc.simp.api.events.impl.player;

import cc.simp.api.events.CancellableEvent;

public class AvailableClickEvent extends CancellableEvent {

    private boolean shouldRightClick;
    private int slot;

    public AvailableClickEvent(final int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setSlot(final int slot) {
        this.slot = slot;
    }

    public boolean isShouldRightClick() {
        return this.shouldRightClick;
    }

    public void setShouldRightClick(final boolean shouldRightClick) {
        this.shouldRightClick = shouldRightClick;
    }

}
