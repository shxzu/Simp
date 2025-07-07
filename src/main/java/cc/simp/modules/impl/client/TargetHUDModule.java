package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.event.impl.render.overlay.Render2DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.combat.KillAuraModule;
import cc.simp.property.Property;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

import static net.minecraft.client.gui.Gui.drawRect;

@ModuleInfo(label = "TargetHUD", category = ModuleCategory.CLIENT, description = "Displays information about the current target of KillAura")
public final class TargetHUDModule extends Module {

    private final Property<Boolean> rainbowProperty = new Property<>("Rainbow", false);

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
        if (target == null) return;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        // Position calculations
        int x = sr.getScaledWidth() / 2 - 62;
        int y = sr.getScaledHeight() - 90;

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
        int healthWidth = (int)(52 * healthPercentage);
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
    };

    private Color getHealthColor(float percentage) {
        if (percentage > 0.75f) return Color.GREEN;
        else if (percentage > 0.25f) return Color.YELLOW;
        else return Color.RED;
    }
}
