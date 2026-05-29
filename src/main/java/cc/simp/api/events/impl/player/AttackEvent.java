package cc.simp.api.events.impl.player;

import cc.simp.api.events.CancellableEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class AttackEvent extends CancellableEvent {

    public AttackEvent(EntityLivingBase target) {
        this.target = target;
    }

    public EntityLivingBase target;
}