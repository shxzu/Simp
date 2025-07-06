package cc.simp.event.impl.game;

import net.minecraft.client.gui.ScaledResolution;
import cc.simp.event.Event;

public final class WindowResizeEvent implements Event {

    private final ScaledResolution scaledResolution;

    public WindowResizeEvent(ScaledResolution scaledResolution) {
        this.scaledResolution = scaledResolution;
    }

    public ScaledResolution getScaledResolution() {
        return scaledResolution;
    }

}
