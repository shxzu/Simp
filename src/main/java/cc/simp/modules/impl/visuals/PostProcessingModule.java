package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.shaders.Bloom;
import cc.simp.utils.render.shaders.Blur;
import cc.simp.utils.render.shaders.Shadow;
import net.minecraft.client.shader.Framebuffer;

@ModuleInfo(label = "Post Processing", category = ModuleCategory.VISUALS)
public final class PostProcessingModule extends Module {
    public final  Property<Boolean> blur = new Property<>("Blur", true);
    public final Property<Boolean> shadow = new Property<>("Shadow", true);
    public final Property<Boolean> bloom = new Property<>("Bloom", true);

    public static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public void renderShaders() {
        if (!this.isEnabled()) return;

        if (blur.getValue()) {
            Blur.startBlur();
            Simp.INSTANCE.getEventBus().post(new ShaderEvent(ShaderEvent.ShaderType.BLUR));
            Blur.endBlur(10, 2);
            RenderUtils.resetColor();
        }

        if (shadow.getValue()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            RenderUtils.resetColor();
            Simp.INSTANCE.getEventBus().post(new ShaderEvent(ShaderEvent.ShaderType.SHADOW));
            stencilFramebuffer.unbindFramebuffer();
            RenderUtils.resetColor();

            if (stencilFramebuffer.framebufferTexture > 0) {
                Shadow.renderShadow(stencilFramebuffer.framebufferTexture, 50, 1);
            }
        }

        if (bloom.getValue()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);

            Simp.INSTANCE.getEventBus().post(new ShaderEvent(ShaderEvent.ShaderType.BLOOM));

            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBloom(stencilFramebuffer.framebufferTexture, 4, 1);

        }

    }
}
