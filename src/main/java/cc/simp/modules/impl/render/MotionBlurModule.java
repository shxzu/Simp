package cc.simp.modules.impl.render;

import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.DoubleProperty;

@ModuleInfo(label = "Motion Blur", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {
    public DoubleProperty blurAmountProperty = new DoubleProperty("Blur Amount", 7.0, 10.0, 0.0, 0.1);
}
