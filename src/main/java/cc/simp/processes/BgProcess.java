package cc.simp.processes;

import net.minecraft.util.ResourceLocation;

public class BgProcess {
    private static BgProcess instance;

    private final ResourceLocation[] backgroundImages;
    private int currentBackgroundIndex = 0;

    private BgProcess() {
        backgroundImages = new ResourceLocation[]{
                new ResourceLocation("simp/images/mainmenu.jpg"),
                new ResourceLocation("simp/images/mainmenu2.jpg"),
                new ResourceLocation("simp/images/mainmenu3.jpg")
        };
    }

    public static BgProcess getInstance() {
        if (instance == null) {
            instance = new BgProcess();
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

