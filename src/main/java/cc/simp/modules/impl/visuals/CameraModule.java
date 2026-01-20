package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.Property;
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
    public static NumberProperty slowdown = new NumberProperty("Slowdown", 1, 1, 15, 1);
    public static Property<Boolean> fluxSwing = new Property<>("Flux Swing", false);
    public static Property<Boolean> swingEating = new Property<>("Swing While Eating", false);
    public static Property<Boolean> noSneakCamera = new Property<>("No Sneak Camera", false);
    public static Property<Boolean> noFireOverlay = new Property<>("No Fire Overlay", true);
    public static Property<Boolean> noBlindness = new Property<>("No Blindness", true);

    public CameraModule() {
        this.toggle();
    }

    public enum AnimationMode {
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
