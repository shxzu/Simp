package cc.simp.utils.mc;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.utils.Util;
import cc.simp.utils.client.MathUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

import java.util.Arrays;

public class MovementUtils extends Util {

    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    private static boolean isMovingEnoughForSprint() {
        MovementInput movementInput = mc.thePlayer.movementInput;
        return movementInput.moveForward > 0.8F || movementInput.moveForward < -0.8F ||
                movementInput.moveStrafe > 0.8F || movementInput.moveStrafe < -0.8F;
    }
    public static boolean canSprint(boolean omni) {
        final EntityPlayerSP player = mc.thePlayer;
        return (omni ? isMovingEnoughForSprint() : player.movementInput.moveForward >= 0.8F) &&
                !player.isCollidedHorizontally &&
                (player.getFoodStats().getFoodLevel() > 6 ||
                        player.capabilities.allowFlying) &&
                !player.isSneaking() &&
                !player.isUsingItem() &&
                !player.isPotionActive(Potion.moveSlowdown.id);
    }

    public static boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F;
    }

    public static boolean isOnGround() {
        return mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically;
    }

    public static void setSpeed(double moveSpeed) {
        setSpeed(moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }

    public static void setSpeed(MotionEvent e, double speed) {
        final EntityPlayerSP player = mc.thePlayer;
        setSpeed(e, speed, player.moveForward, player.moveStrafing, player.rotationYaw);
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            if (strafe > 0.0D) {
                yaw += ((forward > 0.0D) ? -45 : 45);
            } else if (strafe < 0.0D) {
                yaw += ((forward > 0.0D) ? 45 : -45);
            }
            strafe = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        if (strafe > 0.0D) {
            strafe = 1.0D;
        } else if (strafe < 0.0D) {
            strafe = -1.0D;
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz;
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx;
    }

    public static void setSpeed(MotionEvent e, double speed, float forward, float strafing, float yaw) {
        if (forward == 0.0F && strafing == 0.0F)
            return;

        boolean reversed = forward < 0.0f;
        float strafingYaw = 90.0f *
                (forward > 0.0f ? 0.5f : reversed ? -0.5f : 1.0f);

        if (reversed)
            yaw += 180.0f;
        if (strafing > 0.0f)
            yaw -= strafingYaw;
        else if (strafing < 0.0f)
            yaw += strafingYaw;

        double x = StrictMath.cos(StrictMath.toRadians(yaw + 90.0f));
        double z = StrictMath.cos(StrictMath.toRadians(yaw));

        e.setPosX(x * speed);
        e.setPosZ(z * speed);
    }

    public void strafe(MotionEvent event) {
        strafe(event, getSpeed());
    }

    public static void strafe(double movementSpeed) {
        strafe(null, movementSpeed);
    }

    public static void strafe(MotionEvent motionEvent, double movementSpeed) {
        if (mc.thePlayer.movementInput.moveForward > 0.0) {
            mc.thePlayer.movementInput.moveForward = (float) 1.0;
        } else if (mc.thePlayer.movementInput.moveForward < 0.0) {
            mc.thePlayer.movementInput.moveForward = (float) -1.0;
        }

        if (mc.thePlayer.movementInput.moveStrafe > 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (float) 1.0;
        } else if (mc.thePlayer.movementInput.moveStrafe < 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (float) -1.0;
        }

        if (mc.thePlayer.movementInput.moveForward == 0.0 && mc.thePlayer.movementInput.moveStrafe == 0.0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }

        if (mc.thePlayer.movementInput.moveForward != 0.0 && mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveForward *= (float) Math.sin(0.6398355709958845);
            mc.thePlayer.movementInput.moveStrafe *= (float) Math.cos(0.6398355709958845);
        }

        if (motionEvent != null) {
            motionEvent.setPosX(mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw))
                    + mc.thePlayer.movementInput.moveStrafe * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)));
            motionEvent.setPosZ(mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw))
                    - mc.thePlayer.movementInput.moveStrafe * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)));
        } else {
            mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw))
                    + mc.thePlayer.movementInput.moveStrafe * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw));
            mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * movementSpeed * Math.cos(Math.toRadians(mc.thePlayer.rotationYaw))
                    - mc.thePlayer.movementInput.moveStrafe * movementSpeed * -Math.sin(Math.toRadians(mc.thePlayer.rotationYaw));
        }
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static double getSpeed() {
        return mc.thePlayer == null ? 0 : Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static void fixMovement(final MoveEvent event, final float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();

        final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MovementUtils.direction(mc.thePlayer.rotationYaw, forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MovementUtils.direction(yaw, predictedForward, predictedStrafe)));
                final double difference = MathUtils.wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }
    public static double direction() {
        float rotationYaw = mc.thePlayer.movementYaw;

        if (mc.thePlayer.moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (mc.thePlayer.moveForward < 0) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0) {
            forward = 0.5F;
        }

        if (mc.thePlayer.moveStrafing > 0) {
            rotationYaw -= 90 * forward;
        }

        if (mc.thePlayer.moveStrafing < 0) {
            rotationYaw += 90 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double direction(MoveEvent moveInputEvent) {
        float rotationYaw = mc.thePlayer.movementYaw;

        if (moveInputEvent.getForward() < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (moveInputEvent.getForward() < 0) {
            forward = -0.5F;
        } else if (moveInputEvent.getForward() > 0) {
            forward = 0.5F;
        }

        if (moveInputEvent.getStrafe() > 0) {
            rotationYaw -= 70 * forward;
        }

        if (moveInputEvent.getStrafe() < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double direction(float inputForward, float inputStrafe) {
        float rotationYaw = mc.thePlayer.movementYaw;

        if (inputForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (inputForward < 0) {
            forward = -0.5F;
        } else if (inputForward > 0) {
            forward = 0.5F;
        }

        if (inputStrafe > 0) {
            rotationYaw -= 70 * forward;
        }

        if (inputStrafe < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }
    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }
    public static void stop() {
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    public static void useDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        final int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> {
            down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0);
        });

        boolean active = down[0] == 1;

        if (!active) return;

        final double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        final double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        final double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(increase);
    }

    public static void moveFlying(double increase) {
        if (!MovementUtils.isMoving()) return;
        final double yaw = MovementUtils.direction();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }

}
