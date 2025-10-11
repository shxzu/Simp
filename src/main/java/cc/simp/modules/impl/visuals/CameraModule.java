package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;

@ModuleInfo(label = "Camera", category = ModuleCategory.VISUALS)
public final class CameraModule extends Module {

    public static ModeProperty<AnimationMode> mode = new ModeProperty<>("Style", AnimationMode.Old);
    public static NumberProperty x = new NumberProperty("X", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty y = new NumberProperty("Y", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty z = new NumberProperty("Z", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty scale = new NumberProperty("Scale", 1, 0.1, 2, 0.1);
    public static NumberProperty swingSpeed = new NumberProperty("Swing Speed", 1, -200, 50, 1);

    public CameraModule() {
        this.toggle();
    }

    public static enum AnimationMode {
        Slide,
        Old,
        Exhibition,
        Novoline,
        Spin,
        Noov,
        Smooth,
        Leaked
    }
}
