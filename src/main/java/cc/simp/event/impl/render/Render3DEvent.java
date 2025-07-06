package cc.simp.event.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import cc.simp.event.Event;

public final class Render3DEvent implements Event {

    private final ScaledResolution scaledResolution;
    private final float partialTicks;
    public Render3DEvent(ScaledResolution scaledResolution, float partialTicks) {
        this.scaledResolution = scaledResolution;
        this.partialTicks = partialTicks;
    }

    public ScaledResolution getScaledResolution() {
        return scaledResolution;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

}
