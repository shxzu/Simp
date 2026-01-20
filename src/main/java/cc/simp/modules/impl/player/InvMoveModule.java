package cc.simp.modules.impl.player;

import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.mc.InventoryUtils;
import cc.simp.utils.mc.PacketUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Inventory Movement", category = ModuleCategory.PLAYER)
public class InvMoveModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Normal);

    private enum Mode {
        Normal,
        Hypixel
    }

    private boolean sentFirstOpen = false;
    private boolean failedClientStatus = false;
    private boolean failedCloseWindow = false;

    KeyBinding[] moveKeys = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump};;

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = event -> {
        setSuffix(mode.getValue().toString());
        if (!(mc.currentScreen instanceof GuiChat)) {
            KeyBinding[] keyBindingArray = this.moveKeys;
            int n = this.moveKeys.length;
            int n2 = 0;
            while (n2 < n) {
                KeyBinding bind = keyBindingArray[n2];
                KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown((int)bind.getKeyCode()));
                ++n2;
            }

            if (mode.getValue() == Mode.Hypixel && (mc.currentScreen instanceof GuiInventory || InventoryUtils.isInventoryOpen)) {
                if (!sentFirstOpen) {
                    PacketUtils.sendSilentPacket(new C0DPacketCloseWindow());
                    sentFirstOpen = true;
                }

                int safePacketTick = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 3 : 4;

                if (mc.thePlayer.ticksExisted % safePacketTick == 0) {
                    PacketUtils.sendSilentPacket(new C0DPacketCloseWindow());
                } else if (mc.thePlayer.ticksExisted % safePacketTick == 1) {
                    PacketUtils.sendSilentPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                }
            } else {
                sentFirstOpen = false;
            }
        }
    };


    @EventLink
    private final Listener<PacketReceiveEvent> packetReceiveEventListener = event -> {
        if (mode.getValue() == Mode.Hypixel) {
            if (event.getPacket() instanceof C16PacketClientStatus) {
                if (this.failedClientStatus) {
                    event.setCancelled();
                }
                this.failedClientStatus = true;
            }
            if (event.getPacket() instanceof C0DPacketCloseWindow) {
                if (this.failedCloseWindow) {
                    event.setCancelled();
                }
                this.failedCloseWindow = true;
            }
        }
    };


    @Override
    public void onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward) || mc.currentScreen != null) {
            mc.gameSettings.keyBindForward.setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindBack) || mc.currentScreen != null) {
            mc.gameSettings.keyBindBack.setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight) || mc.currentScreen != null) {
            mc.gameSettings.keyBindRight.setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft) || mc.currentScreen != null) {
            mc.gameSettings.keyBindLeft.setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindJump) || mc.currentScreen != null) {
            mc.gameSettings.keyBindJump.setPressed(false);
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSprint) || mc.currentScreen != null) {
            mc.gameSettings.keyBindSprint.setPressed(false);
        }
        this.sentFirstOpen = false;
        this.failedClientStatus = false;
        this.failedCloseWindow = false;
        super.onDisable();
    }
}
