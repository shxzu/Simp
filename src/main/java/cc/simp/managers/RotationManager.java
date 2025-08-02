package cc.simp.managers;

import cc.simp.Simp;
import cc.simp.utils.mc.MovementUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;

import java.util.Random;

public class RotationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private float clientYaw;
    private float clientPitch;
    private float prevClientYaw;
    private float prevClientPitch;
    private long lastRotationUpdate;
    private boolean isRotating;
    private boolean isReturning;
    private static final long ROTATION_TIMEOUT = 300L;
    private Random random;

    public RotationManager() {
        this.lastRotationUpdate = 0L;
        this.isRotating = false;
        this.isReturning = false;
        this.random = new Random();
        this.clientYaw = 114514.0f;
        this.clientPitch = 114514.0f;
        this.prevClientYaw = 114514.0f;
        this.prevClientPitch = 114514.0f;
    }

    public void rotateToward(final float targetYaw, final float targetPitch, float rotationSpeed) {
        rotationSpeed += (this.random.nextFloat() - 0.5f) * 10.0f;
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - this.clientYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(targetPitch - this.clientPitch);
        yawDiff = MathHelper.clamp_float(yawDiff, -rotationSpeed, rotationSpeed);
        pitchDiff = MathHelper.clamp_float(pitchDiff, -rotationSpeed, rotationSpeed);
        this.setRotations(this.clientYaw + yawDiff, this.clientPitch + pitchDiff);
    }

    public boolean canHitEntityAtRotation(final Entity target, final float yaw, final float pitch) {
        final double reach = this.mc.playerController.getBlockReachDistance();
        final Vec3 eyePos = this.mc.thePlayer.getPositionEyes(1.0f);
        final Vec3 lookVec = this.getLookVecFromRotations(yaw, pitch);
        final Vec3 reachVec = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
        final AxisAlignedBB box = target.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        final MovingObjectPosition hit = box.calculateIntercept(eyePos, reachVec);
        return hit != null;
    }

    public void faceBlockHypixelSafe(final float rotationSpeed, final boolean slowdown) {
        this.rotateToward(this.snapToHypYaw(MovementUtils.getDirection(), slowdown), 80.0f, rotationSpeed);
    }

    public float snapToHypYaw(final float yaw, final boolean slowdown) {
        final float snappedBase = Math.round(yaw / 45.0f) * 45.0f;
        float lowerOffset;
        float upperOffset;
        if (Math.abs(snappedBase % 90.0f) < 0.001f) {
            lowerOffset = 111.0f;
            upperOffset = 111.0f;
        }
        else {
            lowerOffset = 137.0f;
            upperOffset = 137.0f;
            if (slowdown) {
                MovementUtils.strafe(0.009999999776482582);
            }
        }
        final float lowerCandidate = snappedBase - lowerOffset;
        final float upperCandidate = snappedBase + upperOffset;
        return (Math.abs(yaw - lowerCandidate) <= Math.abs(upperCandidate - yaw)) ? lowerCandidate : upperCandidate;
    }

    public void tick() {
        if (this.isRotating) {
            if (System.currentTimeMillis() - this.lastRotationUpdate <= 300L) {
                this.updateRotations();
                return;
            }
            this.isRotating = false;
            this.isReturning = true;
        }
        if (this.isReturning && this.mc.thePlayer != null) {
            final float targetYaw = this.mc.thePlayer.rotationYaw;
            final float targetPitch = this.mc.thePlayer.rotationPitch;
            float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - this.clientYaw);
            float pitchDiff = MathHelper.wrapAngleTo180_float(targetPitch - this.clientPitch);
            final float returnSpeed = 30.0f;
            yawDiff = MathHelper.clamp_float(yawDiff, -returnSpeed, returnSpeed);
            pitchDiff = MathHelper.clamp_float(pitchDiff, -returnSpeed, returnSpeed);
            yawDiff += (this.random.nextFloat() - 0.5f) * 2.0f;
            pitchDiff += (this.random.nextFloat() - 0.5f) * 2.0f;
            this.prevClientYaw = this.clientYaw;
            this.prevClientPitch = this.clientPitch;
            this.clientYaw += this.applyGCDFix(yawDiff);
            this.clientPitch += this.applyGCDFix(pitchDiff);
            if (Math.abs(yawDiff) < 1.0f && Math.abs(pitchDiff) < 1.0f) {
                this.clientYaw = targetYaw;
                this.clientPitch = targetPitch;
                this.isReturning = false;
            }
        } else if (this.mc.thePlayer != null) {
            this.prevClientYaw = this.clientYaw;
            this.prevClientPitch = this.clientPitch;
            this.clientYaw = this.mc.thePlayer.rotationYaw;
            this.clientPitch = this.mc.thePlayer.rotationPitch;
        }
    }

    public void resetRotationsInstantly() {
        if (this.isRotating) {
            this.isRotating = false;
            this.isReturning = false;
            if (this.mc.thePlayer != null) {
                this.clientYaw = this.mc.thePlayer.rotationYaw;
                this.clientPitch = this.mc.thePlayer.rotationPitch;
                this.prevClientYaw = this.clientYaw;
                this.prevClientPitch = this.clientPitch;
            }
        }
    }

    private void updateRotations() {
        this.prevClientYaw = this.clientYaw;
        this.prevClientPitch = this.clientPitch;
    }

    private void setRotations(final float yaw, final float pitch) {
        this.clientYaw = this.applyGCDFix(yaw);
        this.clientPitch = MathHelper.clamp_float(this.applyGCDFix(pitch), -90.0f, 90.0f);
        this.onRotationUpdated();
    }

    private float getGCDValue() {
        final double sensitivity = this.mc.gameSettings.mouseSensitivity;
        final double f = sensitivity * 0.6000000238418579 + 0.20000000298023224;
        return (float) (f * f * f * 8.0);
    }

    private float applyGCDFix(final float delta) {
        final float gcd = this.getGCDValue();
        return Math.round(delta / gcd) * gcd;
    }

    private void onRotationUpdated() {
        this.lastRotationUpdate = System.currentTimeMillis();
        this.isRotating = true;
    }

    private Vec3 getLookVecFromRotations(final float yaw, final float pitch) {
        final float yawRad = (float) Math.toRadians(yaw);
        final float pitchRad = (float) Math.toRadians(pitch);
        final float x = -MathHelper.cos(pitchRad) * MathHelper.sin(yawRad);
        final float y = -MathHelper.sin(pitchRad);
        final float z = MathHelper.cos(pitchRad) * MathHelper.cos(yawRad);
        return new Vec3(x, y, z);
    }

    public float[] getRotationsToBlock(BlockPos blockPos, EnumFacing enumFacing) {
        double d = (double) blockPos.getX() + 0.5 - mc.thePlayer.posX + (double) enumFacing.getFrontOffsetX() * 0.25;
        double d2 = (double) blockPos.getZ() + 0.5 - mc.thePlayer.posZ + (double) enumFacing.getFrontOffsetZ() * 0.25;
        double d3 = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - blockPos.getY() - (double) enumFacing.getFrontOffsetY() * 0.25;
        double d4 = MathHelper.sqrt_double(d * d + d2 * d2);
        float f = (float) (Math.atan2(d2, d) * 180.0 / Math.PI) - 90.0f;
        float f2 = (float) (Math.atan2(d3, d4) * 180.0 / Math.PI);
        f = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(f - mc.thePlayer.rotationYaw);
        f2 = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(f2 - mc.thePlayer.rotationPitch);
        return new float[]{f, f2};
    }

    public float[] getRotationsTo(final double x, final double y, final double z) {
        final Vec3 eyePos = this.mc.thePlayer.getPositionEyes(1.0f);
        final double diffX = x - eyePos.xCoord;
        final double diffY = y - eyePos.yCoord;
        final double diffZ = z - eyePos.zCoord;
        final double distXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 57.29577951308232) - 90.0f;
        float pitch = (float) (-(Math.atan2(diffY, distXZ) * 57.29577951308232));
        yaw = this.mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - this.mc.thePlayer.rotationYaw);
        pitch = this.mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - this.mc.thePlayer.rotationPitch);
        return new float[]{yaw, pitch};
    }

    public boolean faceEntity(final Entity entity, final float rotationSpeed, float noiseValue) {
        final float[] rotations = this.getRotationsTo(entity.posX, entity.posY + entity.getEyeHeight() / 2.0, entity.posZ);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];
        final float currentPitch = this.getClientPitch();
        targetYaw += (float) ((Math.random() - 0.5) * 2 * noiseValue);
        targetPitch += (float) ((Math.random() - 0.5) * 2 * noiseValue);
        targetPitch = Math.max(-90F, Math.min(90F, targetPitch));
        this.rotateToward(targetYaw, currentPitch, rotationSpeed);
        if (this.canHitEntityAtRotation(entity, this.getClientYaw(), this.getClientPitch())) {
            return true;
        }
        this.rotateToward(targetYaw, targetPitch, rotationSpeed);
        return this.canHitEntityAtRotation(entity, this.getClientYaw(), this.getClientPitch());
    }

    public boolean faceEntity(final Entity entity, final float rotationSpeed) {
        final float[] rotations = this.getRotationsTo(entity.posX, entity.posY + entity.getEyeHeight() / 2.0, entity.posZ);
        final float targetYaw = rotations[0];
        final float targetPitch = rotations[1];
        final float currentPitch = this.getClientPitch();
        this.rotateToward(targetYaw, currentPitch, rotationSpeed);
        if (this.canHitEntityAtRotation(entity, this.getClientYaw(), this.getClientPitch())) {
            return true;
        }
        this.rotateToward(targetYaw, targetPitch, rotationSpeed);
        return this.canHitEntityAtRotation(entity, this.getClientYaw(), this.getClientPitch());
    }

    public boolean isRotating() {
        return Simp.INSTANCE.getBackgroundManager().canRotation() && (this.isRotating || this.isReturning);
    }

    public float getClientYaw() {
        return this.clientYaw;
    }

    public float getClientPitch() {
        return this.clientPitch;
    }

    public float getPrevClientYaw() {
        return this.prevClientYaw;
    }

    public float getPrevClientPitch() {
        return this.prevClientPitch;
    }

}
