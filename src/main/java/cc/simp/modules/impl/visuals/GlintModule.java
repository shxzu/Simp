package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.GlintEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.src.Config;
import net.optifine.CustomItems;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Glint", category = ModuleCategory.VISUALS)
public final class GlintModule extends Module {
    private final Property<Boolean> glintWeapons = new Property<>("Glint Weapons", true);
    private final NumberProperty minHue = new NumberProperty("Min Hue", 0, 0, 360, 1);
    private final NumberProperty maxHue = new NumberProperty("Max Hue", 360, 0, 360, 1);
    private final NumberProperty layers = new NumberProperty("Layers", 4, 1, 8, 1);

    @EventLink
    public final Listener<GlintEvent> onGlint = event -> {
        final ItemStack itemStack = event.getItemStack();
        final Item item = itemStack.getItem();

        if (this.glintWeapons.getValue() && (item instanceof ItemSword || item instanceof ItemAxe)) {
            event.setEnchanted(true);
        }

        event.setCancelled();

        if (event.isEnchanted() && event.isRender()) {
            this.renderEffect(event.getModel());
        }
    };

    public void renderEffect(final IBakedModel model) {
        if (!Config.isCustomItems() || CustomItems.isUseGlint()) {
            if (!Config.isShaders() || !Shaders.isShadowPass) {
                GlStateManager.depthMask(false);
                GlStateManager.depthFunc(514);
                GlStateManager.disableLighting();
                GlStateManager.blendFunc(768, 1);
                mc.getRenderItem().textureManager.bindTexture(RenderItem.RES_ITEM_GLINT);

                if (Config.isShaders() && !mc.getRenderItem().renderItemGui) {
                    ShadersRender.renderEnchantedGlintBegin();
                }

                GlStateManager.matrixMode(5890);
                GlStateManager.pushMatrix();

                GlStateManager.scale(8.0F, 8.0F, 8.0F);
                final float f = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
                GlStateManager.translate(f, 0.0F, 0.0F);

                for (int layer = 1; layer <= layers.getValue().intValue(); layer++) {
                    GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
                    mc.getRenderItem().renderModel(model,
                            new Color(Color.HSBtoRGB((minHue.getValue().intValue() +
                                    Math.abs(this.maxHue.getValue().intValue() -
                                            minHue.getValue().intValue()) * (layer /
                                            layers.getValue().floatValue())) / 255, 1, 1)).
                                    hashCode());
                }

                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.blendFunc(770, 771);
                GlStateManager.enableLighting();
                GlStateManager.depthFunc(515);
                GlStateManager.depthMask(true);
                mc.getRenderItem().textureManager.bindTexture(TextureMap.locationBlocksTexture);

                if (Config.isShaders() && !mc.getRenderItem().renderItemGui) {
                    ShadersRender.renderEnchantedGlintEnd();
                }
            }
        }
    }
}
