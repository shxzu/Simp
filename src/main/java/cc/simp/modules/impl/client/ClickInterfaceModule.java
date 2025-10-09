package cc.simp.modules.impl.client;

import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;


@ModuleInfo(label = "Click Interface", category = ModuleCategory.CLIENT)
public final class ClickInterfaceModule extends Module {

    public static final ModeProperty<Font> font = new ModeProperty<>("Font", Font.MC);

    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.Simp);

    public enum Font {
        Simp,
        MC
    }

    public enum Color {
        Rainbow,
        Astolfo,
        Simp,
        White,
        Red,
        Purple,
        Pink
    }

    public ClickInterfaceModule() {
        toggle();
    }
}
