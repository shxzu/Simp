package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "W Tap", category = ModuleCategory.COMBAT)
public class WTapModule extends Module {

    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Legit);
    public static final NumberProperty chance = new NumberProperty("Chance", 100, 0, 100, 1);

    private enum Mode {
        Legit ("Legit"),
        Packet ("Packet");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean unsprint, wTap;
    private EntityLivingBase target;

    @EventLink
    public Listener<AttackEvent> attackEventListener = event -> {

        if (event.target == null) return;

        if (mode.getValue() == Mode.Packet) {
            target = event.target;
        }
        if (mode.getValue() == Mode.Legit) {
            wTap = Math.random() * 100 < chance.getValue() && event.target.hurtTime >= 6;

            if (!wTap || unsprint) return;

            if (mc.thePlayer.isSprinting() || mc.gameSettings.keyBindSprint.isKeyDown()) {
                mc.gameSettings.keyBindSprint.setPressed(true);
                unsprint = true;
            }
        }
    };

    @EventLink
    public Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        if (mode.getValue() == Mode.Legit) {
            if (!wTap) return;

            if (unsprint && Math.random() * 100 < chance.getValue()) {
                mc.gameSettings.keyBindSprint.setPressed(false);
                unsprint = false;
            }
        }
        if (mode.getValue() == Mode.Packet) {
            if (target != null && target.hurtTime == 9) {
                if (Math.random() * 100 < chance.getValue()) {
                    PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }
            }
        }
    };

    @Override
    public void onEnable() {

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
