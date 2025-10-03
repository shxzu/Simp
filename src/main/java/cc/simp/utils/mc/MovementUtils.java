package cc.simp.utils.mc;

import cc.simp.utils.Util;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovementInput;

public class MovementUtils extends Util {
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
}
