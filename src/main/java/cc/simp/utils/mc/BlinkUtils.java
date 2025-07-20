package cc.simp.utils.mc;

import com.sun.javafx.geom.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// don't u just love it when I paste random things. hell the 30% of the client is pasted anyway, why not make it more.


public class BlinkUtils {
    private static final Minecraft mc;
    private static Double prevYMotion;
    private static boolean isStarted;
    public static boolean limiter;
    public static boolean blinking;
    private static final List<Packet<?>> packets;
    private static final List<Vec3d> positions;

    public static void addPacket(final Packet<?> packet) {
        BlinkUtils.packets.add(packet);
    }

    public static void doBlink() {
        if (BlinkUtils.mc.isIntegratedServerRunning()) {
            return;
        }
        BlinkUtils.blinking = true;
        if (BlinkUtils.prevYMotion == null && BlinkUtils.mc.thePlayer != null) {
            BlinkUtils.prevYMotion = BlinkUtils.mc.thePlayer.motionY;
        }
        if (!BlinkUtils.isStarted && BlinkUtils.mc.thePlayer != null) {
            synchronized (BlinkUtils.positions) {
                BlinkUtils.positions.add(new Vec3d(BlinkUtils.mc.thePlayer.posX, BlinkUtils.mc.thePlayer.getEntityBoundingBox().minY + BlinkUtils.mc.thePlayer.getEyeHeight() / 2.0f, BlinkUtils.mc.thePlayer.posZ));
                BlinkUtils.positions.add(new Vec3d(BlinkUtils.mc.thePlayer.posX, BlinkUtils.mc.thePlayer.getEntityBoundingBox().minY, BlinkUtils.mc.thePlayer.posZ));
            }
            BlinkUtils.isStarted = true;
            return;
        }
        if (BlinkUtils.mc.thePlayer != null) {
            synchronized (BlinkUtils.positions) {
                BlinkUtils.positions.add(new Vec3d(BlinkUtils.mc.thePlayer.posX, BlinkUtils.mc.thePlayer.posY, BlinkUtils.mc.thePlayer.posZ));
            }
        }
    }

    public static void sync(final boolean blinkSync, final boolean noSyncResetPos) {
        if (blinkSync) {
            try {
                BlinkUtils.limiter = true;
                while (!BlinkUtils.packets.isEmpty()) {
                    mc.getNetHandler().sendPacket(BlinkUtils.packets.remove(0));
                }
            }
            catch (final Exception ex) {}
            finally {
                BlinkUtils.limiter = false;
            }
            synchronized (BlinkUtils.positions) {
                BlinkUtils.positions.clear();
            }
        }
        else {
            try {
                BlinkUtils.limiter = true;
                BlinkUtils.packets.clear();
            }
            catch (final Exception ex2) {}
            finally {
                BlinkUtils.limiter = false;
            }
            if (noSyncResetPos && BlinkUtils.mc.thePlayer != null) {
                synchronized (BlinkUtils.positions) {
                    if (!BlinkUtils.positions.isEmpty() && BlinkUtils.positions.size() > 1) {
                        BlinkUtils.mc.thePlayer.setPosition(BlinkUtils.positions.get(1).x, BlinkUtils.positions.get(1).y, BlinkUtils.positions.get(1).z);
                    }
                }
                if (BlinkUtils.prevYMotion != null) {
                    BlinkUtils.mc.thePlayer.motionY = BlinkUtils.prevYMotion;
                }
            }
        }
    }

    public static void stopBlink() {
        synchronized (BlinkUtils.positions) {
            BlinkUtils.positions.clear();
        }
        BlinkUtils.prevYMotion = null;
        BlinkUtils.isStarted = false;
        BlinkUtils.blinking = false;
    }

    static {
        mc = Minecraft.getMinecraft();
        BlinkUtils.prevYMotion = null;
        BlinkUtils.isStarted = false;
        BlinkUtils.limiter = false;
        BlinkUtils.blinking = false;
        packets = Collections.synchronizedList(new ArrayList<Packet<?>>());
        positions = Collections.synchronizedList(new ArrayList<Vec3d>());
    }
}
