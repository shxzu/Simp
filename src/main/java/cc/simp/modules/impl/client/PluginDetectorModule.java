package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.world.WorldLoadEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.server.S3APacketTabComplete;

import java.util.LinkedHashSet;
import java.util.Set;

@ModuleInfo(label = "Plugin Detector", category = ModuleCategory.CLIENT)
public final class PluginDetectorModule extends Module {
    private final Set<String> cachedPlugins = new LinkedHashSet<>();
    private final Timer clock = new Timer();
    private int step = 0;
    private boolean finished = false;
    private long lastStepTime = 0L;

    private void resetState() {
        cachedPlugins.clear();
        clock.reset();
        step = 0;
        finished = false;
        lastStepTime = 0L;
    }

    @Override
    public void onEnable() {
        resetState();
        sendNextRequest();
        lastStepTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @EventLink
    public final Listener<WorldLoadEvent> worldLoadEventListener = event -> {
        this.toggle();
    };

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        if (!finished && System.currentTimeMillis() - lastStepTime > 500) {
            step++;
            sendNextRequest();
            lastStepTime = System.currentTimeMillis();
        }

        if (!finished && clock.hasTimeElapsed(7000L)) {
            Logger.chatPrint("§c§lFailed to detect plugins");
            this.toggle();
            finished = true;
        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (!(event.getPacket() instanceof S3APacketTabComplete)) return;

        String[] commands = ((S3APacketTabComplete) event.getPacket()).func_149630_c();
        for (String command : commands) {
            String[] parts = command.split(":");
            if (parts.length > 1 && !parts[0].startsWith("/minecraft")) {
                String pluginName = parts[0].replace("/", "");
                cachedPlugins.add(pluginName);
            }
        }

        if (step >= 3) finishDetection();
    };

    private void sendNextRequest() {
        if (finished) return;

        switch (step) {
            case 0:
                PacketUtils.sendPacket(new C14PacketTabComplete("/version "));
                break;
            case 1:
                PacketUtils.sendPacket(new C14PacketTabComplete("/bukkit:version "));
                break;
            case 2:
                PacketUtils.sendPacket(new C14PacketTabComplete("/"));
                break;
            default:
                finishDetection();
                break;
        }
    }

    private void finishDetection() {
        if (finished) return;
        finished = true;

        if (cachedPlugins.isEmpty()) {
            Logger.chatPrint("§6§lNo plugins found");
        } else {
            String joined = String.join("§7, §a", cachedPlugins);
            Logger.chatPrint("§6§lPlugins §7(§b" + cachedPlugins.size() + "§7) §oTook " + clock.getTime() + "ms");
            Logger.chatPrint("§a" + joined);
        }

        this.toggle();
    }
}
