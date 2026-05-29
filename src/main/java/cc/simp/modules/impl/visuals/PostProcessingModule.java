package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
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
    public final NumberProperty blurRadius = new NumberProperty("Blur Radius", 10.0, blur::getValue, 1.0, 128.0, 1.0);
    public final NumberProperty blurCompression = new NumberProperty("Blur Compression", 2.0, blur::getValue, 0.1, 16.0, 0.1);
    public final NumberProperty blurStrength = new NumberProperty("Blur Strength", 1.0, blur::getValue, 0.0, 5.0, 0.05);
    public final Property<Boolean> shadow = new Property<>("Shadow", true);
    public final NumberProperty shadowRadius = new NumberProperty("Shadow Radius", 50.0, shadow::getValue, 0.0, 128.0, 1.0);
    public final NumberProperty shadowOffset = new NumberProperty("Shadow Offset", 1.0, shadow::getValue, 0.0, 16.0, 1.0);
    public final NumberProperty shadowStrength = new NumberProperty("Shadow Strength", 1.0, shadow::getValue, 0.0, 5.0, 0.1);
    public final Property<Boolean> bloom = new Property<>("Bloom", true);
    public final NumberProperty bloomIterations = new NumberProperty("Bloom Iterations", 4.0, () -> bloom.getValue(), 1.0, 8.0, 1.0);
    public final NumberProperty bloomOffset = new NumberProperty("Bloom Offset", 1.0, () -> bloom.getValue(), 0.0, 8.0, 1.0);
    public final NumberProperty bloomStrength = new NumberProperty("Bloom Strength", 1.2, () -> bloom.getValue(), 0.0, 5.0, 0.05);

    public static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public void renderShaders() {
        if (!this.isEnabled()) return;

        if (blur.getValue()) {
            Blur.startBlur();
            Simp.INSTANCE.getEventBus().post(new ShaderEvent(ShaderEvent.ShaderType.BLUR));
            Blur.endBlur(blurRadius.getValue().floatValue(), blurCompression.getValue().floatValue(), blurStrength.getValue().floatValue());
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
                Shadow.renderShadow(
                        stencilFramebuffer.framebufferTexture,
                        shadowRadius.getValue().intValue(),
                        shadowOffset.getValue().intValue(),
                        shadowStrength.getValue().floatValue()
                );
            }
        }

        if (bloom.getValue()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);

            Simp.INSTANCE.getEventBus().post(new ShaderEvent(ShaderEvent.ShaderType.BLOOM));

            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBloom(
                    stencilFramebuffer.framebufferTexture,
                    bloomIterations.getValue().intValue(),
                    bloomOffset.getValue().intValue(),
                    bloomStrength.getValue().floatValue()
            );

        }

    }
}
