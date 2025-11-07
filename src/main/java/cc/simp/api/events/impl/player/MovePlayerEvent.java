package cc.simp.api.events.impl.player;

import cc.simp.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class MovePlayerEvent extends CancellableEvent {
    private double x, y, z;
}
