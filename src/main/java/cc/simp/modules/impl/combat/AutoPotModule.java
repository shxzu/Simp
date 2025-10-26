package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.PlayerUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.util.vector.Vector2f;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Pot", category = ModuleCategory.COMBAT)
public final class AutoPotModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 50, 0, 100, 5);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 5, 0, 10, 1);
    private final Timer stopWatch = new Timer();

    private int attackTicks;
    private long nextThrow;

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextThrow) || attackTicks < 10 || Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemPotion) {
                final ItemPotion potion = (ItemPotion) item;
                final PotionEffect effect = potion.getEffects(stack).get(0);

                if (!ItemPotion.isSplash(stack.getMetadata()) ||
                        !PlayerUtils.goodPotion(effect.getPotionID()) ||
                        (effect.getPotionID() == Potion.regeneration.id ||
                                effect.getPotionID() == Potion.heal.id) &&
                                mc.thePlayer.getHealth() > this.health.getValue().floatValue()) {
                    continue;
                }

                if (mc.thePlayer.isPotionActive(effect.getPotionID()) &&
                        mc.thePlayer.getActivePotionEffect(effect.getPotionID()).getDuration() != 0) {
                    continue;
                }

                final float rotationSpeed = this.rotationSpeed.getValue().floatValue();
                RotationProcess.setRotations(new Vector2f((float) (mc.thePlayer.rotationYaw + (Math.random() - 0.5) * 3), (float) (87 + Math.random() * 3)), rotationSpeed, MovementFix.NORMAL);

                mc.thePlayer.inventory.currentItem = i;

                if (RotationProcess.rotations.y > 85) {
                    mc.playerController.syncCurrentPlayItem();
                    PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));

                    this.nextThrow = delay.getValue().longValue() * 10;
                    stopWatch.reset();
                    break;
                }
            }
        }
    };

    @EventLink
    public final Listener<AttackEvent> onAttack = event -> this.attackTicks = 0;
}
