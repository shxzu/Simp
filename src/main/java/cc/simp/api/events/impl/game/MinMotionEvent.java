package cc.simp.api.events.impl.game;

import cc.simp.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MinMotionEvent extends CancellableEvent {
    private double minimumMotion;
}
