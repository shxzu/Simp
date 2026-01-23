package cc.simp.modules.impl.combat;

import cc.simp.api.events.impl.game.MouseOverEvent;
import cc.simp.api.events.impl.game.RightClickEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.RayCastUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Reach", category = ModuleCategory.COMBAT)
public final class ReachModule extends Module {
    public final NumberProperty minRange = new NumberProperty("Min Range", 3, 4, 6, 0.01);
    public final NumberProperty maxRange = new NumberProperty("Max Range", 3, 4, 6, 0.01);
    private final NumberProperty bufferDecrease = new NumberProperty("Buffer Decrease", 1,  () -> !this.bufferAbuse.getValue(), 0.1, 10, 0.1);
    private final NumberProperty maxBuffer = new NumberProperty("Max Buffer", 5, () -> !this.bufferAbuse.getValue(), 1, 200, 1);
    private final Property<Boolean> bufferAbuse = new Property<>("Buffer Abuse", false);

    private int lastId, attackTicks;
    private double combo;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        this.attackTicks++;
    };

    @EventLink
    public final Listener<MouseOverEvent> onMouseOver = event -> {
        event.setRange(MathUtils.getRandom(this.minRange.getValue().doubleValue(),
                this.maxRange.getValue().doubleValue()));
    };

    @EventLink
    public final Listener<RightClickEvent> onRightClick = event ->
            mc.objectMouseOver = RayCastUtils.rayCast(RotationProcess.rotations, 4.5);

    @EventLink
    public final Listener<AttackEvent> onAttackEvent = event -> {
        final Entity entity = event.target;

        if (this.bufferAbuse.getValue()) {
            if (RayCastUtils.rayCast(RotationProcess.rotations, 3.0D).typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
                if ((this.attackTicks > 9 || entity.getEntityId() != this.lastId) && this.combo < this.maxBuffer.getValue().intValue()) {
                    this.combo++;
                } else {
                    event.setCancelled();
                }
            } else {
                this.combo = Math.max(0, this.combo - this.bufferDecrease.getValue().doubleValue());
            }
        } else {
            this.combo = 0;
        }

        this.lastId = entity.getEntityId();
        this.attackTicks = 0;
    };
}
