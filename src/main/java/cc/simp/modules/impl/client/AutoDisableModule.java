package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.modules.impl.player.AutoArmorModule;
import cc.simp.modules.impl.player.ManagerModule;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.modules.impl.player.StealerModule;
import cc.simp.utils.misc.Manager;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.init.Items;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Auto Disable", category = ModuleCategory.CLIENT)
public class AutoDisableModule extends Module {

    // pasted from old xiva. so wht? it worked fine twin.

    public Property<Boolean> disableKillAuraWhenScaffoldProperty = new Property<>("Disable Kill Aura On Scaffold", true);
    public Property<Boolean> disableSpeedOnScaffoldProperty = new Property<>("Disable Speed On Scaffold", true);
    public Property<Boolean> disableKillAuraProperty = new Property<>("Disable Kill Aura On Flag", false);
    public Property<Boolean> disableSpeedProperty = new Property<>("Disable Speed On Flag", false);
    public Property<Boolean> disableScaffoldProperty = new Property<>("Disable Scaffold On Flag", true);
    public Property<Boolean> disableInvManagerProperty = new Property<>("Disable Manager On Flag", true);
    public Property<Boolean> disableAutoArmorProperty = new Property<>("Disable AutoArmor On Flag", true);
    public Property<Boolean> disableChestStealerProperty = new Property<>("Disable Stealer On Flag", true);

    @EventLink
    private final Listener<PreUpdateEvent> preUpdateEventListener = e -> {
        if (disableKillAuraWhenScaffoldProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled() && Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).toggle();
        }

        if (disableSpeedOnScaffoldProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() && Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()) {
            Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).toggle();
        }
    };

    @EventLink
    private final Listener<PacketReceiveEvent> packetReceiveEventListener = e -> {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {

            // Check if disableKillAura is enabled and KillAura is toggled, then disable
            if (disableKillAuraProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class).toggle();
            }
            // Check if disableSpeed is enabled and Speed is toggled, then disable
            if (disableSpeedProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).toggle();
            }
            // Check if disableInvManager is enabled and InvManager is toggled, then disable
            if (disableInvManagerProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(ManagerModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(ManagerModule.class).toggle();
            }
            // Check if disableAutoArmor is enabled and AutoArmor is toggled, then disable
            if (disableAutoArmorProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(AutoArmorModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(AutoArmorModule.class).toggle();
            }
            // Check if disableChestStealer is enabled and ChestStealer is toggled, then disable
            if (disableChestStealerProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(StealerModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(StealerModule.class).toggle();
            }
            // Check if disableScaffold is enabled and Scaffold is toggled, then disable
            if (disableScaffoldProperty.getValue() && Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
                Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).toggle();
            }
        }
    };
}
