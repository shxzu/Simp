package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Client Settings", category = ModuleCategory.CLIENT)
public final class ClientSettingsModule extends Module {

    public static final ModeProperty<ClickInterface> clickInterface = new ModeProperty<>("Click Interface", ClickInterface.Normal);
    public static final ModeProperty<Font> font = new ModeProperty<>("Font", Font.MC);
    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.Simp);
    public static final ModeProperty<Anime> anime = new ModeProperty<>("Anime", Anime.Onikata);
    public static final Property<String> customAnimeUrl = new Property<>("Custom URL", "", () -> anime.getValue() == Anime.Custom);
    public static final Property<Boolean> showInGame = new Property<>("Show In-Game", false, () -> anime.getValue() != Anime.None);
    public static final Property<Boolean> showInInventory = new Property<>("Show In Inventory", true, () -> anime.getValue() != Anime.None);

    private static final Map<String, ResourceLocation> cachedImages = new HashMap<>();

    public enum ClickInterface {
        Normal,
        Window
    }

    public enum Anime {
        Onikata("Onikata"),
        Takanashi("Takanashi"),
        Io("Io"),
        ZeroTwo("Zero Two"),
        Astolfo("Astolfo"),
        Felix("Felix"),
        Rem("Rem"),
        Ram("Ram"),
        Custom("Custom"),
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
        Convection,
        MC
    }

    public enum Color {
        Rainbow,
        Exhibition,
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
        if (showInGame.getValue() && mc.thePlayer != null && mc.currentScreen == null) {
            ScaledResolution sr = new ScaledResolution(mc);
            renderAnimeImage(sr.getScaledWidth(), sr.getScaledHeight());
        }
    };

    public static ResourceLocation getAnimeImage(Anime animeType) {
        switch (animeType) {
            case Onikata:
                return new ResourceLocation("simp/images/onikata.png");
            case Takanashi:
                return new ResourceLocation("simp/images/takanashi.png");
            case Io:
                return new ResourceLocation("simp/images/io.png");
            case ZeroTwo:
                return new ResourceLocation("simp/images/zerotwo.png");
            case Astolfo:
                return new ResourceLocation("simp/images/astolfo.png");
            case Felix:
                return new ResourceLocation("simp/images/felix.png");
            case Rem:
                return new ResourceLocation("simp/images/rem.png");
            case Ram:
                return new ResourceLocation("simp/images/ram.png");
            case Custom:
                return loadCustomImage();
            case None:
            default:
                return null;
        }
    }

    public static void renderAnimeImage(int width, int height) {
        if (anime.getValue() == Anime.None) return;

        ResourceLocation imageResource = getAnimeImage(anime.getValue());
        if (imageResource == null) return;

        int[] dimensions = RenderUtils.getImageDimensions(imageResource);
        RenderUtils.drawImage(imageResource, width - dimensions[0] / 3f, (float) height / 3, dimensions[0] / 3f, dimensions[1] / 3f);
    }

    private static ResourceLocation loadCustomImage() {
        String url = customAnimeUrl.getValue();
        if (url == null || url.isEmpty()) return null;

        if (cachedImages.containsKey(url)) {
            return cachedImages.get(url);
        }

        new Thread(() -> {
            try {
                BufferedImage image = ImageIO.read(new URL(url));

                mc.addScheduledTask(() -> {
                    try {
                        DynamicTexture texture = new DynamicTexture(image);
                        ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation("custom_anime", texture);
                        cachedImages.put(url, location);
                    } catch (Exception e) {
                        System.err.println("Failed to create texture: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to load custom anime image: " + e.getMessage());
            }
        }).start();

        return null;
    }
}