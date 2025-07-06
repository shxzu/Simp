package cc.simp.event.impl.render.model;

import cc.simp.event.Event;

public final class ModelRenderEvent implements Event {

    private ModelRenderState state;

    public ModelRenderEvent(ModelRenderState state) {
        this.state = state;
    }

    public ModelRenderState getState() {
        return state;
    }

    public void setState(ModelRenderState state) {
        this.state = state;
    }

}
