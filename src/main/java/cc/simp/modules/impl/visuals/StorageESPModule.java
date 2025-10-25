package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Storage ESP", category = ModuleCategory.VISUALS)
public final class StorageESPModule extends Module {

    private final Property<Boolean> chests = new Property<>("Chests", true);
    private final Property<Boolean> enderChests = new Property<>("Ender Chests", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);
    private final Property<Boolean> filled = new Property<>("Filled", false);
    private final Property<Boolean> outline = new Property<>("Outline", true);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 2.0, () -> outline.getValue(), 1.0, 5.0, 0.5);
    private final NumberProperty alpha = new NumberProperty("Alpha", 0.3, 0.1, 1.0, 0.05);

    private final Color espColor = new Color(173, 216, 230);

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        List<TileEntity> storageBlocks = getStorageBlocks();

        if (storageBlocks.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (throughWalls.getValue()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GL11.glLineWidth(lineWidth.getValue().floatValue());

        for (TileEntity tileEntity : storageBlocks) {
            renderStorageBlock(tileEntity);
        }

        if (throughWalls.getValue()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    };

    private List<TileEntity> getStorageBlocks() {
        List<TileEntity> storageBlocks = new ArrayList<>();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (isValidStorageBlock(tileEntity)) {
                storageBlocks.add(tileEntity);
            }
        }

        return storageBlocks;
    }

    private boolean isValidStorageBlock(TileEntity tileEntity) {
        BlockPos pos = tileEntity.getPos();
        if (pos == null) return false;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return false;

        if (tileEntity instanceof TileEntityChest && chests.getValue()) {
            return true;
        }
        if (tileEntity instanceof TileEntityEnderChest && enderChests.getValue()) {
            return true;
        }
        return false;
    }

    private void renderStorageBlock(TileEntity tileEntity) {
        BlockPos pos = tileEntity.getPos();
        if (pos == null) return;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return;

        double x = pos.getX() - mc.getRenderManager().renderPosX;
        double y = pos.getY() - mc.getRenderManager().renderPosY;
        double z = pos.getZ() - mc.getRenderManager().renderPosZ;

        AxisAlignedBB boundingBox = block.getSelectedBoundingBox(mc.theWorld, pos);
        if (boundingBox == null) return;

        boundingBox = boundingBox.offset(-mc.getRenderManager().renderPosX, -mc.getRenderManager().renderPosY, -mc.getRenderManager().renderPosZ);
        boundingBox = boundingBox.expand(-0.002, -0.002, -0.002);

        Color renderColor = new Color(
                espColor.getRed(),
                espColor.getGreen(),
                espColor.getBlue(),
                (int)(alpha.getValue() * 255)
        );

        if (filled.getValue()) {
            renderFilledBox(boundingBox, renderColor);
        }

        if (outline.getValue()) {
            renderOutlinedBox(boundingBox, renderColor);
        }
    }

    private void renderFilledBox(AxisAlignedBB boundingBox, Color color) {
        RenderUtils.drawBlockESP(
                new BlockPos(boundingBox.minX + mc.getRenderManager().renderPosX,
                        boundingBox.minY + mc.getRenderManager().renderPosY,
                        boundingBox.minZ + mc.getRenderManager().renderPosZ),
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f * 0.3f,
                0,
                0
        );
    }

    private void renderOutlinedBox(AxisAlignedBB boundingBox, Color color) {
        RenderUtils.drawBlockESP(
                new BlockPos(boundingBox.minX + mc.getRenderManager().renderPosX,
                        boundingBox.minY + mc.getRenderManager().renderPosY,
                        boundingBox.minZ + mc.getRenderManager().renderPosZ),
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                0,
                color.getAlpha() / 255.0f,
                lineWidth.getValue().floatValue()
        );
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}