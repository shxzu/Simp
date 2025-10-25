package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "W Tap", category = ModuleCategory.COMBAT)
public class WTapModule extends Module {

    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Legit);
    public static final Property<Boolean> onlyWithKillaura = new Property<>("Only With Killaura", false);
    public static final NumberProperty wtapChance = new NumberProperty("WTap Chance", 100, 0, 100, 1);

    private enum Mode {
        Legit,
        Packet,
        Silent
    }

    private boolean shouldWTap;
    private int wtapTicks;

    @EventLink
    public Listener<MoveEvent> moveEventListener = event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mode.getValue() == Mode.Legit) {
            if (shouldWTap && wtapTicks > 0) {
                if (wtapTicks == 2) {
                    if (!mc.gameSettings.keyBindForward.isPressed()) return;
                    mc.gameSettings.keyBindForward.setPressed(false);
                } else if (wtapTicks == 1) {
                    mc.gameSettings.keyBindForward.setPressed(true);
                    shouldWTap = false;
                }
                wtapTicks--;
            }
        }

        if (mode.getValue() == Mode.Packet) {
            if (shouldWTap && wtapTicks > 0) {
                if (wtapTicks == 2) {
                    if (!mc.gameSettings.keyBindForward.isPressed()) return;
                    PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                } else if (wtapTicks == 1) {
                    PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    shouldWTap = false;
                }
                wtapTicks--;
            }
        }

        if (mode.getValue() == Mode.Silent) {
            if (shouldWTap && wtapTicks > 0) {
                if (wtapTicks == 2) {
                    if (!mc.gameSettings.keyBindForward.isPressed()) return;
                    mc.thePlayer.setSprinting(false);
                } else if (wtapTicks == 1) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.serverSprintState = true;
                    mc.thePlayer.setSprinting(true);
                    shouldWTap = false;
                }
                wtapTicks--;
            }
        }

        if (mc.thePlayer.isSwingInProgress && mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {

            Entity target = mc.objectMouseOver.entityHit;

            if (target instanceof EntityPlayer) {
                int chance = Math.max(0, Math.min(100, wtapChance.getValue().intValue()));
                if (mc.theWorld.rand.nextInt(100) < chance) {
                    if (!onlyWithKillaura.getValue() || isKillauraActive()) {
                        triggerWTap();
                    }
                }
            }
        }
    };

    private void triggerWTap() {
        shouldWTap = true;
        wtapTicks = 2;
    }

    private boolean isKillauraActive() {
        Module killAura = Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);
        return killAura != null && killAura.isEnabled();
    }

    @Override
    public void onEnable() {
        shouldWTap = false;
        wtapTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        shouldWTap = false;
        wtapTicks = 0;
        super.onDisable();
    }
}