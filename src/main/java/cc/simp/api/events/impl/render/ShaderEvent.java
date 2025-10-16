package cc.simp.api.events.impl.render;

import cc.simp.api.events.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShaderEvent implements Event {
    private ShaderType shaderType;

    public ShaderEvent(ShaderType shaderType) {
        this.shaderType = shaderType;
    }

    public enum ShaderType {
        BLUR, SHADOW, GLOW
    }
}
