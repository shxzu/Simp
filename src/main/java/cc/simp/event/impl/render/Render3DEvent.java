package cc.simp.event.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import cc.simp.event.Event;

public final class Render3DEvent implements Event {

    private final float partialTicks;

    public Render3DEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

}
