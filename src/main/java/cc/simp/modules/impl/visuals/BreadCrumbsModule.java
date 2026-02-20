package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.render.GlUtils;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Bread Crumbs", category = ModuleCategory.VISUALS)
public final class BreadCrumbsModule extends Module {

    @Override
    public void onEnable() {
        path.clear();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        path.clear();
        super.onDisable();
    }

    private final List<Vec3> path = new ArrayList<>();

    @EventLink
    private final Listener<MotionEvent> motionEventListener = event -> {
            if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY
                    || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
                path.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
            }

            while (path.size() > 40) {
                path.remove(0);
            }
    };

    @EventLink
    private final Listener<Render3DEvent> render3DEventListener = event -> {
        renderLine(path);
    };

    public void renderLine(final List<Vec3> path) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        RenderUtils.setAlphaLimit(0);
        RenderUtils.resetColor();
        GlUtils.setup2DRendering();
        GlUtils.startBlend();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glLineWidth(4);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        int count = 0;
        int fadeOffset = path.size();

        for (Vec3 v : path) {
            int alpha = fadeOffset > 0 ? Math.min(255, (int) ((count / (float) fadeOffset) * 255)) : 255;

            RenderUtils.color(ColorProcess.getColor().getRGB(), alpha / 255f);

            final double x = v.xCoord - mc.getRenderManager().renderPosX;
            final double y = v.yCoord - mc.getRenderManager().renderPosY;
            final double z = v.zCoord - mc.getRenderManager().renderPosZ;

            GL11.glVertex3d(x, y, z);
            count++;
        }
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlUtils.endBlend();
        GlUtils.end2DRendering();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlUtils.resetColor();
        RenderUtils.resetColor();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
