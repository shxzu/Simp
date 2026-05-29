package cc.simp.api.events.impl.game;

import cc.simp.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public final class LivingUpdateEvent implements Event {
    private final Entity entity;
}
