package cc.simp.managers;

import cc.simp.Simp;
import cc.simp.event.impl.packet.PacketReceiveEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.event.impl.player.PreUpdateEvent;
import cc.simp.event.impl.world.WorldLoadEvent;
import cc.simp.utils.client.Timer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import static cc.simp.utils.client.Util.mc;

public class BackgroundManager {

    private int rotLockTick;
    private boolean worldFullLoaded;
    private boolean loadingWorld;
    private Timer cfgTimer = new Timer();

    public BackgroundManager() {
        this.worldFullLoaded = true;
        this.loadingWorld = false;
        this.rotLockTick = 0;
    }

    public boolean isWorldFullLoaded() {
        return this.worldFullLoaded && !this.loadingWorld;
    }

    public boolean canRotation() {
        return this.rotLockTick == 0;
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        this.worldFullLoaded = false;
        this.loadingWorld = true;
        this.rotLockTick = 0;
    };

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {

        if (this.cfgTimer.hasTimeElapsed(30000, true)) Simp.INSTANCE.getConfigManager().saveConfig("default");

        if (this.rotLockTick > 0) {
            --this.rotLockTick;
        }

        if (this.loadingWorld && mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.ticksExisted > 10.0f && mc.currentScreen == null) {
            this.worldFullLoaded = true;
            this.loadingWorld = false;
        }

    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (mc.thePlayer == null) {
            return;
        }
        final Packet<?> packet = event.getPacket();
        if (packet instanceof S08PacketPlayerPosLook) {
            Simp.INSTANCE.getRotationManager().resetRotationsInstantly();
            this.rotLockTick = 3;
        }
    };

}
