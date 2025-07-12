package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Auto Tool", category = ModuleCategory.PLAYER)
public class AutoToolModule extends Module {

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
        if (Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && KillAuraModule.target != null) {
            float bestStr = 0.0F;
            int itemToUse = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
                if (itemStack == null) continue;

                if (!(itemStack.getItem() instanceof ItemSword)) continue;

                ItemSword item = (ItemSword) itemStack.getItem();

                if (item.attackDamage > bestStr) {
                    bestStr = item.attackDamage;
                    itemToUse = i;
                }
            }
            if (itemToUse != -1) mc.thePlayer.inventory.currentItem = itemToUse;
            return;
        }

        if (!mc.gameSettings.keyBindAttack.isPressed() || mc.objectMouseOver == null) return;

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) return;

        int itemToUse = getBestToolSlot(pos);
        if (itemToUse == -1) return;

        mc.thePlayer.inventory.currentItem = itemToUse;
    };

    private int getBestToolSlot(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();

        float bestStr = 1.0F;
        int itemTouse = -1;

        for(int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) continue;

            if (itemStack.getStrVsBlock(block) > bestStr) {
                bestStr = itemStack.getStrVsBlock(block);
                itemTouse = i;
            }
        }

        return itemTouse;
    }

}

