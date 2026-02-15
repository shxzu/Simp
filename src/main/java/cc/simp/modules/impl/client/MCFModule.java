package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.MiddleClickEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Logger;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "MCF", category = ModuleCategory.CLIENT, description = "Middle Click to exclude players from targeting")
public class MCFModule extends Module {

    public static final List<Entity> excludedPlayers = new ArrayList<>();

    @EventLink
    public final Listener<MiddleClickEvent> onMiddleClick = event -> {
        if (mc.objectMouseOver != null &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
            mc.objectMouseOver.entityHit instanceof EntityPlayer) {

            EntityPlayer clickedPlayer = (EntityPlayer) mc.objectMouseOver.entityHit;

            if (clickedPlayer != mc.thePlayer) {
                if (excludedPlayers.contains(clickedPlayer)) {
                    excludedPlayers.remove(clickedPlayer);
                    Logger.chatPrint("§a[MCF] §fRemoved §c" + clickedPlayer.getName() + "§f from exclusion list");
                } else {
                    excludedPlayers.add(clickedPlayer);
                    Logger.chatPrint("§a[MCF] §fAdded §c" + clickedPlayer.getName() + "§f to exclusion list");
                }
                event.setCancelled(true);
            }
        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        excludedPlayers.clear();
    };

    @Override
    public void onDisable() {
        excludedPlayers.clear();
        super.onDisable();
    }
}

