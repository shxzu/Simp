package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Fast Break", category = ModuleCategory.PLAYER)
public final class FastBreakModule extends Module {

    public final NumberProperty speed = new NumberProperty("Speed", 15, 1, 100, 1);
    public final NumberProperty delay = new NumberProperty("Delay", 0, 0, 4, 1);

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        setSuffix(speed.getValue().intValue() + "%");
        if (!mc.playerController.isInCreativeMode()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                mc.playerController.setBlockHitDelay(Math.min(mc.playerController.getBlockHitDelay(), this.delay.getValue().intValue() + 1));
                if (mc.playerController.getIsHittingBlock()) {
                    float curBlockDamageMP = mc.playerController.getCurBlockDamageMP();
                    float damage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
                    if (curBlockDamageMP < damage) {
                        mc.playerController.setCurBlockDamageMP(damage);
                    }
                }
            }
        }
    };
}
