package cc.simp.modules.impl.render;

import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.property.impl.EnumProperty;

@ModuleInfo(label = "Block Animations", category = ModuleCategory.RENDER)
public final class BlockAnimationsModule extends Module {

    public static EnumProperty<AnimationMode> animationModeProperty = new EnumProperty<>("Style", AnimationMode.SIMP);

    public static enum AnimationMode {
        SIMP,
        OLD,
        EXHIBITION,
        NOVOLINE,
    }

}
