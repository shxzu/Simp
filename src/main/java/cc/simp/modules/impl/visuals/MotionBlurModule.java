package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;

@ModuleInfo(label = "Motion Blur", category = ModuleCategory.VISUALS)
public final class MotionBlurModule extends Module {
    public NumberProperty blurAmount = new NumberProperty("Blur Amount", 7.0, 0.0, 10.0, 0.1);
}
