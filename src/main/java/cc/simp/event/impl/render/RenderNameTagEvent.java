package cc.simp.event.impl.render;

import net.minecraft.entity.EntityLivingBase;
import cc.simp.event.CancellableEvent;

public final class RenderNameTagEvent extends CancellableEvent {

    private final EntityLivingBase entityLivingBase;

    public RenderNameTagEvent(EntityLivingBase entityLivingBase) {
        this.entityLivingBase = entityLivingBase;
    }

    public EntityLivingBase getEntityLivingBase() {
        return entityLivingBase;
    }

}
