package cc.simp.modules.impl.client;

import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.property.impl.EnumProperty;

@ModuleInfo(label = "Click GUI", category = ModuleCategory.CLIENT)
public final class ClickGUIModule extends Module {

    public static EnumProperty<ClickGUIStyle> clickGuiStyleProperty = new EnumProperty<>("Click GUI Style", ClickGUIStyle.SIMP);

    public static enum ClickGUIStyle {
        SIMP,
        ASTOLFO;
    }

    public ClickGUIModule() {
        this.toggle();
    }

    // HANDLED IN MAIN CLIENT CLASS!!

}
