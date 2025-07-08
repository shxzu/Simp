package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.overlay.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.property.Property;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.font.FontManager;
import cc.simp.utils.client.font.TrueTypeFontRenderer;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static cc.simp.utils.client.Util.mc;
import static net.minecraft.client.gui.Gui.drawRect;

@ModuleInfo(label = "Target HUD", category = ModuleCategory.CLIENT, description = "Displays information about the current target of KillAura")
public final class TargetHUDModule extends Module {

    private final EnumProperty<TargetHUDType> targetHudTypeProperty = new EnumProperty<>("HUD Type", TargetHUDType.SIMP);
    private final Property<Boolean> rainbowProperty = new Property<>("Rainbow", false);

    public enum TargetHUDType {
        SIMP,
        ASTOLFO
    }

    private EntityLivingBase getTarget() {
        KillAuraModule killAura = (KillAuraModule) Simp.INSTANCE.getModuleManager().getModule(KillAuraModule.class);
        if (killAura != null && killAura.isEnabled() && KillAuraModule.target != null) {
            return KillAuraModule.target;
        }
        return null;
    }

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> {
        EntityLivingBase target = getTarget();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        MinecraftFontRenderer fr = Minecraft.getMinecraft().minecraftFontRendererObj;
        int x = sr.getScaledWidth() / 2 - 62;
        int y = sr.getScaledHeight() - 90;
        if (target == null) return;

        if (targetHudTypeProperty.getValue() == TargetHUDType.SIMP) {

            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0);

            // Background
            drawRect(0, 0, 125, 36, new Color(0, 0, 0, 150).getRGB());

            // Name
            fr.drawStringWithShadow(target.getName(), 38, 2, -1);

            // Health bar
            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float healthPercentage = health / maxHealth;

            // Rainbow or health-based color
            Color color;
            if (rainbowProperty.getValue()) {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                color = Color.getHSBColor(hue, 0.55f, 0.9f);
            } else {
                color = getHealthColor(healthPercentage);
            }

            // Health bar background
            drawRect(37, 11, 89, 15, new Color(0, 0, 0, 255).getRGB());

            // Health bar
            int healthWidth = (int) (52 * healthPercentage);
            drawRect(37, 11, 37 + healthWidth, 15, color.getRGB());

            // Health divisions
            for (int i = 0; i < 10; i++) {
                int divX = 37 + (52 * i / 10);
                drawRect(divX, 11, divX + 1, 15, new Color(0, 0, 0, 100).getRGB());
            }

            // Player face background
            drawRect(1, 1, 35, 35, new Color(0, 0, 0, 255).getRGB());

            // Info text
            GL11.glScaled(0.5f, 0.5f, 0.5f);
            String healthText = String.format("HP: %.1f | Dist: %.1f", health, Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target));
            fr.drawStringWithShadow(healthText, 76, 35, -1);

            String rotations = String.format("Yaw: %.1f Pitch: %.1f", target.rotationYaw, target.rotationPitch);
            fr.drawStringWithShadow(rotations, 76, 47, -1);

            // Player face
            GL11.glScaled(2.0f, 2.0f, 2.0f);
            GuiInventory.drawEntityOnScreen(16, 32, 22, -target.rotationYaw, target.rotationPitch, target);

            GL11.glPopMatrix();
        } else {
            // Health bar
            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float healthPercentage = health / maxHealth;

            // Rainbow or health-based color
            Color color;
            if (rainbowProperty.getValue()) {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                color = Color.getHSBColor(hue, 0.55f, 0.9f);
            } else {
                color = getHealthColor(healthPercentage);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);

            // Background
            drawRect(0, 0, 125, 36, new Color(0, 0, 0, 150).getRGB());

            // Health bar background
            drawRect(37, 26, 89, 32, new Color(0, 0, 0, 255).getRGB());

            // Health bar
            int healthWidth = (int) (52 * healthPercentage);
            drawRect(37, 26, 37 + healthWidth, 32, color.getRGB());

            //drawing the entity
            GuiInventory.drawEntityOnScreen(13, 38, 18, -target.rotationYaw, target.rotationPitch, target);

            //target name
            fr.drawString(target.getName(), 38, 2, -1, true);

            GL11.glPushMatrix();
            GL11.glScaled(2,2,2);
            mc.minecraftFontRendererObj.drawStringWithShadow((Math.round(target.getHealth() * 10f) / 10f + "❤").replace(".0", ""), 19,
                    5, color.getRGB());
            GL11.glPopMatrix();

            GlStateManager.popMatrix();
        }
    };

    private Color getHealthColor(float percentage) {
        if (percentage > 0.75f) return Color.GREEN;
        else if (percentage > 0.25f) return Color.YELLOW;
        else return Color.RED;
    }

}
