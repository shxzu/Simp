package cc.simp.event.impl.render;

import cc.simp.event.Event;
import net.minecraft.client.gui.ScaledResolution;

import static cc.simp.utils.Util.mc;

public final class Render2DEvent implements Event {

    private ScaledResolution resolution = new ScaledResolution(mc);
    private final float partialTicks;

    public Render2DEvent(ScaledResolution resolution, float partialTicks) {
        this.resolution = resolution;
        this.partialTicks = partialTicks;
    }

    public ScaledResolution getResolution() {
        return resolution;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

}
