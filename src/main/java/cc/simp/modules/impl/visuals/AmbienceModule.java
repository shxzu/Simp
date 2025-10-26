package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Ambience", category = ModuleCategory.VISUALS)
public final class AmbienceModule extends Module {

    private final ModeProperty<TimeEnum> timeProperty = new ModeProperty<>("Time", TimeEnum.Night);
    private final Property<Boolean> rainProperty = new Property<>("Rain", false);

    public enum TimeEnum {
        Day,
        Night,
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (mc.theWorld == null)
            return;
        switch (timeProperty.getValue()) {
            case Day:
                mc.theWorld.setWorldTime(1000);
                break;
            case Night:
                mc.theWorld.setWorldTime(18000);
                break;
        }
        if (rainProperty.getValue()) {
            mc.theWorld.setRainStrength(1);
            mc.theWorld.setThunderStrength(1);
        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        }
    };
}
