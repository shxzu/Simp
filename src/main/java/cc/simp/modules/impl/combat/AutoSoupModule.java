package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Soup", category = ModuleCategory.COMBAT)
public final class AutoSoupModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 50, 0, 100, 5);
    private final Timer stopWatch = new Timer();

    private int attackTicks;
    private long nextEat;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextEat) || attackTicks < 10 || Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemSoup && mc.thePlayer.getHealth() <= this.health.getValue().floatValue()) {
                mc.thePlayer.inventory.currentItem = i;

                mc.playerController.syncCurrentPlayItem();
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));

                this.nextEat = delay.getValue().longValue() * 10;
                stopWatch.reset();
                break;
            }
        }
    };

    @EventLink
    public final Listener<AttackEvent> onAttack = event -> this.attackTicks = 0;
}
