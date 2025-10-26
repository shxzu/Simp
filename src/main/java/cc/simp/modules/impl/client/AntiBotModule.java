package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import com.mojang.authlib.GameProfile;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Anti Bot", category = ModuleCategory.CLIENT)
public class AntiBotModule extends Module {

    public ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.TabList);

    private enum Mode {
        TabList("Tab List"), NPC("NPC");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    public static final List<EntityLivingBase> botList = new ArrayList<>();

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {

        setSuffix(modeProperty.getValue().toString());

        // Global AntiBot Strategies

        mc.theWorld.playerEntities.forEach(player -> {
            if (player.maxHurtTime == 0) {
                if (player.getHealth() == 20.0f) {
                    String unformattedText = player.getDisplayName().getUnformattedText();
                    if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                        botList.add(player);
                    }
                    if (player.getDisplayName().toString().contains(" ")) {
                        botList.add(player);
                    }
                }
            }
            if (player.getDisplayName().toString().isEmpty()) {
                botList.add(player);
            }
            if(player.getEntityId() < 0) {
                botList.add(player);
            }
        });

        switch (modeProperty.getValue()) {
            case NPC:
                mc.theWorld.playerEntities.forEach(player -> {
                    if (player.moved) {
                        botList.remove(player);
                    } else {
                        botList.add(player);
                    }
                });
                break;
            case TabList:
                mc.theWorld.playerEntities.forEach(player -> {
                    if (!getTablist().contains(player.getDisplayName().toString())) {
                        botList.add(player);
                    }
                });
                break;

        }
    };

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        if (!mc.theWorld.playerEntities.isEmpty()) {
            mc.theWorld.playerEntities.forEach(player -> {
                botList.clear();
            });
        }
    };

    private static @NonNull List<String> getTablist() {
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().parallelStream()
                .map(NetworkPlayerInfo::getGameProfile)
                .filter(profile -> profile.getId() != Minecraft.getMinecraft().thePlayer.getUniqueID())
                .map(GameProfile::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void onDisable() {
        botList.clear();
        super.onDisable();
    }

}
