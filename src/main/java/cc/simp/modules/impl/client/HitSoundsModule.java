package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.game.EntityHurtSoundEvent;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.client.SoundUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

@ModuleInfo(label = "Hit Sounds", category = ModuleCategory.CLIENT)
public class HitSoundsModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Skeet);

    public enum Mode {
        Skeet("Skeet"),
        Felix("Felix"),
        UltraKill("UltraKill"),
        OniChan("Oni Chan");

        public String name;
        Mode(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    @EventLink
    public final Listener<EntityHurtSoundEvent> entityHurtSoundEventListener = event -> {

        event.setCancelled();

        switch (mode.getValue()) {
            case Skeet:
                SoundUtils.playSound("skeet.wav");
                break;
            case Felix:
                SoundUtils.playSound("felix.wav");
                break;
            case UltraKill:
                SoundUtils.playSound("ultrakill.wav");
                break;
            case OniChan:
                SoundUtils.playSound("onichan.wav");
                break;
        }
    };

}
