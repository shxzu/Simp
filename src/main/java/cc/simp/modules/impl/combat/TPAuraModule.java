package cc.simp.modules.impl.combat;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.player.AttackEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.client.AntiBotModule;
import cc.simp.processes.TargetSelectionProcess;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.PacketUtils;
import cc.simp.utils.mc.PathFinderUtils;
import cc.simp.utils.render.RenderUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "TP Aura", category = ModuleCategory.COMBAT)
public final class TPAuraModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Single);
    public static NumberProperty reach = new NumberProperty("Reach", 100, 3, 200, 1);
    public static NumberProperty cps = new NumberProperty("CPS", 2, 1, 20, 1);
    private final Property<Boolean> crits = new Property<>("Do Critical Hits", true);
    private final Property<Boolean> render = new Property<>("Render Trail", true);

    public enum Mode {
        Single,
        Multi
    }

    private final Timer clickTimer = new Timer();
    private List<Vec3> path;
    public Entity target;
    private long nextSwing;

    @Override
    public void onDisable() {
        target = null;
        super.onDisable();
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {

        /*
         * Getting targets and selecting the nearest one
         */
        final List<Entity> targets = TargetSelectionProcess.getTargetList();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        target = targets.getFirst();

        if (target == null || mc.thePlayer.isDead) {
            return;
        }

        /*
         * Doing the attack
         */
        this.doAttack(targets);
    };

    @EventLink
    public final Listener<Render3DEvent> onRender3D = event -> {
        if (!render.getValue() || path == null || target == null) {
            return;
        }

        Vec3 lastVector = null;

        for (final Vec3 vector : path) {
            if (lastVector != null) {
                RenderUtils.drawLine(lastVector.xCoord, lastVector.yCoord + 0.01, lastVector.zCoord, vector.xCoord, vector.yCoord + 0.01, vector.zCoord, Color.WHITE, 1);
            }
            lastVector = vector;
        }
    };

    private void doAttack(final List<Entity> targets) {
        if (clickTimer.hasTimeElapsed(this.nextSwing) && target != null && !mc.gameSettings.keyBindAttack.isKeyDown() && !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            final long clicks = cps.getValue().intValue();
            this.nextSwing = 1000 / clicks;

            /*
             * Attacking target
             */
            final double range = reach.getValue();

            switch (mode.getValue()) {
                case Single: {
                    if (mc.thePlayer.getDistanceToEntity(target) <= range) {
                        this.attack(target);
                    }
                    break;
                }

                case Multi: {
                    targets.removeIf(target -> mc.thePlayer.getDistanceToEntity(target) > range);

                    if (!targets.isEmpty()) {
                        targets.forEach(this::attack);
                    }
                    break;
                }
            }

            this.clickTimer.reset();
        }
    }

    private void attack(Entity target) {
        mc.playerController.syncCurrentPlayItem();

        final AttackEvent event = new AttackEvent(target);
        Simp.INSTANCE.getEventBus().post(event);

        if (event.isCancelled()) {
            return;
        }

        target = event.target;

        path = PathFinderUtils.computePath(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), new Vec3(target.posX, target.posY, target.posZ), true);

        if (path == null) {
            return;
        }

        for (final Vec3 vector : path) {
            PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(vector.xCoord, vector.yCoord, vector.zCoord, true));
        }

        mc.thePlayer.swingItem();

        PacketUtils.sendSilentPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));

        Collections.reverse(path);

        for (final Vec3 vector : path) {
            PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(vector.xCoord, vector.yCoord, vector.zCoord, true));
        }

        if (crits.getValue() || mc.thePlayer.fallDistance > 0 && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null) {
            mc.thePlayer.onCriticalHit(target);
        }
    }
}
