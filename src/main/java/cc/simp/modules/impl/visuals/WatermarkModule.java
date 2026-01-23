package cc.simp.modules.impl.visuals;

import cc.simp.Simp;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.player.ScaffoldModule;
import cc.simp.modules.impl.player.StealerModule;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Watermark", category = ModuleCategory.VISUALS)
public final class WatermarkModule extends Module {

    public static final ModeProperty<Type> type = new ModeProperty<>("Client Watermark Type", Type.Simple);
    public static final Property<Boolean> info = new Property<>("Watermark Info", true, () -> type.getValue() != Type.GameSense && type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst && type.getValue() != Type.Nursultan);
    public static final Property<String> name = new Property<>("Client Name", "Simp");

    public static String customName = "Simp";

    public enum Type {
        Simple,
        Exhibition,
        Astolfo,
        GameSense,
        Logo,
        Island,
        Tutorial2017,
        Wurst,
        Nursultan
    }

    private long lastServerTime;
    private long lastUpdateTime;
    private String currentServer;
    private float islandWidth;
    private float islandHeight;
    private ResourceLocation clientLogo;
    private ResourceLocation wurstLogo = new ResourceLocation("simp/images/wurst.png");
    private float stealerWidth = 200f;
    private float stealerHeight = 28f;
    private final java.util.Map<Integer, ChestItemAnimation> chestItemAnimations = new java.util.HashMap<>();
    private int lastChestSize = 0;
    private boolean wasInChest = false;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        setSuffix(type.getValue().toString());
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        customName = name.getValue();
        String clientName = customName;

        if (type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            String text = clientName;
            if (type.getValue() == Type.Exhibition) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " ";
                    if (info.getValue())
                        text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.Astolfo) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.WHITE + clientName.substring(1) + " ";
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.Simple) {
                if (info.getValue()) {
                    text = clientName + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
                } else {
                    text = clientName;
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.GameSense) {
                String serverInfo = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy") ? "LiquidProxy" : mc.getCurrentServerData().serverIP : "Singleplayer";
                text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                        clientName, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
                RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
                fr.drawStringWithShadow(text, 2, 3, ColorProcess.getColor().getRGB());
            }

        } else if (type.getValue() == Type.Logo) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/simp_light.png"), 2, 2, (float) 157 / 2, (float) 125 / 2);

        } else if (type.getValue() == Type.Island) {
            renderDynamicIsland(sr, clientName);

        } else if (type.getValue() == Type.Tutorial2017) {
            RenderUtils.drawRect(2, 2, fr.getStringWidth(clientName) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString(clientName, 4, 4, 0x5555FF);
            RenderUtils.drawRect(2, 15, fr.getStringWidth("FPS: " + mc.getDebugFPS()) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString("FPS: " + mc.getDebugFPS(), 4, 17, -1);

        } else if (type.getValue() == Type.Wurst) {
            RenderUtils.drawRect(0, 10, 223, 21, new Color(255, 255, 255, 100));
            RenderUtils.drawImage(wurstLogo, 0, 10f, 758 / 8.5f, 192 / 8.5f);
            mc.fontRendererObj.drawString("v7.46.1" + " MC1.8.9 (outdated)",  92, 17, Color.BLACK.getRGB());

        }

        if (type.getValue() == Type.Nursultan) {
            int ping = 67;
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (playerInfo != null && playerInfo.getResponseTime() != 0) {
                ping = playerInfo.getResponseTime();
            }
            String skibidi = clientName + EnumChatFormatting.WHITE + " - " + mc.getDebugFPS() + " FPS" + " - " + ping + "ms";
            RenderUtils.drawRoundOutline(2, 2, fr.getStringWidth(skibidi) + 4, fr.FONT_HEIGHT + 4, 4, 0.05f, new Color(0, 0, 0, 130), ColorProcess.getColor());
            fr.drawStringWithShadow(skibidi, 4, 4, ColorProcess.getColor().getRGB());
        }

    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);
        String clientName = customName;

        if (type.getValue() != Type.Logo && type.getValue() != Type.Island && type.getValue() != Type.Tutorial2017 && type.getValue() != Type.Wurst) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm a");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            String text = clientName;
            if (type.getValue() == Type.Exhibition) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " ";
                    if (info.getValue())
                        text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.GRAY + clientName.substring(1) + " " + EnumChatFormatting.WHITE + Simp.VERSION + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + strDate + EnumChatFormatting.GRAY + "]" + EnumChatFormatting.GRAY + " [FPS: " + EnumChatFormatting.WHITE + mc.getDebugFPS() + EnumChatFormatting.GRAY + "]";
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.Astolfo) {
                if (!clientName.isEmpty()) {
                    text = String.valueOf(clientName.charAt(0)) + EnumChatFormatting.WHITE + clientName.substring(1) + " ";
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.Simple) {
                if (info.getValue()) {
                    text = clientName + EnumChatFormatting.WHITE + " " + EnumChatFormatting.WHITE + Simp.VERSION;
                } else {
                    text = clientName;
                }
                fr.drawStringWithShadow(text, 2, 2, ColorProcess.getColor().getRGB());
            } else if (type.getValue() == Type.GameSense) {
                String serverInfo = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy") ? "LiquidProxy" : mc.getCurrentServerData().serverIP : "Singleplayer";
                text = String.format(EnumChatFormatting.WHITE + "%s v%s | %d FPS | %s",
                        clientName, Simp.VERSION, Minecraft.getDebugFPS(), serverInfo);
                RenderUtils.drawBorderedRect(0, 0.5f, fr.getStringWidth(text) + 4, 7 * sr.getScaleFactor(), 2, new Color(0, 0, 0, 100).getRGB(), ColorProcess.getColor().getRGB(), true, false, false, false);
                fr.drawStringWithShadow(text, 2, 3, ColorProcess.getColor().getRGB());
            }
        } else if (type.getValue() == Type.Logo) {
            RenderUtils.drawImage(new ResourceLocation("simp/images/simp_light.png"), 2, 2, (float) 157 / 2, (float) 125 / 2);
        } else if (type.getValue() == Type.Island) {
            renderDynamicIsland(sr, clientName);
        } else if (type.getValue() == Type.Tutorial2017) {
            RenderUtils.drawRect(2, 2, fr.getStringWidth(clientName) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString(clientName, 4, 4, 0x5555FF);
            RenderUtils.drawRect(2, 15, fr.getStringWidth("FPS: " + mc.getDebugFPS()) + 4, fr.FONT_HEIGHT + 2, new Color(0, 0, 0, 100));
            fr.drawString("FPS: " + mc.getDebugFPS(), 4, 17, -1);
        } else if (type.getValue() == Type.Wurst) {
            RenderUtils.drawRect(0, 10, 223, 21, new Color(255, 255, 255, 100));
            RenderUtils.drawImage(wurstLogo, 0, 10f, 758 / 8.5f, 192 / 8.5f);
            mc.fontRendererObj.drawString("v7.46.1" + " MC1.8.9 (outdated)",  92, 17, Color.BLACK.getRGB());
        }
        if (type.getValue() == Type.Nursultan) {
            int ping = 67;
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (playerInfo != null && playerInfo.getResponseTime() != 0) {
                ping = playerInfo.getResponseTime();
            }
            String skibidi = clientName + EnumChatFormatting.WHITE + " - " + mc.getDebugFPS() + " FPS" + " - " + ping + "ms";
            RenderUtils.drawRoundOutline(2, 2, fr.getStringWidth(skibidi) + 4, fr.FONT_HEIGHT + 4, 6, 0.05f, new Color(0, 0, 0, 130), ColorProcess.getColor());
            fr.drawStringWithShadow(skibidi, 4, 4, ColorProcess.getColor().getRGB());
        }
    };

    private void renderDynamicIsland(ScaledResolution sr, String clientName) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();
        if (System.currentTimeMillis() - lastServerTime > 5000) {
            updateServerInfo();
            lastServerTime = System.currentTimeMillis();
        }

        int centerX = sr.getScaledWidth() / 2;
        int yPos = 8;

        // Check if Stealer is enabled and chest is open
        StealerModule stealer = Simp.INSTANCE.getModuleManager().getModule(StealerModule.class);
        boolean stealerActive = stealer != null && stealer.isEnabled() && mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest;

        // Check if Scaffold is enabled
        ScaffoldModule scaffold = Simp.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        boolean scaffoldEnabled = scaffold != null && scaffold.isEnabled();

        if (stealerActive) {
            renderStealerIsland(sr, centerX, yPos);
        } else if (scaffoldEnabled) {
            renderScaffoldIsland(sr, centerX, yPos);
        } else {
            renderNormalIsland(sr, centerX, yPos, clientName);
        }
    }


    private void renderNormalIsland(ScaledResolution sr, int centerX, int yPos, String clientName) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();

        String serverText = currentServer != null ? currentServer : "Loading...";
        String versionText = "v" + Simp.VERSION;
        String fpsText = mc.getDebugFPS() + " fps";

        int contentPadding = 12;
        int logoSize = 30;
        int textSpacing = 8;

        int dotWidth = fr.getStringWidth("•");
        int textWidth = fr.getStringWidth(serverText) +
                textSpacing + dotWidth +
                textSpacing + fr.getStringWidth(versionText) +
                textSpacing + dotWidth +
                textSpacing + fr.getStringWidth(fpsText);

        int totalWidth = logoSize + contentPadding * 2 + textWidth + 8;
        int height = 28;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        float smoothTime = 0.2f;
        float alpha = 1.0f - (float) Math.exp(-deltaTime / smoothTime);

        islandHeight = RenderUtils.lerp(islandHeight, height, alpha);
        islandWidth = RenderUtils.lerp(islandWidth, totalWidth, alpha);

        float startX = centerX - islandWidth / 2;

        RenderUtils.drawRoundedRect(startX, yPos, islandWidth, islandHeight, 14f, new Color(20, 20, 25, 200));
        RenderUtils.drawRoundOutline(startX, yPos, islandWidth, islandHeight, 14f, 0.5f,
                new Color(0, 0, 0, 0), new Color(255, 255, 255, 30));

        clientLogo = new ResourceLocation("simp/images/simp_light.png");
        float logoX = startX + contentPadding;
        float logoWidth = logoSize;
        float logoHeight = logoSize / 1.256f;
        float logoY = yPos + (islandHeight - logoHeight) / 2;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderUtils.drawImage(clientLogo, logoX, logoY, logoWidth, logoHeight);
        GlStateManager.disableBlend();

        float textX = logoX + logoSize + 10;
        float textY = yPos + (islandHeight - fr.FONT_HEIGHT) / 1.8f;

        fr.drawString(serverText, (int) textX, (int) textY, ColorProcess.getColor().getRGB());
        textX += fr.getStringWidth(serverText) + textSpacing;

        fr.drawString("•", (int) textX, (int) textY, new Color(100, 100, 110).getRGB());
        textX += dotWidth + textSpacing;

        fr.drawString(versionText, (int) textX, (int) textY, new Color(150, 150, 160).getRGB());
        textX += fr.getStringWidth(versionText) + textSpacing;

        fr.drawString("•", (int) textX, (int) textY, new Color(100, 100, 110).getRGB());
        textX += dotWidth + textSpacing;

        fr.drawString(fpsText, (int) textX, (int) textY, new Color(180, 180, 190).getRGB());
    }

    private float scaffoldWidth = 200f;
    private float scaffoldHeight = 28f;
    private int lastBlockCount = 0;
    private float blockCountDisplay = 0;
    private long lastBPSUpdate = 0;
    private float currentBPS = 0;
    private float displayBPS = 0;

    private void renderScaffoldIsland(ScaledResolution sr, int centerX, int yPos) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();

        // Get block count - count ALL blocks first to establish the total
        int currentBlockCount = 0;
        int totalBlocks = 0;

        for (int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++) {
            net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemBlock) {
                totalBlocks += stack.stackSize;
            }
        }

        // If holding blocks, get current stack size
        if (mc.thePlayer.inventory.getCurrentItem() != null &&
                mc.thePlayer.inventory.getCurrentItem().getItem() instanceof net.minecraft.item.ItemBlock) {
            currentBlockCount = totalBlocks; // Current total is what we have now
        } else {
            currentBlockCount = totalBlocks;
        }

        // Calculate BPS
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBPSUpdate > 50) {
            double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
            double dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
            double distance = Math.sqrt(dx * dx + dz * dz) * 20;
            currentBPS = (float) distance;
            lastBPSUpdate = currentTime;
        }

        // Smooth animations
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        float smoothTime = 0.15f;
        float alpha = 1.0f - (float) Math.exp(-deltaTime / smoothTime);

        blockCountDisplay = RenderUtils.lerp(blockCountDisplay, currentBlockCount, alpha * 2);

        // Track initial total for progress calculation
        if (lastBlockCount == 0 || currentBlockCount > lastBlockCount) {
            lastBlockCount = currentBlockCount;
        }

        displayBPS = RenderUtils.lerp(displayBPS, currentBPS, alpha);

        // Calculate dimensions - match normal island logo size
        int contentPadding = 12;
        int logoSize = 30; // Match normal island
        int textSpacing = 8;
        int progressBarWidth = 80;
        int progressBarHeight = 4;

        String blocksText = (int) blockCountDisplay + "/" + lastBlockCount;
        String bpsText = String.format("%.1f BPS", displayBPS);

        int dotWidth = fr.getStringWidth("•");
        int textWidth = logoSize + 10 + // logo + spacing (matching normal island)
                fr.getStringWidth(blocksText) + textSpacing +
                progressBarWidth + textSpacing +
                dotWidth + textSpacing +
                fr.getStringWidth(bpsText);

        int targetWidth = contentPadding * 2 + textWidth + 8;
        int targetHeight = 36;

        scaffoldHeight = RenderUtils.lerp(scaffoldHeight, targetHeight, alpha);
        scaffoldWidth = RenderUtils.lerp(scaffoldWidth, targetWidth, alpha);

        float startX = centerX - scaffoldWidth / 2;

        // Background
        RenderUtils.drawRoundedRect(startX, yPos, scaffoldWidth, scaffoldHeight, 14f,
                new Color(20, 20, 25, 200));
        RenderUtils.drawRoundOutline(startX, yPos, scaffoldWidth, scaffoldHeight, 14f, 0.5f,
                new Color(0, 0, 0, 0), new Color(255, 255, 255, 30));

        // Draw logo - match normal island size
        clientLogo = new ResourceLocation("simp/images/simp_light.png");
        float logoX = startX + contentPadding;
        float logoWidth = logoSize;
        float logoHeight = logoSize / 1.256f;
        float logoY = yPos + (scaffoldHeight - logoHeight) / 2;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderUtils.drawImage(clientLogo, logoX, logoY, logoWidth, logoHeight);
        GlStateManager.disableBlend();

        // Text content
        float textX = logoX + logoSize + 10; // Match normal island spacing
        float textY = yPos + (scaffoldHeight - fr.FONT_HEIGHT) / 1.8f;

        // Block count
        fr.drawString(blocksText, (int) textX, (int) textY, ColorProcess.getColor().getRGB());
        textX += fr.getStringWidth(blocksText) + textSpacing;

        // Progress bar - calculate based on current vs initial total
        float progressY = textY + fr.FONT_HEIGHT / 2 - progressBarHeight / 2;
        float progress = lastBlockCount > 0 ? (float) currentBlockCount / lastBlockCount : 0;
        progress = Math.max(0, Math.min(1, progress)); // Clamp between 0 and 1

        // Progress bar background
        RenderUtils.drawRoundedRect(textX, progressY, progressBarWidth, progressBarHeight, 2f,
                new Color(40, 40, 45, 180));

        // Progress bar fill
        if (progress > 0) {
            float fillWidth = progressBarWidth * progress;
            Color progressColor = progress < 0.2f ?
                    new Color(255, 100, 100) :
                    progress < 0.5f ?
                            new Color(255, 255, 100) :
                            ColorProcess.getColor();

            RenderUtils.drawRoundedRect(textX, progressY, fillWidth, progressBarHeight, 2f, progressColor);
        }

        textX += progressBarWidth + textSpacing;

        // Separator dot
        fr.drawString("•", (int) textX, (int) textY, new Color(100, 100, 110).getRGB());
        textX += dotWidth + textSpacing;

        // BPS
        Color bpsColor = displayBPS > 8 ?
                new Color(100, 255, 100) :
                displayBPS > 4 ?
                        new Color(255, 255, 100) :
                        new Color(180, 180, 190);
        fr.drawString(bpsText, (int) textX, (int) textY, bpsColor.getRGB());
    }

    private void renderStealerIsland(ScaledResolution sr, int centerX, int yPos) {
        CustomFontRenderer fr = FontProcess.getCurrentFont();

        if (!(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest)) {
            wasInChest = false;
            chestItemAnimations.clear();
            return;
        }

        net.minecraft.client.gui.inventory.GuiChest guiChest = (net.minecraft.client.gui.inventory.GuiChest) mc.currentScreen;
        net.minecraft.inventory.ContainerChest chest = (net.minecraft.inventory.ContainerChest) mc.thePlayer.openContainer;
        net.minecraft.inventory.IInventory inventory = chest.getLowerChestInventory();

        int chestSize = inventory.getSizeInventory();
        boolean isDoubleChest = chestSize > 27;
        int rows = isDoubleChest ? 6 : 3;
        int columns = 9;

        // Reset animations when chest changes
        if (!wasInChest || lastChestSize != chestSize) {
            chestItemAnimations.clear();
            lastChestSize = chestSize;
        }
        wasInChest = true;

        // Update animations
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        float smoothTime = 0.15f;
        float alpha = 1.0f - (float) Math.exp(-deltaTime / smoothTime);

        // Track item changes
        for (int i = 0; i < chestSize; i++) {
            net.minecraft.item.ItemStack stack = inventory.getStackInSlot(i);

            if (!chestItemAnimations.containsKey(i)) {
                if (stack != null) {
                    chestItemAnimations.put(i, new ChestItemAnimation(stack));
                }
            } else {
                ChestItemAnimation anim = chestItemAnimations.get(i);
                if (stack == null && anim.lastStack != null) {
                    // Item was removed, start fade out animation
                    anim.removing = true;
                } else if (stack != null) {
                    anim.lastStack = stack;
                    anim.removing = false;
                    anim.opacity = 1.0f;
                    anim.scale = 1.0f;
                }
            }
        }

        // Update and clean up animations
        java.util.Iterator<java.util.Map.Entry<Integer, ChestItemAnimation>> iterator =
                chestItemAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Integer, ChestItemAnimation> entry = iterator.next();
            ChestItemAnimation anim = entry.getValue();

            if (anim.removing) {
                anim.opacity = Math.max(0, anim.opacity - deltaTime * 4);
                anim.scale = Math.min(1.5f, anim.scale + deltaTime * 3);

                if (anim.opacity <= 0) {
                    iterator.remove();
                }
            }
        }

        // Calculate dimensions
        int contentPadding = 12;
        int logoSize = 30;
        int textSpacing = 8;
        int itemSize = 16;
        int itemSpacing = 2;

        String chestName = inventory.getDisplayName().getFormattedText();
        if (chestName.length() > 20) {
            chestName = chestName.substring(0, 20) + "...";
        }

        int chestGridWidth = (columns * itemSize) + ((columns - 1) * itemSpacing);
        int chestGridHeight = (rows * itemSize) + ((rows - 1) * itemSpacing);

        int textWidth = Math.max(fr.getStringWidth(chestName), chestGridWidth);
        int targetWidth = contentPadding * 2 + logoSize + 10 + textWidth + 8;
        int targetHeight = contentPadding * 2 + Math.max(logoSize, fr.FONT_HEIGHT + 8 + chestGridHeight);

        stealerHeight = RenderUtils.lerp(stealerHeight, targetHeight, alpha);
        stealerWidth = RenderUtils.lerp(stealerWidth, targetWidth, alpha);

        float startX = centerX - stealerWidth / 2;

        // Background
        RenderUtils.drawRoundedRect(startX, yPos, stealerWidth, stealerHeight, 14f,
                new Color(20, 20, 25, 200));
        RenderUtils.drawRoundOutline(startX, yPos, stealerWidth, stealerHeight, 14f, 0.5f,
                new Color(0, 0, 0, 0), new Color(255, 255, 255, 30));

        // Draw logo
        clientLogo = new ResourceLocation("simp/images/simp_light.png");
        float logoX = startX + contentPadding;
        float logoWidth = logoSize;
        float logoHeight = logoSize / 1.256f;
        float logoY = yPos + contentPadding;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderUtils.drawImage(clientLogo, logoX, logoY, logoWidth, logoHeight);
        GlStateManager.disableBlend();

        // Draw chest name
        float textX = logoX + logoSize + 10;
        float textY = yPos + contentPadding;

        fr.drawString(chestName, (int) textX, (int) textY, ColorProcess.getColor().getRGB());

        // Draw chest grid
        float gridX = textX;
        float gridY = textY + fr.FONT_HEIGHT + 6;

        // Center the grid if it's smaller than text
        if (chestGridWidth < fr.getStringWidth(chestName)) {
            gridX += (fr.getStringWidth(chestName) - chestGridWidth) / 2;
        }

        renderChestGrid(inventory, gridX, gridY, rows, columns, itemSize, itemSpacing);
    }

    private void renderChestGrid(net.minecraft.inventory.IInventory inventory, float startX, float startY,
                                 int rows, int columns, int itemSize, int itemSpacing) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableRescaleNormal();
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotIndex = row * columns + col;
                if (slotIndex >= inventory.getSizeInventory()) break;

                float slotX = startX + (col * (itemSize + itemSpacing));
                float slotY = startY + (row * (itemSize + itemSpacing));

                // Draw slot background
                RenderUtils.drawRoundedRect(slotX, slotY, itemSize, itemSize, 2f,
                        new Color(30, 30, 35, 150));

                // Get item and animation
                net.minecraft.item.ItemStack stack = inventory.getStackInSlot(slotIndex);
                ChestItemAnimation anim = chestItemAnimations.get(slotIndex);

                // Render item with animation
                if (anim != null && anim.lastStack != null) {
                    GlStateManager.pushMatrix();

                    float centerX = slotX + itemSize / 2f;
                    float centerY = slotY + itemSize / 2f;

                    GlStateManager.translate(centerX, centerY, 0);
                    GlStateManager.scale(anim.scale, anim.scale, 1);
                    GlStateManager.translate(-centerX, -centerY, 0);

                    GlStateManager.color(1.0f, 1.0f, 1.0f, anim.opacity);

                    try {
                        mc.getRenderItem().renderItemAndEffectIntoGUI(anim.lastStack,
                                (int) slotX, (int) slotY);

                        // Draw stack size if > 1
                        if (anim.lastStack.stackSize > 1) {
                            String stackSize = String.valueOf(anim.lastStack.stackSize);
                            GlStateManager.disableLighting();
                            GlStateManager.disableDepth();
                            GlStateManager.disableBlend();

                            mc.fontRendererObj.drawStringWithShadow(stackSize,
                                    slotX + itemSize - mc.fontRendererObj.getStringWidth(stackSize) - 1,
                                    slotY + itemSize - 7,
                                    (int) (255 * anim.opacity) << 24 | 0xFFFFFF);

                            GlStateManager.enableLighting();
                            GlStateManager.enableDepth();
                            GlStateManager.enableBlend();
                        }
                    } catch (Exception e) {
                        // Silently handle render errors
                    }

                    GlStateManager.popMatrix();
                    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }

        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    private void updateServerInfo() {
        currentServer = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy") ? "LiquidProxy" : mc.getCurrentServerData().serverIP : "Singleplayer";
    }

    private static class ChestItemAnimation {
        float opacity = 1.0f;
        float scale = 1.0f;
        boolean removing = false;
        net.minecraft.item.ItemStack lastStack;

        ChestItemAnimation(net.minecraft.item.ItemStack stack) {
            this.lastStack = stack;
        }
    }
}