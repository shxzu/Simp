package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;

public class ClickEvent extends CancellableEvent {

    private boolean shouldRightClick;
    private int slot;

    public ClickEvent(final int slot) {
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
