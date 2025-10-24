package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "ItemESP", category = ModuleCategory.VISUALS)
public final class ItemESPModule extends Module {

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        for (final Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (!(entity instanceof EntityItem entityItem))
                continue;

            String enhancement = "";

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, entityItem.getEntityItem()) != 0) {
                enhancement = "§b Protection:§c" + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, entityItem.getEntityItem());
            }

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, entityItem.getEntityItem()) != 0) {
                enhancement = "§b Sharpness:§c" + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, entityItem.getEntityItem());
            }

            if (entityItem.getEntityItem().getItem() == Items.golden_apple) {
                if (entityItem.getEntityItem().getItem().hasEffect(entityItem.getEntityItem())) {
                    enhancement = "§c Enchanted";
                }
            }

            final String var3 = (entityItem.getEntityItem().stackSize > 1) ? ("§f x" + entityItem.getEntityItem().stackSize) : "";

            float partialTicks = mc.timer.renderPartialTicks;
            double interpolatedX = entityItem.lastTickPosX + (entityItem.posX - entityItem.lastTickPosX) * partialTicks;
            double interpolatedY = entityItem.lastTickPosY + (entityItem.posY - entityItem.lastTickPosY) * partialTicks;
            double interpolatedZ = entityItem.lastTickPosZ + (entityItem.posZ - entityItem.lastTickPosZ) * partialTicks;
            double diffX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks - interpolatedX;
            double diffY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks - interpolatedY;
            double diffZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks - interpolatedZ;

            double dist = MathHelper.sqrt_double(diffX * diffX + diffY * diffY + diffZ * diffZ);

            GlStateManager.pushMatrix();
            drawText(entityItem.getEntityItem().getDisplayName() + var3 + enhancement, -1, interpolatedX, interpolatedY, interpolatedZ, dist);
            GlStateManager.popMatrix();
        }
    };

    public static void drawText(String value, int textColor, double posX, double posY, double posZ, double dist) {
        posX -= mc.getRenderManager().viewerPosX;
        posY -= mc.getRenderManager().viewerPosY;
        posZ -= mc.getRenderManager().viewerPosZ;
        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY + 1, (float) posZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        float scale = Math.min(Math.max(0.02266667f, (float) (0.001500000013038516 * dist)), 0.07f);
        GlStateManager.scale(-scale, -scale, -scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        int textWidth = mc.fontRendererObj.getStringWidth(value);
        mc.fontRendererObj.drawString(value, -textWidth / 2 + (int)(scale * 3.5f), (int)(-(123.805f * scale - 2.47494f)), textColor);
        mc.fontRendererObj.drawString(value, -textWidth / 2 + (int)(scale * 3.5f) - 1, (int)(-(123.805f * scale - 2.47494f)), Color.BLACK.getRGB());
        mc.fontRendererObj.drawString(value, -textWidth / 2 + (int)(scale * 3.5f) + 1, (int)(-(123.805f * scale - 2.47494f)), Color.BLACK.getRGB());
        mc.fontRendererObj.drawString(value, -textWidth / 2 + (int)(scale * 3.5f), (int)(-(123.805f * scale - 2.47494f)) - 1, Color.BLACK.getRGB());
        mc.fontRendererObj.drawString(value, -textWidth / 2 + (int)(scale * 3.5f), (int)(-(123.805f * scale - 2.47494f)) + 1, Color.BLACK.getRGB());

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }
}