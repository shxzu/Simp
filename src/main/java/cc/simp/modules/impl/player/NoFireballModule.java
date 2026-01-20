package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.BadPacketsProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.RotationUtils;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "No Fireball", category = ModuleCategory.PLAYER)
public final class NoFireballModule extends Module {

    private final Property<Boolean> rotate = new Property<>("Rotate", true);

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        if (BadPacketsProcess.bad()) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFireball && entity.getDistanceToEntity(mc.thePlayer) < 5) {
                if (this.rotate.getValue()) {
                    RotationProcess.setRotations(RotationUtils.calculate(entity), 10, MovementFix.NORMAL);
                }
                mc.clickMouse();
                break;
            }
        }
    };

}
