package cc.simp.modules;

import cc.simp.Simp;
import cc.simp.event.impl.KeyPressEvent;
import cc.simp.modules.impl.client.*;
import cc.simp.modules.impl.combat.AntiKnockbackModule;
import cc.simp.modules.impl.combat.BackTrackModule;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.modules.impl.combat.TimerRangeModule;
import cc.simp.modules.impl.exploit.AntiCheatDisablerModule;
import cc.simp.modules.impl.exploit.BedDestroyerModule;
import cc.simp.modules.impl.exploit.InventoryMovementModule;
import cc.simp.modules.impl.movement.*;
import cc.simp.modules.impl.player.*;
import cc.simp.modules.impl.render.AmbienceModule;
import cc.simp.modules.impl.render.BlockAnimationsModule;
import cc.simp.modules.impl.render.ChamsModule;
import cc.simp.modules.impl.render.MotionBlurModule;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final ImmutableClassToInstanceMap<Module> instanceMap;

    public ModuleManager() {
        instanceMap = putInInstanceMap(

                // COMBAT
                new KillAuraModule(),
                new AntiKnockbackModule(),
                new BackTrackModule(),
                new TimerRangeModule(),

                // PLAYER
                new ScaffoldModule(),
                new NoFallDamageModule(),
                new ChestStealerModule(),
                new InventoryManagerModule(),
                new AutoToolModule(),
                new ClientRotationsModule(),

                // MOVEMENT
                new SprintModule(),
                new SpeedModule(),
                new FlightModule(),
                new NoSlowdownModule(),
                new MovementCorrectionModule(),

                // EXPLOIT
                new AntiCheatDisablerModule(),
                new BedDestroyerModule(),
                new InventoryMovementModule(),

                // RENDER
                new BlockAnimationsModule(),
                new AmbienceModule(),
                new ChamsModule(),
                new MotionBlurModule(),

                // CLIENT
                new WatermarkModule(),
                new ArraylistModule(),
                new PlayerInfoModule(),
                new TargetHUDModule(),
                new TabGUIModule(),
                new FontManagerModule(),
                new ClickGUIModule()

        );
        getModules().forEach(Module::reflectProperties);

        getModules().forEach(Module::resetPropertyValues);

        Simp.INSTANCE.getEventBus().subscribe(this);
    }

    @EventLink
    public final Listener<KeyPressEvent> onKeyPress = event -> {
        final int keyPressed = event.getKey();
        for (final Module module : this.getModules()) {
            final int moduleBind = module.getKey();
            if (moduleBind == keyPressed) {
                module.toggle();
            }
        }
    };

    public void postInit() {
        getModules().forEach(Module::resetPropertyValues);
    }

    private ImmutableClassToInstanceMap<Module> putInInstanceMap(Module... modules) {
        ImmutableClassToInstanceMap.Builder<Module> modulesBuilder = ImmutableClassToInstanceMap.builder();
        Arrays.stream(modules).forEach(module -> modulesBuilder.put((Class<Module>) module.getClass(), module));
        return modulesBuilder.build();
    }

    public Collection<Module> getModules() {
        return instanceMap.values();
    }

    public <T extends Module> T getModule(Class<T> moduleClass) {
        return instanceMap.getInstance(moduleClass);
    }

    public Module getModule(String label) {
        return getModules().stream().filter(module -> module.getLabel().replaceAll(" ", "").equalsIgnoreCase(label)).findFirst().orElse(null);
    }

    public static <T extends Module> T getInstance(Class<T> clazz) {
        return Simp.INSTANCE.getModuleManager().getModule(clazz);
    }

    public List<Module> getModulesForCategory(ModuleCategory category) {
        return getModules().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

}
