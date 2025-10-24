package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.BlockAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Legit Scaffold", category = ModuleCategory.PLAYER)
public final class LegitScaffoldModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 50.0, 0.0, 200.0, 10.0);
    private final Property<Boolean> blockCheck = new Property<>("Blocks Only", true);
    private final Property<Boolean> directionCheck = new Property<>("Directional Check", true);

    private boolean wasOverBlock = false;
    private long lastSneakTime = 0;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = e -> {
        if (e.isPre()) {
            if (!blockCheck.getValue() || (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) && !directionCheck.getValue() || mc.thePlayer.moveForward < 0) {
                if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround) {
                    mc.gameSettings.keyBindSneak.setPressed(true);
                    wasOverBlock = true;
                } else if (mc.thePlayer.onGround) {
                    if (wasOverBlock) lastSneakTime = System.currentTimeMillis();

                    long currentTime = System.currentTimeMillis();
                    long delayTime = delay.getValue().longValue();
                    long randomizedDelay = (long) (delayTime * (Math.random() * 0.1 + 0.95));

                    if (currentTime - lastSneakTime >= randomizedDelay) {
                        mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
                    }

                    wasOverBlock = false;
                }
            } else {
                mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
            }
        }
    };

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
    }
}