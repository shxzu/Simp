package cc.simp.utils.client.mc;

import cc.simp.utils.client.Util;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public class PlayerUtils extends Util {

    public static boolean isHoldingSword() {
        final ItemStack stack;
        return (stack = mc.thePlayer.getCurrentEquippedItem()) != null && stack.getItem() instanceof ItemSword;
    }

}
