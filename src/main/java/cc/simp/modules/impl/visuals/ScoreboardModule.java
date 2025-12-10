package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;

@ModuleInfo(label = "Scoreboard", category = ModuleCategory.VISUALS)
public class ScoreboardModule extends Module {

    public static ModeProperty<Mode> scoreboardStyle = new ModeProperty<>("Scoreboard Style", Mode.Left);

    public enum Mode {
        Vanilla("Vanilla"), VanillaOffset("Vanilla Offset"), Left("Left"), LeftOffset("Left Offset");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }
}
