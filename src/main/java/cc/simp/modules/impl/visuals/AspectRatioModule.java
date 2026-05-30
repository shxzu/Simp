package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;

@ModuleInfo(label = "Aspect Ratio", category = ModuleCategory.VISUALS)
public final class AspectRatioModule extends Module {

    public static NumberProperty aspect = new NumberProperty("Aspect", 1.0f, 0.1f, 5.0f, 0.1f);


}
