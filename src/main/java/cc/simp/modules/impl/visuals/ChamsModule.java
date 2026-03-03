package cc.simp.modules.impl.visuals;

import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Chams", category = ModuleCategory.VISUALS)
public final class ChamsModule extends Module {

    private static final Property<Boolean> tileEntities = new Property<>("Tile Entities", false);

    public static boolean shouldRender(Entity entity) {
        return entity instanceof EntityPlayer && (!(entity instanceof EntityPlayerSP) || mc.gameSettings.thirdPersonView != 0);
    }

    public static boolean doRenderTileEntities() {
        return tileEntities.getValue();
    }
}
