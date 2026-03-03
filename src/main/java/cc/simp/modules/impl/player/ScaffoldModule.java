package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.notifications.NotificationManager;
import cc.simp.api.notifications.NotificationType;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.EnumFacingOffset;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.client.Timer;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.animations.Animation;
import cc.simp.utils.render.animations.Direction;
import cc.simp.utils.render.animations.impl.DecelerateAnimation;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Scaffold", category = ModuleCategory.PLAYER)
public final class ScaffoldModule extends Module {

    private static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Normal);
    private static final ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Normal, () -> mode.getValue() != Mode.SlowTelly && mode.getValue() != Mode.FastTelly && mode.getValue() != Mode.Hypixel);
    private static final ModeProperty<SearchAlgorithm> searchAlgorithm = new ModeProperty<>("Search Algorithm", SearchAlgorithm.Normal);
    private final NumberProperty minRotationSpeed = new NumberProperty("Min Rotation Speed", 5, 0, 10, 1);
    private final NumberProperty maxRotationSpeed = new NumberProperty("Max Rotation Speed", 8, 0, 10, 1);
    public static Property<Boolean> limitRotations = new Property<>("Limit Rotations", false);
    private final NumberProperty rotationLimiterYawMax = new NumberProperty("Yaw Max", 30, limitRotations::getValue, 30, 180, 1);
    private final NumberProperty rotationLimiterYawMin = new NumberProperty("Yaw Min", 30, limitRotations::getValue, 30, 180, 1);
    private final NumberProperty rotationLimiterPitchMax = new NumberProperty("Pitch Max", 30, limitRotations::getValue, 20, 90, 1);
    private final NumberProperty rotationLimiterPitchMin = new NumberProperty("Pitch Min", 30, limitRotations::getValue, 20, 90, 1);
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 10, 1);
    public Property<Boolean> packetPlace = new Property<>("Packet Place", false);
    public Property<Boolean> packetSwing = new Property<>("Packet Swing", false);
    private static final ModeProperty<SprintMode> sprintMode = new ModeProperty<>("Sprint Mode", SprintMode.None);
    private static final ModeProperty<TowerMode> towerMode = new ModeProperty<>("Tower Mode", TowerMode.None);
    private static final Property<Boolean> towerMove = new Property<>("Tower Move", true, () -> towerMode.getValue() != TowerMode.None);
    public static Property<Boolean> moveFix = new Property<>("Move Fix", true);
    private final ModeProperty<RayCast> rayCast = new ModeProperty<>("Ray Cast", RayCast.Normal);
    public static ModeProperty<JumpMode> autoJump = new ModeProperty<>("Auto Jump", JumpMode.None);
    private final NumberProperty jumpDelayTicks = new NumberProperty("Auto Jump Delay", 0, () -> autoJump.getValue() != JumpMode.None, 0, 5, 1);
    public static Property<Boolean> edge = new Property<>("Jump Only On Edge", false, () -> autoJump.getValue() != JumpMode.None);
    public static Property<Boolean> keepY = new Property<>("Keep Y", false, () -> autoJump.getValue() != JumpMode.None);
    private static final Property<Boolean> sneak = new Property<>("Sneak", false);
    private final NumberProperty sneakEvery = new NumberProperty("Sneak Every", 1, sneak::getValue, 0, 10, 1);
    private static final Property<Boolean> safeWalk = new Property<>("Safe Walk", false);
    private static final Property<Boolean> safeWalkOnAir = new Property<>("Safe Walk On Air", false, safeWalk::getValue);
    private final NumberProperty expand = new NumberProperty("Expand", 0, 0, 4, 1);
    private static final ModeProperty<BlockCounter> blockCounter = new ModeProperty<>("Block Counter", BlockCounter.None);
    private final Property<Boolean> render = new Property<>("Render Block Selection", true);

    private enum Mode {
        Normal("Normal"),
        Hypixel("Hypixel"),
        SlowTelly("Slow Telly"),
        FastTelly("Fast Telly"),
        Breezily("Breezily"),
        GodBridge("God Bridge");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private enum Rotations {
        Normal("Normal"),
        StaticYaw("Static Yaw");

        public String name;

        Rotations(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private enum SprintMode {
        Vanilla,
        Legit,
        None
    }

    private enum TowerMode {
        Vanilla,
        Dev,
        NCP,
        None
    }

    private enum RayCast {
        None,
        Normal,
        Strict
    }

    private enum SearchAlgorithm {
        Normal,
        Secondary,
        Simple
    }

    private enum JumpMode {
        None,
        Normal,
        Dev
    }

    private enum BlockCounter {
        Tenacity,
        None
    }

    private final Timer delayTimer = new Timer();
    private Vec3 targetBlock;
    private EnumFacingOffset enumFacing;
    public Vec3i offset = new Vec3i(0, 0, 0);
    private BlockPos blockFace;
    private float targetYaw, targetPitch, yawDrift, pitchDrift;
    @Getter
    @Setter
    private int ticksOnAir;
    public double startY;
    private boolean canPlace;
    private int directionalChange;
    private int blockCount;
    private Animation anim = new DecelerateAnimation(250, 1);
    private float rotSpeed;
    private boolean overrided;
    public int recursions, recursion;
    private boolean stop;
    private int blocksPlaced;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        this.offset = new Vec3i(0, 0, 0);
    };

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        this.setSuffix(mode.getValue().toString());

        resetBinds(false, false, true, true, false, false);

        if (safeWalk.getValue()) {
            if (safeWalkOnAir.getValue()) {
                mc.thePlayer.safeWalk = true;
            } else {
                mc.thePlayer.safeWalk = mc.thePlayer.onGround;
            }
        }

        sprint();

        sneak();

        tower();

        for (recursion = 0; recursion <= recursions; recursion++) {

            if (!overrided) {
                this.rotSpeed = (float) MathUtils.getRandom(this.minRotationSpeed.getValue(), this.maxRotationSpeed.getValue());
            }

            if (expand.getValue().intValue() != 0) {
                double direction = MovementUtils.direction(mc.thePlayer.rotationYaw, mc.gameSettings.keyBindForward.isKeyDown() ? 1 :
                        mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 0, mc.gameSettings.keyBindRight.isKeyDown() ? -1 :
                        mc.gameSettings.keyBindLeft.isKeyDown() ? 1 : 0);

                for (int range = 0; range <= expand.getValue().intValue(); range++) {
                    if (PlayerUtils.blockAheadOfPlayer(range, this.offset.getY() - 0.5) instanceof BlockAir) {
                        this.offset = this.offset.add(new Vec3i((int) (-Math.sin(direction) * (range + 1)), 0, (int) (Math.cos(direction) * (range + 1))));
                        break;
                    }
                }
            }

            // Same Y
            final boolean sameY = ((keepY.getValue() || Simp.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()) && !mc.gameSettings.keyBindJump.isKeyDown()) && MovementUtils.isMoving();

            if (InventoryUtils.findBlock() == -1) {
                NotificationManager.post(NotificationType.DISABLE, "Scaffold", "No blocks found, disabling Scaffold.");
                this.toggle();
                return;
            }

            // Getting ItemSlot
            if (InventoryUtils.findBlock() != -1) {
                mc.thePlayer.inventory.currentItem = InventoryUtils.findBlock();
            }

            // Used to detect when to place a block, if over air, allow placement of blocks
            if (doesNotContainBlock(1) && (!sameY || (doesNotContainBlock(2) && doesNotContainBlock(3) && doesNotContainBlock(4)))) {
                ticksOnAir++;
            } else {
                ticksOnAir = 0;
            }

            canPlace = mc.thePlayer.inventory.currentItem == InventoryUtils.findBlock() &&
                    ticksOnAir > 0 && delayTimer.hasTimeElapsed(placeDelay.getValue().longValue() * 20);

            // Gets block to place
            targetBlock = PlayerUtils.getPlacePossibility(offset.getX(), offset.getY(), offset.getZ(), sameY ? (int) Math.floor(startY) : null);

            if (targetBlock == null) {
                return;
            }

            // Gets EnumFacing
            enumFacing = PlayerUtils.getEnumFacing(targetBlock, offset.getY() < 0);

            if (enumFacing == null) {
                return;
            }

            final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);

            blockFace = position.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);

            if (blockFace == null || enumFacing == null || enumFacing.getEnumFacing() == null) {
                return;
            }

            this.doRotations();

            if (targetBlock == null || enumFacing == null || blockFace == null) {
                return;
            }

            if (startY - 1 != Math.floor(targetBlock.yCoord) && sameY) {
                return;
            }

            if (mc.thePlayer.inventory.getCurrentItem() == null || !(mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock)) {
                return;
            }

            if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
                if (canPlace && (RayCastUtils.overBlock(enumFacing.getEnumFacing(), blockFace, rayCast.getValue() == RayCast.Strict) || rayCast.getValue() == RayCast.None)) {
                    this.place();

                    ticksOnAir = 0;

                }
            }

            // For Same Y
            if (mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.posY % 1 > 0.5) {
                startY = Math.floor(mc.thePlayer.posY);
            }

            if ((mc.thePlayer.posY < startY || mc.thePlayer.onGround) && !MovementUtils.isMoving()) {
                startY = Math.floor(mc.thePlayer.posY);
            }
        }
    };

    @EventLink
    public final Listener<StrafeEvent> onStrafe = event -> {
        if (!moveFix.getValue()) {
            MovementUtils.useDiagonalSpeed();
        }
        if (!mc.gameSettings.keyBindJump.isPressed()) {
            this.jump();
        }
    };

    @EventLink
    public final Listener<MoveEvent> moveEventListener = event -> {
        if (stop) {
            event.setForward(0);
            event.setStrafe(0);
            event.setJump(false);
        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> onPacketReceiveEvent = PacketUtils::correctBlockCount;

    @EventLink
    public final Listener<Render3DEvent> render3DEventListener = event -> {
        if (render.getValue()) {
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glDisable(2929);
            GL11.glDisable(3553);
            GlStateManager.disableCull();
            GL11.glDepthMask(false);
            final float red = ColorProcess.getColor().getRed();
            final float green = ColorProcess.getColor().getGreen();
            final float blue = ColorProcess.getColor().getBlue();
            float lineWidth = 0.0f;
            if (this.blockFace != null) {
                if (mc.thePlayer.getDistance(this.blockFace.getX(), this.blockFace.getY(), this.blockFace.getZ()) > 1.0) {
                    double d0 = 1.0 - mc.thePlayer.getDistance(this.blockFace.getX(), this.blockFace.getY(), this.blockFace.getZ()) / 20.0;
                    if (d0 < 0.3) {
                        d0 = 0.3;
                    }
                    lineWidth *= (float) d0;
                }
                RenderUtils.drawBlockESP(this.blockFace, red, green, blue, 0.3137255f, 1.0f, lineWidth);
            }
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glDepthMask(true);
            GlStateManager.enableCull();
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2848);
        }
    };

    @EventLink
    public final Listener<Render2DEvent> render2DEventListener = event -> renderBlockCounter();

    @EventLink
    public final Listener<ShaderEvent> shaderEventListener = event -> renderBlockCounter();

    @Override
    public void onEnable() {
        anim = new DecelerateAnimation(250, 1);
        if (mc.thePlayer != null) {
            targetYaw = mc.thePlayer.rotationYaw - 180;
            targetPitch = 90;

            pitchDrift = (float) ((Math.random() - 0.5) * (Math.random() - 0.5) * 10);
            yawDrift = (float) ((Math.random() - 0.5) * (Math.random() - 0.5) * 10);

            startY = Math.floor(mc.thePlayer.posY);
            targetBlock = null;
            overrided = false;
            recursions = 0;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        anim = new DecelerateAnimation(250, 1);
        if (mc.thePlayer != null) {
            mc.thePlayer.safeWalk = false;
            mc.timer.timerSpeed = 1.0f;
            overrided = false;
            stop = false;
            blocksPlaced = 0;
        }
        resetBinds();
        super.onDisable();
    }

    private void renderBlockCounter() {
        if (blockCounter.getValue() == BlockCounter.None) return;
        switch (blockCounter.getValue()) {
            case Tenacity -> {
                anim.setDirection(this.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!this.isEnabled() && anim.isDone()) return;
                ScaledResolution sr = new ScaledResolution(mc);
                float output = anim.getOutput().floatValue();
                float x, y;
                if (mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock)
                    blockCount = mc.thePlayer.inventory.getCurrentItem().stackSize;
                float blockWH = mc.thePlayer.inventory.getCurrentItem() != null ? 15 : -2;
                int spacing = 3;
                String text = "§l" + blockCount + "§r block" + (blockCount != 1 ? "s" : "");
                float textWidth = FontProcess.getFont("bold").getStringWidth(text);

                float totalWidth = ((textWidth + blockWH + spacing) + 6) * output;
                x = sr.getScaledWidth() / 2f - (totalWidth / 2f);
                y = sr.getScaledHeight() - (sr.getScaledHeight() / 2f - 20);
                float height = 20;
                RenderUtils.startScissor(x - 1.5f, y - 1.5f, totalWidth + 3, height + 3);

                RenderUtils.drawRoundedRect(x, y, totalWidth, height, 5, new Color(ColorProcess.getColor().darker().getRed(), ColorProcess.getColor().darker().getGreen(), ColorProcess.getColor().darker().getBlue(), 130));

                FontProcess.getFont("bold").drawString(text, x + 3 + blockWH + spacing, y + height / 2f - FontProcess.getFont("bold").getHeight() / 2f + .5f, -1);
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.thePlayer.inventory.getCurrentItem(), (int) x + 3, (int) (y + 10 - (blockWH / 2)));
                RenderHelper.disableStandardItemLighting();
                RenderUtils.endScissor();
            }
        }
    }

    public void resetBinds() {
        resetBinds(true, true, true, true, true, true);
    }

    public void resetBinds(boolean sneak, boolean jump, boolean right, boolean left, boolean forward, boolean back) {
        if (sneak)
            mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
        if (jump) mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
        if (right)
            mc.gameSettings.keyBindRight.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
        if (left) mc.gameSettings.keyBindLeft.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));

        if (forward)
            mc.gameSettings.keyBindForward.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        if (back)
            mc.gameSettings.keyBindBack.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
    }

    public void doRotations() {

        MovementFix movementFix = moveFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF;

        /* Calculating target rotations */
        switch (mode.getValue()) {
            case Normal:
                mc.entityRenderer.getMouseOver(1);
                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        getBaseRotations();
                        if (rotations.getValue() == Rotations.StaticYaw) {
                            targetYaw = mc.thePlayer.rotationYaw - 180;
                        }
                    }
                }
                break;
            case Breezily:
                if (canPlace) {
                    if (enumFacing.getEnumFacing() == EnumFacing.UP) {
                        targetPitch = 90;
                    } else {
                        double staticYaw = (float) (Math.toDegrees(Math.atan2(enumFacing.getOffset().zCoord,
                                enumFacing.getOffset().xCoord)) % 360) - 90;
                        double staticPitch = 80;

                        targetYaw = (float) staticYaw + yawDrift;
                        targetPitch = (float) staticPitch + pitchDrift;
                    }
                } else if (Math.random() > 0.99 || targetPitch % 90 == 0) {
                    yawDrift = (float) (Math.random() - 0.5);
                    pitchDrift = (float) (Math.random() - 0.5);
                }

                if (mc.gameSettings.keyBindForward.isKeyDown() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    double offset = 0;
                    double speed = 0;

                    switch (mc.thePlayer.getHorizontalFacing()) {
                        case NORTH:
                            offset = mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
                            speed = mc.thePlayer.motionZ;
                            break;

                        case EAST:
                            offset = mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
                            speed = mc.thePlayer.motionX;
                            break;

                        case SOUTH:
                            offset = 1 - (mc.thePlayer.posX - Math.floor(mc.thePlayer.posX));
                            speed = mc.thePlayer.motionZ;
                            break;

                        case WEST:
                            offset = 1 - (mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ));
                            speed = mc.thePlayer.motionX;
                            break;

                        default:
                            Logger.chatPrint("Unknown " + Math.random());
                            break;
                    }
                    speed = Math.abs(speed);

                    if (speed < 0.086 && Math.abs(offset - 0.5) < 0.4 && placeDelay.getValue().intValue() <= 1) {
                    } else if (offset < 0.5 + ((Math.random() - 0.5) / 10)) {
                        mc.gameSettings.keyBindLeft.setPressed(false);
                        mc.gameSettings.keyBindRight.setPressed(true);
                    } else {
                        mc.gameSettings.keyBindRight.setPressed(false);
                        mc.gameSettings.keyBindLeft.setPressed(true);
                    }
                }

                break;
            case SlowTelly:
                if (recursion == 0) {
                    if (mc.thePlayer.offGroundTicks <= 9 && mc.thePlayer.offGroundTicks > 3) {
                        mc.entityRenderer.getMouseOver(1);

                        if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                            if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                                getBaseRotations();
                            }
                        }
                    } else {
                        targetPitch = mc.thePlayer.rotationPitch;
                        targetYaw = mc.thePlayer.rotationYaw;
                        canPlace = false;
                    }
                }
                break;
            case FastTelly:
                if (recursion == 0) {

                    mc.entityRenderer.getMouseOver(1);

                    if (mc.thePlayer.hurtTime == 0 && mc.thePlayer.onGround) {
                        targetYaw = (float) Math.toDegrees(MovementUtils.direction());
                    }

                    if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                        this.targetYaw = mc.thePlayer.rotationYaw;
                    } else {
                        if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                            if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                                getBaseRotations();
                            }
                        }
                    }
                }
                break;
            case Hypixel:
                overrided = true;
                boolean diagonal = RotationUtils.getMovementYaw() % 90.0f > 10.0f && RotationUtils.getMovementYaw() % 90.0f < 80.0f;
                if (recursion == 0) {
                    mc.entityRenderer.getMouseOver(1);
                    if (mc.thePlayer.onGround && MovementUtils.isMoving() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        rotSpeed = 5f;
                        this.targetYaw = mc.thePlayer.rotationYaw;
                        canPlace = false;
                    } else {
                        if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                            if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                                rotSpeed = 0.8f;
                                if (diagonal) {
                                    rotSpeed = 0.9f;
                                }
                                getBaseRotations();
                                canPlace = true;
                            }
                        }
                    }
                }
                break;
            case GodBridge:
                targetYaw = (mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw % 90) - 180 + 45 * (mc.thePlayer.rotationYaw > 0 ? 1 : -1);
                targetPitch = 76.4f;

                movementFix = MovementFix.TRADITIONAL;

                double spacing = 0.15;
                boolean edgeX = Math.abs(mc.thePlayer.posX % 1) > 1 - spacing ||
                        Math.abs(mc.thePlayer.posX % 1) < spacing;
                boolean edgeZ = Math.abs(mc.thePlayer.posZ % 1) > 1 - spacing ||
                        Math.abs(mc.thePlayer.posZ % 1) < spacing;

                mc.gameSettings.keyBindRight.setPressed((edgeX && edgeZ) || (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())));
                mc.gameSettings.keyBindBack.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                mc.gameSettings.keyBindForward.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
                mc.gameSettings.keyBindLeft.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));

                directionalChange++;
                if (Math.abs(MathHelper.wrapAngleTo180_double(targetYaw -
                        RotationProcess.lastServerRotations.getX())) > 10) {
                    directionalChange = (int) (Math.random() * 4);
                    yawDrift = (float) (Math.random() - 0.5) / 10f;
                    pitchDrift = (float) (Math.random() - 0.5) / 10f;
                }

                if (Math.random() > 0.99) {
                    yawDrift = (float) (Math.random() - 0.5) / 10f;
                    pitchDrift = (float) (Math.random() - 0.5) / 10f;
                }

                if (directionalChange <= 10) {
                    mc.gameSettings.keyBindSneak.setPressed(true);
                } else if (directionalChange == 11) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                }

                targetYaw += yawDrift;
                targetPitch += pitchDrift;
                break;
        }

        Vector2f limitedRotations = applyRotationLimits(targetYaw, targetPitch);
        targetYaw = limitedRotations.x;
        targetPitch = limitedRotations.y;

        if (rotSpeed != 0 && blockFace != null && enumFacing != null) {
            RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, movementFix);
        }
    }

    public void getBaseRotations() {

        switch (searchAlgorithm.getValue()) {
            case Normal -> {
                EntityPlayer player = mc.thePlayer;
                double difference = player.posY + player.getEyeHeight() - targetBlock.yCoord -
                        0.5 - (Math.random() - 0.5) * 0.1;

                MovingObjectPosition movingObjectPosition = null;

                for (int offset = -180; offset <= 180; offset += 45) {
                    player.setPosition(player.posX, player.posY - difference, player.posZ);
                    movingObjectPosition = RayCastUtils.rayCast(new Vector2f((float) (player.rotationYaw + (offset * 3)), 0), 4.5);
                    player.setPosition(player.posX, player.posY + difference, player.posZ);

                    if (movingObjectPosition == null || movingObjectPosition.hitVec == null) return;

                    Vector2f rotations = RotationUtils.calculate(movingObjectPosition.hitVec);

                    if (RayCastUtils.overBlock(rotations, blockFace, enumFacing.getEnumFacing())) {
                        targetYaw = rotations.x;
                        targetPitch = rotations.y;
                        return;
                    }
                }

                // Backup Rotations
                final Vector2f rotations = RotationUtils.calculate(
                        new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacing.getEnumFacing());

                if (!RayCastUtils.overBlock(new Vector2f(targetYaw, targetPitch), blockFace, enumFacing.getEnumFacing())) {
                    targetYaw = rotations.x;
                    targetPitch = rotations.y;
                }
            }
            case Secondary -> {
                EntityPlayer player = mc.thePlayer;

                // Calculate the optimal pitch based on distance and height difference
                double deltaX = blockFace.getX() - player.posX + 0.5;
                double deltaY = blockFace.getY() - (player.posY + player.getEyeHeight()) + 0.5;
                double deltaZ = blockFace.getZ() - player.posZ + 0.5;
                double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                // Start with calculated rotations
                float baseYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
                float basePitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

                // Fine-tune rotations by checking multiple angles around the base
                float bestYaw = baseYaw;
                float bestPitch = basePitch;
                double bestDistance = Double.MAX_VALUE;

                // Search in a grid pattern around the base rotations
                for (float yawOffset = -15; yawOffset <= 15; yawOffset += 3) {
                    for (float pitchOffset = -15; pitchOffset <= 15; pitchOffset += 3) {
                        float testYaw = baseYaw + yawOffset;
                        float testPitch = basePitch + pitchOffset;

                        // Clamp pitch to valid range
                        testPitch = MathHelper.clamp_float(testPitch, -90, 90);

                        Vector2f testRotations = new Vector2f(testYaw, testPitch);

                        // Check if these rotations can see the target face
                        if (RayCastUtils.overBlock(testRotations, enumFacing.getEnumFacing(), blockFace, rayCast.getValue() == RayCast.Strict)) {
                            // Calculate distance from current rotations for smooth transition
                            double yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(testYaw - RotationProcess.rotations.x));
                            double pitchDiff = Math.abs(testPitch - RotationProcess.rotations.y);
                            double totalDistance = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

                            if (totalDistance < bestDistance) {
                                bestDistance = totalDistance;
                                bestYaw = testYaw;
                                bestPitch = testPitch;
                            }
                        }
                    }
                }

                // If we found valid rotations, use them
                if (bestDistance != Double.MAX_VALUE) {
                    targetYaw = bestYaw;
                    targetPitch = bestPitch;
                } else {
                    // Fallback to basic calculation
                    final Vector2f rotations = RotationUtils.calculate(
                            new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                            enumFacing.getEnumFacing()
                    );
                    targetYaw = rotations.x;
                    targetPitch = rotations.y;
                }
            }
            case Simple -> {
                if (RayCastUtils.overBlock(RotationProcess.rotations, enumFacing.getEnumFacing(), blockFace, true)) {
                    return;
                }
                for (float possibleYaw = mc.thePlayer.rotationYaw - 180; possibleYaw <= mc.thePlayer.rotationYaw + 360 - 180; possibleYaw += 45) {
                    for (float possiblePitch = 90; possiblePitch > 30; possiblePitch -= possiblePitch > (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 60 : 80) ? 1 : 10) {
                        if (RayCastUtils.overBlock(new Vector2f(possibleYaw, possiblePitch), enumFacing.getEnumFacing(), blockFace, true)) {
                            targetYaw = possibleYaw;
                            targetPitch = possiblePitch;
                        }
                    }
                }
            }
        }
    }

    private float getHypixelYaw() {
            final float snappedBase = Math.round(mc.thePlayer.rotationYaw / 45.0f) * 45.0f;
            float lowerOffset;
            float upperOffset;
            if (Math.abs(snappedBase % 90.0f) < 0.001f) {
                lowerOffset = 111.0f;
                upperOffset = 111.0f;
            }
            else {
                lowerOffset = 137.0f;
                upperOffset = 137.0f;
            }
            final float lowerCandidate = snappedBase - lowerOffset;
            final float upperCandidate = snappedBase + upperOffset;
            return (Math.abs(mc.thePlayer.rotationYaw - lowerCandidate) <= Math.abs(upperCandidate - mc.thePlayer.rotationYaw)) ? lowerCandidate : upperCandidate;
    }

    private Vector2f applyRotationLimits(float targetYaw, float targetPitch) {
        if (!limitRotations.getValue()) {
            return new Vector2f(targetYaw, targetPitch);
        }

        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        // Calculate yaw difference
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
        float maxYaw = rotationLimiterYawMax.getValue().floatValue();
        float minYaw = rotationLimiterYawMin.getValue().floatValue();

        // Clamp yaw difference
        if (Math.abs(yawDiff) > maxYaw) {
            yawDiff = yawDiff > 0 ? maxYaw : -maxYaw;
        } else if (Math.abs(yawDiff) < minYaw) {
            yawDiff = yawDiff > 0 ? minYaw : -minYaw;
        }

        float limitedYaw = currentYaw + yawDiff;

        // Calculate pitch difference
        float pitchDiff = targetPitch - currentPitch;
        float maxPitch = rotationLimiterPitchMax.getValue().floatValue();
        float minPitch = rotationLimiterPitchMin.getValue().floatValue();

        // Clamp pitch difference
        if (Math.abs(pitchDiff) > maxPitch) {
            pitchDiff = pitchDiff > 0 ? maxPitch : -maxPitch;
        } else if (Math.abs(pitchDiff) < minPitch) {
            pitchDiff = pitchDiff > 0 ? minPitch : -minPitch;
        }

        float limitedPitch = MathHelper.clamp_float(currentPitch + pitchDiff, -90.0f, 90.0f);

        return new Vector2f(limitedYaw, limitedPitch);
    }

    public Vec3 getHitVec() {
        /* Correct HitVec */
        Vec3 hitVec = new Vec3(blockFace.getX() + Math.random(), blockFace.getY() + Math.random(), blockFace.getZ() + Math.random());

        final MovingObjectPosition movingObjectPosition = RayCastUtils.rayCast(RotationProcess.rotations, mc.playerController.getBlockReachDistance());

        switch (enumFacing.getEnumFacing()) {
            case DOWN:
                hitVec.yCoord = blockFace.getY();
                break;

            case UP:
                hitVec.yCoord = blockFace.getY() + 1;
                break;

            case NORTH:
                hitVec.zCoord = blockFace.getZ();
                break;

            case EAST:
                hitVec.xCoord = blockFace.getX() + 1;
                break;

            case SOUTH:
                hitVec.zCoord = blockFace.getZ() + 1;
                break;

            case WEST:
                hitVec.xCoord = blockFace.getX();
                break;
        }

        if (movingObjectPosition != null && movingObjectPosition.getBlockPos() != null &&
                movingObjectPosition.hitVec != null && movingObjectPosition.getBlockPos().equals(blockFace) &&
                movingObjectPosition.sideHit == enumFacing.getEnumFacing()) {
            hitVec = movingObjectPosition.hitVec;
        }

        return hitVec;
    }


    private void place() {
        Vec3 hitVec = this.getHitVec();
        if (!packetPlace.getValue()) {
            mc.rightClickMouse();
        } else if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockFace, enumFacing.getEnumFacing(), hitVec)) {
            if (!packetSwing.getValue()) {
                mc.thePlayer.swingItem();
            } else {
                PacketUtils.sendPacket(new C0APacketAnimation());
            }
        }
        blocksPlaced++;
        delayTimer.reset();
    }


    public void jump() {
        if (mc.thePlayer.onGroundTicks < jumpDelayTicks.getValue().intValue()) return;

        if (mc.gameSettings.keyBindJump.isKeyDown()) return;

        if (mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.Hypixel) {
            if (autoJump.getValue() == JumpMode.None) autoJump.setValue(JumpMode.Normal);
        }

        if (keepY.getValue() && autoJump.getValue() != JumpMode.None) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving() && mc.thePlayer.posY == startY && (!edge.getValue() || isNearEdge())) {
                handleJump();
            }
        }

        if (autoJump.getValue() != JumpMode.None && !keepY.getValue() && (!edge.getValue() || isNearEdge())) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                handleJump();
            }
        }
    }

    private void handleJump() {
        if (mode.getValue() == Mode.Hypixel && !(MovementUtils.getSpeed() <= 0.02) && mc.thePlayer.offGroundTicks >= 9) {
            return;
        }

        if (autoJump.getValue() == JumpMode.Dev) {
            mc.thePlayer.motionY = 0.42F;
        }
        if (autoJump.getValue() == JumpMode.Normal) {
            mc.thePlayer.jump();
        }
    }

    private void sprint() {
        switch (sprintMode.getValue()) {
            case Vanilla:
                mc.thePlayer.setSprinting(MovementUtils.isMoving());
                break;
            case Legit:
                // Handled in RotationProcess
                break;
            case None:
                mc.gameSettings.keyBindSprint.setPressed(false);
                mc.thePlayer.setSprinting(false);
                break;
        }
    }

    private void sneak() {
        if (sneak.getValue()) {
            if (blocksPlaced >= sneakEvery.getValue().intValue() && !mc.gameSettings.keyBindSneak.isPressed()) {
                mc.gameSettings.keyBindSneak.setPressed(true);
                blocksPlaced = 0;
            } else {
                mc.gameSettings.keyBindSneak.setPressed(false);
            }
        }
    }


    public void tower() {

        if (towerMode.getValue() == TowerMode.None || !mc.gameSettings.keyBindJump.isKeyDown()) {
            return;
        }

        if (!towerMove.getValue() && !MovementUtils.isMoving()) {
            return;
        }

        switch (towerMode.getValue()) {
            case NCP:
                if (mc.thePlayer.posY % 1.0D <= 0.00153598D) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0.41998D;
                } else if (mc.thePlayer.posY % 1.0D < 0.1D && mc.thePlayer.onGround) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                }
                break;
            case Vanilla:
                mc.thePlayer.motionY = 0.42;
                break;
            case Dev:
                switch (mc.thePlayer.offGroundTicks) {
                    case 3:
                        mc.timer.timerSpeed = 1.25f;
                        break;
                    case 4:
                        mc.timer.timerSpeed = 1.12f;
                        break;
                    case 5:
                        mc.timer.timerSpeed = 1.06f;
                        break;
                    case 6:
                        mc.timer.timerSpeed = 1.0f;
                        break;
                }
                break;
        }
    }

    public boolean doesNotContainBlock(int down) {
        return PlayerUtils.blockRelativeToPlayer(offset.getX(), -down + offset.getY(), offset.getZ()).isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down(down));
    }

    public static boolean isNearEdge() {
        final double x = mc.thePlayer.posX;
        final double z = mc.thePlayer.posZ;
        final int y = (int) Math.floor(mc.thePlayer.posY) - 2;
        for (double expand = 0.15, dx = -expand; dx <= expand; dx += expand) {
            for (double dz = -expand; dz <= expand; dz += expand) {
                if (dx != 0.0 || dz != 0.0) {
                    final BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    if (!mc.theWorld.getBlockState(pos).getBlock().isFullBlock()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
