package cc.simp.utils.client;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import static cc.simp.utils.Util.mc;

public class ViaMCPFixes {
    public static float eyeHeight;
    public static float lastEyeHeight;
    public static boolean initialized = false;
    private static boolean forceSneaking = false;
    private static long lastRenderTime = System.nanoTime();

    public static void handlePlayerSize() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (shouldSwimOrCrawl() && mc.thePlayer.isInWater()) {
            double d3 = mc.thePlayer.getLookVec().yCoord;
            double d4 = 0.025D;
            double motionBoost = d3 <= 0.0D || mc.thePlayer.worldObj.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1.0D - 0.64D, mc.thePlayer.posZ)).getBlock().getMaterial() == Material.water ? ((d3 - mc.thePlayer.motionY) * d4) + 0.018 : 0.018;
            mc.thePlayer.motionX *= 1.09F;
            mc.thePlayer.motionZ *= 1.09F;
            mc.thePlayer.motionY += motionBoost;
        }

        float newHeight = shouldSwimOrCrawl() ? 0.6f : mc.thePlayer.isSneaking() && olderThanOrEqualsTo1_8() ? 1.8f : mc.thePlayer.isSneaking() && olderThanOrEqualsTo1_13_2() ? 1.65f : mc.thePlayer.isSneaking() ? 1.5f : 1.8f;

        double d0 = mc.thePlayer.width / 2.0;
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB fixedBB = new AxisAlignedBB(mc.thePlayer.posX - d0, box.minY, mc.thePlayer.posZ - d0, mc.thePlayer.posX + d0, box.minY + mc.thePlayer.height, mc.thePlayer.posZ + d0);
        AxisAlignedBB sneakBB = new AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.8, box.minZ + 0.6);

        mc.thePlayer.setEntityBoundingBox(fixedBB);
        mc.thePlayer.height = newHeight;

        if (newerThanOrEqualsTo1_9() && mc.thePlayer.onGround && !mc.theWorld.getCollisionBoxes(sneakBB).isEmpty() && !shouldSwimOrCrawl()) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            forceSneaking = true;
        } else if (mc.theWorld.getCollisionBoxes(sneakBB).isEmpty() && forceSneaking) {
            if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
                mc.gameSettings.keyBindSneak.setPressed(false);
            forceSneaking = false;
        }
    }

    public static void handleEyeYHeight() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.nanoTime();
        float deltaTime = (now - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = now;
        deltaTime = Math.min(deltaTime, 0.05f);

        float startHeight = 1.62f;

        if (!initialized) {
            eyeHeight = startHeight;
            lastEyeHeight = eyeHeight;
            initialized = true;
            return;
        }

        float endHeight = shouldSwimOrCrawl() ? 0.45f : olderThanOrEqualsTo1_8() ? 1.54f : olderThanOrEqualsTo1_13_2() ? 1.47f : 1.32f;
        float delta = shouldSwimOrCrawl() ? 0.06f : olderThanOrEqualsTo1_8() ? 0.154f : olderThanOrEqualsTo1_13_2() ? 0.147f : 0.132f;

        lastEyeHeight = eyeHeight;

        Float target = shouldSwimOrCrawl() || mc.thePlayer.isSneaking() ? Float.valueOf(endHeight) : eyeHeight < startHeight ? startHeight : null;

        if (target != null) {
            eyeHeight = lerp(eyeHeight, target, 100f * delta * deltaTime);
        }
    }

    public static boolean shouldNotPushout() {
        return shouldSwimOrCrawl() || newerThanOrEqualsTo1_13() && mc.thePlayer.isSneaking();
    }

    public static boolean shouldSwimOrCrawl() {
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB crawlBB = new AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.5, box.minZ + 0.6);

        return newerThanOrEqualsTo1_13() && (canSwim() || !mc.theWorld.getCollisionBoxes(crawlBB).isEmpty());
    }

    private static boolean isUnderWater() {
        final World world = mc.thePlayer.getEntityWorld();
        double eyeBlock = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - 0.25;
        BlockPos blockPos = new BlockPos(mc.thePlayer.posX, eyeBlock, mc.thePlayer.posZ);

        return world.getBlockState(blockPos).getBlock().getMaterial() == Material.water && !(mc.thePlayer.ridingEntity instanceof EntityBoat);
    }

    private static boolean canSwim() {
        return !mc.thePlayer.isSneaking() && mc.thePlayer.isInWater() && !mc.thePlayer.capabilities.isFlying && mc.thePlayer.isSprinting() && isUnderWater();
    }

    private static boolean olderThanOrEqualsTo1_8() {
        return (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8));
    }

    private static boolean olderThanOrEqualsTo1_13_2() {
        return ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2);
    }

    private static boolean newerThanOrEqualsTo1_13() {
        return ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_13);
    }

    private static boolean newerThanOrEqualsTo1_9() {
        return ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_9);
    }

    public static float lerp(float from, float to, float speed) {
        return from + (to - from) * speed;
    }
}
