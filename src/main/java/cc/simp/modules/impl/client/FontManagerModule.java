package cc.simp.modules.impl.client;

import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.EnumProperty;

@ModuleInfo(label = "Font Manager", category = ModuleCategory.CLIENT)
public final class FontManagerModule extends Module {

    public static final EnumProperty<FontType> fontTypeProperty = new EnumProperty<>("Font Type", FontType.TAHOMA);

    public enum FontType {
        TAHOMA,
        MC
    }

    public FontManagerModule() {
        toggle();
    }

}
