package cc.simp.modules.impl.combat;

import cc.simp.event.impl.packet.PacketReceiveEvent;
import cc.simp.event.impl.player.AttackEvent;
import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.property.impl.Representation;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Anti Knockback", category = ModuleCategory.COMBAT)
public final class AntiKnockbackModule extends Module {

    public static final EnumProperty<Mode> modeProperty = new EnumProperty<>("Mode", Mode.MOTION);
    public static final EnumProperty<IntaveMode> intaveModeProperty = new EnumProperty<>("Intave Mode", IntaveMode.SAFE, () -> modeProperty.getValue() == Mode.INTAVE);
    public DoubleProperty horizontal = new DoubleProperty("Horizontal", 100, () -> modeProperty.getValue() == Mode.MOTION, 0, 100, 1, Representation.INT);
    public DoubleProperty vertical = new DoubleProperty("Vertical", 100, () -> modeProperty.getValue() == Mode.MOTION, 0, 100, 1, Representation.INT);

    public enum Mode {
        MOTION,
        HYPIXEL,
        INTAVE,
        JUMP
    }

    public enum IntaveMode {
        SAFE,
        BLATANT,
    }

    public AntiKnockbackModule() {
        setSuffixListener(modeProperty);
    }

    @EventLink
    public final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (modeProperty.getValue() == Mode.HYPIXEL) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    p.setMotionX((int) (mc.thePlayer.motionX * 8000));
                    p.setMotionZ((int) (mc.thePlayer.motionZ * 8000));
                }
            }
        }
        if (modeProperty.getValue() == Mode.MOTION) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    p.setMotionX((int) (p.getMotionX() * horizontal.getValue() / 100.0));
                    p.setMotionZ((int) (p.getMotionZ() * horizontal.getValue() / 100.0));
                    p.setMotionY((int) (p.getMotionY() * vertical.getValue() / 100.0));
                }
            }

        }
    };

    @EventLink
    private final Listener<MotionEvent> motitonEventListener = event -> {
        if (modeProperty.getValue() == Mode.JUMP) {
            if (mc.thePlayer.hurtTime >= 8) {
                mc.gameSettings.keyBindJump.setPressed(true);
            }
            if (mc.thePlayer.hurtTime >= 4) {
                mc.gameSettings.keyBindJump.setPressed(false);
            } else if (mc.thePlayer.hurtTime > 1) {
                mc.gameSettings.keyBindJump.setPressed(GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
            }
        }
    };

    @EventLink
    private final Listener<AttackEvent> attackEventListener = event -> {
        if (modeProperty.getValue() == Mode.INTAVE) {
            if (intaveModeProperty.getValue() == IntaveMode.SAFE) {
                if (event.target instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionX *= 0.52;
                        mc.thePlayer.motionZ *= 0.52;
                    } else {
                        mc.thePlayer.motionX *= 0.8;
                        mc.thePlayer.motionZ *= 0.8;
                    }
                }
            } else {
                if (event.target instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                }
            }
        }
    };

}
