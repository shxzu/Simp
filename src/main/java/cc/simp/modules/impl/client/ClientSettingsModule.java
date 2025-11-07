package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.interfaces.click.ClickInterface;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import static cc.simp.utils.Util.mc;


@ModuleInfo(label = "Client Settings", category = ModuleCategory.CLIENT)
public final class ClientSettingsModule extends Module {

    public static final ModeProperty<Font> font = new ModeProperty<>("Font", Font.MC);

    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.Simp);

    public static final ModeProperty<Anime> anime = new ModeProperty<>("Anime", Anime.Onikata);
    public static final Property<Boolean> showInGame = new Property<>("Show In-Game", false, () -> anime.getValue() != Anime.None);
    public static final Property<Boolean> showInInventory = new Property<>("Show In Inventory", true, () -> anime.getValue() != Anime.None);

    public enum Anime {
        Onikata("Onikata"),
        Takanashi("Takanashi"),
        Io("Io"),
        ZeroTwo("Zero Two"),
        Astolfo("Astolfo"),
        Felix("Felix"),
        Rem("Rem"),
        Ram("Ram"),
        None("None");

        public String name;

        Anime(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Font {
        Simp,
        Bold,
        Noto,
        Arial,
        Apple,
        Sans,
        MC
    }

    public enum Color {
        Rainbow,
        Astolfo,
        Simp,
        Tenacity,
        FDP,
        Rise,
        Vaporwave,
        Sunset,
        White,
        Red,
        Purple,
        Pink
    }

    public ClientSettingsModule() {
        toggle();
        setHidden(true);
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();
        if (ClientSettingsModule.showInGame.getValue() && mc.thePlayer != null && mc.currentScreen == null) {
            switch (ClientSettingsModule.anime.getValue()) {
                case Onikata:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/onikata.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Takanashi:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/takanashi.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Io:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/io.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case ZeroTwo:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/zerotwo.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Astolfo:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/astolfo.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Felix:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/felix.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Rem:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/rem.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case Ram:
                    RenderUtils.drawImage(new ResourceLocation("simp/images/ram.png"), width - 216, (float) height / 2, 216, 289);
                    break;
                case None:
                    // No background
                    break;
            }
        }
    };
}
