package cc.simp.api.events.impl.game;

import cc.simp.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;

@Getter
@AllArgsConstructor
public class EntityHurtSoundEvent extends CancellableEvent {
    private final EntityLivingBase entity;
}
