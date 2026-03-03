package cc.simp.api.events.impl.render;

import cc.simp.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;

@Getter
@Setter
@AllArgsConstructor
public final class GlintEvent extends CancellableEvent {

    private boolean enchanted, render;
    private ItemStack itemStack;
    private IBakedModel model;

}
