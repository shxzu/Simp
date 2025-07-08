package cc.simp.event.impl.player;

import cc.simp.event.CancellableEvent;
import net.minecraft.entity.Entity;

public class AttackEvent extends CancellableEvent {

    public AttackEvent(Entity target) {
        this.target = target;
    }

    public Entity target;
}
