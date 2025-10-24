package cc.simp.modules;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.KeyPressEvent;
import cc.simp.modules.impl.client.ClickInterfaceModule;
import cc.simp.modules.impl.client.DisablerModule;
import cc.simp.modules.impl.combat.*;
import cc.simp.modules.impl.movement.FlightModule;
import cc.simp.modules.impl.movement.NoSlowModule;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.modules.impl.movement.SprintModule;
import cc.simp.modules.impl.player.*;
import cc.simp.modules.impl.visuals.*;
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

                // Combat
                new KillAuraModule(),
                new TPAuraModule(),
                new TickBaseModule(),
                new LagRangeModule(),
                new VelocityModule(),
                new BackTrackModule(),
                new WTapModule(),
                new AutoClickerModule(),
                new AimAssistModule(),
                new AutoRodModule(),
                new NoHitDelayModule(),

                // Movement
                new SprintModule(),
                new SpeedModule(),
                new FlightModule(),
                new NoSlowModule(),

                // Player
                new ScaffoldModule(),
                new NoFallModule(),
                new AutoToolModule(),
                new StealerModule(),
                new ManagerModule(),
                new AutoArmorModule(),
                new InvMoveModule(),
                new LegitScaffoldModule(),

                // Client
                new ClickInterfaceModule(),
                new DisablerModule(),

                // Visuals
                new ArrayListModule(),
                new WatermarkModule(),
                new CameraModule(),
                new ESPModule(),
                new TargetInterfaceModule(),
                new PostProcessingModule(),
                new NickHiderModule(),
                new NameTagsModule(),
                new StorageESPModule(),
                new ChinaHatModule(),
                new FullBrightModule(),
                new ItemESPModule()
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
            if (moduleBind == 0)
                continue;
            if (moduleBind == keyPressed) {
                module.toggle();
                return;
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