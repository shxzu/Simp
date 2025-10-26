package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Name Tags", category = ModuleCategory.VISUALS)
public final class NameTagsModule extends Module {

    private final Property<Boolean> showArmor = new Property<>("Show Armor", true);
    private final Property<Boolean> showHealth = new Property<>("Show Health", true);
    private final Property<Boolean> background = new Property<>("Background", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.5, 2.0, 0.1);
    private final Property<Boolean> distance = new Property<>("Show Distance", false);

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if(AntiBotModule.botList.contains(player)) return;
            if (player == mc.thePlayer || player.isDead || player.isInvisible()) continue;

            renderNameTag(player);
        }
    };

    private void renderNameTag(EntityPlayer player) {
        double x = player.posX - mc.getRenderManager().renderPosX;
        double y = player.posY - mc.getRenderManager().renderPosY;
        double z = player.posZ - mc.getRenderManager().renderPosZ;

        y += player.height + 0.5;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1, 0, 0);

        float scaleFactor = (float) (0.02666667f * scale.getValue());
        GlStateManager.scale(-scaleFactor, -scaleFactor, scaleFactor);

        if (throughWalls.getValue()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        String name = player.getDisplayName().getFormattedText();
        float health = player.getHealth();
        int armorValue = player.getTotalArmorValue();
        double distanceToPlayer = mc.thePlayer.getDistanceToEntity(player);

        StringBuilder displayText = new StringBuilder();

        displayText.append("§7").append(stripColorCodes(name));

        if (distance.getValue()) {
            displayText.append(" §8[§7").append(String.format("%.1f", distanceToPlayer)).append("§8]");
        }

        if (showHealth.getValue()) {
            displayText.append(" §8| §a").append(String.format("%.1f", health));
        }

        String finalText = displayText.toString();

        FontRenderer fontRenderer = mc.fontRendererObj;
        int textWidth = fontRenderer.getStringWidth(stripColorCodes(finalText));
        int textHeight = 8;

        int padding = 2;
        int bgWidth = textWidth + padding * 2;
        int bgHeight = textHeight + padding * 2;

        int armorHeight = 0;
        if (showArmor.getValue() && armorValue > 0) {
            armorHeight = 10;
            bgHeight += armorHeight;
        }

        if (background.getValue()) {
            RenderUtils.drawRoundedRect(-bgWidth / 2, -bgHeight, bgWidth, bgHeight, 2, new Color(0, 0, 0, 150));
        }

        fontRenderer.drawString(finalText, -textWidth / 2, -bgHeight + padding, -1, true);

        if (showArmor.getValue() && armorValue > 0) {
            drawArmor(player, -bgWidth / 2 + padding, -bgHeight + textHeight + padding * 2, bgWidth - padding * 2);
        }

        if (throughWalls.getValue()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        GlStateManager.popMatrix();
    }

    private void drawArmor(EntityPlayer player, float x, float y, float width) {
        ItemStack[] armorInventory = player.inventory.armorInventory;
        int armorCount = 0;

        for (ItemStack stack : armorInventory) {
            if (stack != null) {
                armorCount++;
            }
        }

        if (armorCount == 0) return;

        float itemWidth = 8;
        float totalWidth = armorCount * itemWidth;
        float startX = x + (width - totalWidth) / 2;

        int index = 0;
        for (int i = 0; i < armorInventory.length; i++) {
            ItemStack stack = armorInventory[i];
            if (stack != null) {
                drawArmorItem(stack, startX + index * itemWidth, y, itemWidth);
                index++;
            }
        }
    }

    private void drawArmorItem(ItemStack stack, float x, float y, float size) {
        GlStateManager.pushMatrix();

        RenderUtils.drawRect(x, y, size, size, new Color(30, 30, 30, 200));

        if (stack.isItemDamaged()) {
            float durability = 1.0f - (float) stack.getItemDamage() / stack.getMaxDamage();
            Color durabilityColor = getDurabilityColor(durability);

            RenderUtils.drawRect(x, y + size - 2, size, 2, Color.BLACK);
            RenderUtils.drawRect(x, y + size - 2, size * durability, 2, durabilityColor);
        }

        Color armorColor = getArmorColor(stack);
        RenderUtils.drawRect(x + 1, y + 1, size - 2, size - 4, armorColor);

        if (stack.stackSize > 1) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            String count = String.valueOf(stack.stackSize);
            mc.fontRendererObj.drawStringWithShadow(count,
                    (x + size - 2) * 2 - mc.fontRendererObj.getStringWidth(count),
                    (y + size - 6) * 2, -1);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    private Color getArmorColor(ItemStack stack) {
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            switch (armor.getArmorMaterial()) {
                case LEATHER: return new Color(160, 101, 64);
                case CHAIN: return new Color(195, 195, 195);
                case IRON: return new Color(215, 215, 215);
                case GOLD: return new Color(249, 225, 58);
                case DIAMOND: return new Color(81, 196, 196);
                default: return Color.WHITE;
            }
        }
        return Color.GRAY;
    }

    private Color getDurabilityColor(float durability) {
        if (durability > 0.7) return Color.GREEN;
        if (durability > 0.3) return Color.YELLOW;
        return Color.RED;
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("§.", "");
    }
}