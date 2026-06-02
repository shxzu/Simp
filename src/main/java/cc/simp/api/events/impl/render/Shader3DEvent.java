package cc.simp.api.events.impl.render;

import cc.simp.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class Shader3DEvent implements Event {
    private final float partialTicks;
}
