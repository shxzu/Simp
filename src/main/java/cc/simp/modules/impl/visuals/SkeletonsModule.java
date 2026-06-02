package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static cc.simp.utils.Util.mc;
import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(label = "Skeletons", category = ModuleCategory.VISUALS)
public final class SkeletonsModule extends Module {
    public final Property<Boolean> useClientColor = new Property<>("Use Client Color", false);
    public final NumberProperty skeletonWidth = new NumberProperty("Skeletons Width", 0.5f, 0.5f, 5, 0.5f);

    public final Map<EntityPlayer, float[][]> playerRotationMap = new HashMap<>();

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = event -> {
        if (mc.theWorld == null) return;

        // Populate rotation map from each player's ModelBiped
        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            try {
                Render<?> render = mc.getRenderManager().getEntityRenderObject(player);
                if (!(render instanceof RenderPlayer)) continue;
                RenderPlayer rp = (RenderPlayer) render;
                if (rp == null) continue;
                ModelBiped model = (ModelBiped) rp.getMainModel();

                playerRotationMap.put(player, new float[][] {
                        // [0] head
                        { model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleY, model.bipedHead.rotateAngleZ },
                        // [1] right arm
                        { model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ },
                        // [2] left arm
                        { model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ },
                        // [3] right leg
                        { model.bipedRightLeg.rotateAngleX, model.bipedRightLeg.rotateAngleY, model.bipedRightLeg.rotateAngleZ },
                        // [4] left leg
                        { model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ }
                });
            } catch (Exception ignored) {}
        }

        glLineWidth(skeletonWidth.getValue().floatValue());
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glDepthMask(false);

        RenderUtils.color(useClientColor.getValue() ? ColorProcess.getColor().getRGB() : Color.WHITE.getRGB());

        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            drawSkeleton(mc.timer.renderPartialTicks, player);
        }

        RenderUtils.resetColor();
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_DEPTH_TEST);
    };

    private void drawSkeleton(float pt, EntityPlayer player) {
        float[][] entPos = playerRotationMap.get(player);
        if (entPos == null) return;

        glPushMatrix();

        float x = (float) (MathUtils.interpolate(player.prevPosX, player.posX, pt) -
                mc.getRenderManager().renderPosX);
        float y = (float) (MathUtils.interpolate(player.prevPosY, player.posY, pt) -
                mc.getRenderManager().renderPosY);
        float z = (float) (MathUtils.interpolate(player.prevPosZ, player.posZ, pt) -
                mc.getRenderManager().renderPosZ);

        glTranslated(x, y, z);

        boolean sneaking = player.isSneaking();
        final float xOff = MathUtils.interpolate(player.prevRenderYawOffset, player.renderYawOffset, pt);
        float yOff = sneaking ? 0.6F : 0.75F;

        glRotatef(-xOff, 0.0F, 1.0F, 0.0F);
        glTranslatef(0.0F, 0.0F, sneaking ? -0.235F : 0.0F);

        // Right leg
        glPushMatrix();
        glTranslatef(-0.125F, yOff, 0.0F);
        if (entPos[3][0] != 0.0F) glRotatef(entPos[3][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[3][1] != 0.0F) glRotatef(entPos[3][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[3][2] != 0.0F) glRotatef(entPos[3][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -yOff, 0.0F);
        glEnd();
        glPopMatrix();

        // Left leg
        glPushMatrix();
        glTranslatef(0.125F, yOff, 0.0F);
        if (entPos[4][0] != 0.0F) glRotatef(entPos[4][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[4][1] != 0.0F) glRotatef(entPos[4][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[4][2] != 0.0F) glRotatef(entPos[4][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -yOff, 0.0F);
        glEnd();
        glPopMatrix();

        glTranslatef(0.0F, 0.0F, sneaking ? 0.25F : 0.0F);

        glPushMatrix();
        glTranslatef(0.0F, sneaking ? -0.05F : 0.0F, sneaking ? -0.01725F : 0.0F);

        // Right arm
        glPushMatrix();
        glTranslatef(-0.375F, yOff + 0.55F, 0.0F);
        if (entPos[1][0] != 0.0F) glRotatef(entPos[1][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[1][1] != 0.0F) glRotatef(entPos[1][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[1][2] != 0.0F) glRotatef(-entPos[1][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -0.5F, 0.0F);
        glEnd();
        glPopMatrix();

        // Left arm
        glPushMatrix();
        glTranslatef(0.375F, yOff + 0.55F, 0.0F);
        if (entPos[2][0] != 0.0F) glRotatef(entPos[2][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[2][1] != 0.0F) glRotatef(entPos[2][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[2][2] != 0.0F) glRotatef(-entPos[2][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -0.5F, 0.0F);
        glEnd();
        glPopMatrix();

        glRotatef(xOff - player.rotationYawHead, 0.0F, 1.0F, 0.0F);

        // Head
        glPushMatrix();
        glTranslatef(0.0F, yOff + 0.55F, 0.0F);
        if (entPos[0][0] != 0.0F) glRotatef(entPos[0][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, 0.3F, 0.0F);
        glEnd();
        glPopMatrix();

        glPopMatrix(); // end sneaking arm offset

        glRotatef(sneaking ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
        glTranslatef(0.0F, sneaking ? -0.16175F : 0.0F, sneaking ? -0.48025F : 0.0F);

        // Pelvis
        glPushMatrix();
        glTranslated(0.0F, yOff, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3f(-0.125F, 0.0F, 0.0F);
        glVertex3f(0.125F, 0.0F, 0.0F);
        glEnd();
        glPopMatrix();

        // Body
        glPushMatrix();
        glTranslatef(0.0F, yOff, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, 0.55F, 0.0F);
        glEnd();
        glPopMatrix();

        // Chest
        glPushMatrix();
        glTranslatef(0.0F, yOff + 0.55F, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3f(-0.375F, 0.0F, 0.0F);
        glVertex3f(0.375F, 0.0F, 0.0F);
        glEnd();
        glPopMatrix();

        glPopMatrix(); // end main player matrix
    }

    @Override
    public void onDisable() {
        playerRotationMap.clear();
    }
}