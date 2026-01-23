package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.MouseOverEvent;
import cc.simp.api.events.impl.game.RightClickEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.mc.RayCastUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Hitbox Expand", category = ModuleCategory.COMBAT)
public final class HitboxExpandModule extends Module {
    public final NumberProperty expand = new NumberProperty("Expand Amount", 0, 0, 6, 0.01);
    private final Property<Boolean> effectRange = new Property<>("Effect range", true);

    @EventLink
    public final Listener<MouseOverEvent> onMouseOver = event -> {
        event.setExpand(this.expand.getValue().floatValue());

        if (!this.effectRange.getValue()) {
            event.setRange(event.getRange() - expand.getValue().doubleValue());
        }
    };

    @EventLink
    public final Listener<RightClickEvent> onRightClick = event ->
            mc.objectMouseOver = RayCastUtils.rayCast(RotationProcess.rotations, 4.5);
}
