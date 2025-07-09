package cc.simp.modules.impl.render;

import cc.simp.event.impl.packet.PacketReceiveEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Ambience", category = ModuleCategory.RENDER)
public final class AmbienceModule extends Module {

    private final EnumProperty<TimeEnum> timeProperty = new EnumProperty<>("Time", TimeEnum.NIGHT);
    private final Property<Boolean> rainProperty = new Property<>("Rain", true);

    public enum TimeEnum {
        DAY,
        NIGHT,
    }

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        setSuffixListener(timeProperty);
        if (mc.theWorld == null)
            return;
        switch (timeProperty.getValue()) {
            case DAY:
                mc.theWorld.setWorldTime(1000);
                break;
            case NIGHT:
                mc.theWorld.setWorldTime(18000);
                break;
        }
        if (rainProperty.getValue()) {
            mc.theWorld.setRainStrength(1);
            mc.theWorld.setThunderStrength(1);
        }
    };

    @EventLink
    private final Listener<PacketReceiveEvent> rE = event -> {
        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled();
        }
    };
}
