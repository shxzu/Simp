package cc.simp.modules.impl.client;

import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;

@ModuleInfo(label = "Toggle Sounds", category = ModuleCategory.CLIENT)
public class ToggleSoundsModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Simp);

    public enum Mode {
        Simp("Simp"),
        Augustus("Augustus"),
        Vanilla("Vanilla"),
        Sigma5("Sigma 5.0"),
        Note("Note");

        public String name;
        Mode(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

}
