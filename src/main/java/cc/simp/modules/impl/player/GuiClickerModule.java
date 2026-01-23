package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "GUI Clicker", category = ModuleCategory.PLAYER)
public final class GuiClickerModule extends Module {
    public int mouseDownTicks;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        if (mc.currentScreen instanceof GuiContainer) {
            GuiContainer container = ((GuiContainer) mc.currentScreen);

            final int i = Mouse.getEventX() * container.width / mc.displayWidth;
            final int j = container.height - Mouse.getEventY() * container.height / mc.displayHeight - 1;

            try {
                if (Mouse.isButtonDown(0)) {
                    mouseDownTicks++;
                    if (mouseDownTicks > 2 && Math.random() > 0.1) container.mouseClicked(i, j, 0);
                } else if (Mouse.isButtonDown(1)) {
                    mouseDownTicks++;
                    if (mouseDownTicks > 2 && Math.random() > 0.1) container.mouseClicked(i, j, 1);
                } else {
                    mouseDownTicks = 0;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    };
}
