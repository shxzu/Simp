package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.TargetSelectionProcess;
import net.minecraft.util.ResourceLocation;

@ModuleInfo(label = "Capes", category = ModuleCategory.VISUALS)
public final class CapesModule extends Module {

    public static ModeProperty<Cape> cape = new ModeProperty<>("Cape", Cape.Simp);

    public enum Cape {
        Simp,
        Rise,
        Gato,
        Minecon,
        Kitty,
        Blonde,
        Epstien,
        OMG
    }

}
