package cc.simp.utils.client;

import net.minecraft.util.ResourceLocation;

public class BgUtils {
    private static BgUtils instance;

    private final ResourceLocation[] backgroundImages;
    private int currentBackgroundIndex = 0;

    private BgUtils() {
        backgroundImages = new ResourceLocation[]{
                new ResourceLocation("simp/images/mainmenu.jpg"),
                new ResourceLocation("simp/images/mainmenu2.png"),
                new ResourceLocation("simp/images/mainmenu3.jpg"),
                new ResourceLocation("simp/images/mainmenu4.png")
        };
    }

    public static BgUtils getInstance() {
        if (instance == null) {
            instance = new BgUtils();
        }
        return instance;
    }

    public ResourceLocation getCurrentBackground() {
        return backgroundImages[currentBackgroundIndex];
    }

    public void cycleBackground() {
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.length;
    }

    public int getCurrentIndex() {
        return currentBackgroundIndex;
    }
}

