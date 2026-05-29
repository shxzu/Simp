package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Block Outline", category = ModuleCategory.VISUALS)
public class BlockOutlineModule extends Module {

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = e -> {
        if (mc.objectMouseOver == null) return;
        if(mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK){
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            RenderUtils.start3D();
            RenderUtils.color(ColorProcess.getColor().getRGB());
            double x = pos.getX() - mc.getRenderManager().renderPosX;
            double y = pos.getY() - mc.getRenderManager().renderPosY;
            double z = pos.getZ() - mc.getRenderManager().renderPosZ;
            double height = mc.theWorld.getBlockState(pos).getBlock().getBlockBoundsMaxY() - mc.theWorld.getBlockState(pos).getBlock().getBlockBoundsMinY();
            GL11.glLineWidth(1);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y,z);
            GL11.glVertex3d(x,y + height,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y,z);
            GL11.glVertex3d(x + 1,y + height,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y,z + 1);
            GL11.glVertex3d(x + 1,y + height,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y,z + 1);
            GL11.glVertex3d(x,y + height,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y,z);
            GL11.glVertex3d(x + 1,y,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y + height,z);
            GL11.glVertex3d(x + 1,y + height,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y,z);
            GL11.glVertex3d(x,y,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y + height,z);
            GL11.glVertex3d(x,y + height,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y,z + 1);
            GL11.glVertex3d(x + 1,y,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y + height,z + 1);
            GL11.glVertex3d(x + 1,y + height,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y,z + 1);
            GL11.glVertex3d(x + 1,y,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x + 1,y + height,z + 1);
            GL11.glVertex3d(x + 1,y + height,z);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y,z + 1);
            GL11.glVertex3d(x + 1,y,z + 1);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(x,y + height,z + 1);
            GL11.glVertex3d(x + 1,y + height,z + 1);
            GL11.glEnd();
            RenderUtils.stop3D();
        }
    };

}
