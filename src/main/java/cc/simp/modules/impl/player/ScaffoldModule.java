package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.SprintEvent;
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
import cc.simp.processes.LagProcess;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.EnumFacingOffset;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.MathUtils;
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
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static cc.simp.utils.Util.mc;
import static net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING;

@ModuleInfo(label = "Scaffold", category = ModuleCategory.PLAYER)
public final class ScaffoldModule extends Module {

    private static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Normal);
    private static final ModeProperty<SearchAlgorithm> searchAlgorithm = new ModeProperty<>("Search Algorithm", SearchAlgorithm.Normal);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 8, 0, 10, 1);
    public final NumberProperty placeDelay = new NumberProperty("Place CPS", 15, 1, 40, 1);
    public static Property<Boolean> swing = new Property<>("Swing", false);
    public static Property<Boolean> sprint = new Property<>("Sprint", false);
    public static Property<Boolean> moveFix = new Property<>("Move Fix", true);
    private final ModeProperty<RayCast> rayCast = new ModeProperty<>("Ray Cast", RayCast.Normal);
    public static Property<Boolean> jump = new Property<>("Auto Jump", false, () -> mode.getValue() != Mode.SlowTelly && mode.getValue() != Mode.FastTelly && mode.getValue() != Mode.Hypixel);
    public static Property<Boolean> edge = new Property<>("Jump Only On Edge", false, () -> mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.Hypixel || jump.getValue());
    private final NumberProperty jumpDelayTicks = new NumberProperty("Jump Delay Ticks", 0, () -> mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.Hypixel || jump.getValue(), 0, 5, 1);
    public static Property<Boolean> keepY = new Property<>("Keep Y", false);
    private static final Property<Boolean> safeWalk = new Property<>("Safe Walk", false);
    private final NumberProperty expand = new NumberProperty("Expand", 0, 0, 4, 1);
    private final Property<Boolean> render = new Property<>("Render Selection", true);
    private final Property<Boolean> counter = new Property<>("Block Counter", true);

    private enum Mode {
        Normal("Normal"),
        StaticYaw("Static Yaw"),
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

    private enum RayCast {
        None,
        Normal,
        Strict
    }

    private enum SearchAlgorithm {
        Normal,
        Best,
        Extra
    }

    private long lastPlaceTime = 0;
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
    private boolean blinked = false;

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
            mc.thePlayer.safeWalk = true;
        }

        for (recursion = 0; recursion <= recursions; recursion++) {

            // Calculate interval based on CPS (1000ms / CPS = ms between clicks)
            long currentTime = System.currentTimeMillis();
            long requiredInterval = (long) (1000.0 / placeDelay.getValue());

            // Add randomization to make it more human-like (±10% variation)
            long randomizedInterval = (long) (requiredInterval * (0.9 + Math.random() * 0.2));

            if (!overrided) {
                this.rotSpeed = (float) MathUtils.getRandom(this.rotationSpeed.getValue(), this.rotationSpeed.getValue() * Math.random());
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
                    ticksOnAir > 0 && (currentTime - lastPlaceTime) >= randomizedInterval;

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

                } else if (Math.random() > 0.3 && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                        mc.objectMouseOver.getBlockPos().equals(blockFace) && mc.objectMouseOver.sideHit ==
                        EnumFacing.UP && rayCast.getValue() == RayCast.Strict && !(PlayerUtils.blockRelativeToPlayer(0, -1, 0) instanceof BlockAir)) {
                    mc.rightClickMouse();
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
        this.jump();
    };

    @EventLink
    public final Listener<SprintEvent> sprintEventListener = event -> {
        if (sprint.getValue()) {
            mc.thePlayer.setSprinting(true);
            event.setSprinting(true);
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
            final float red = 1.0f;
            final float green = 1.0f;
            final float blue = 1.0f;
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
            if (blinked) {
                LagProcess.disable();
                LagProcess.dispatch();
            }
        }
        resetBinds();
        super.onDisable();
    }

    private void renderBlockCounter() {
        if (!counter.getValue()) return;
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
                    }
                }
                break;

            case StaticYaw:
                mc.entityRenderer.getMouseOver(1);

                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        getBaseRotations();
                        targetYaw = mc.thePlayer.rotationYaw - 180;
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
                    if (MovementUtils.isOnGround()) {
                        if (MovementUtils.isMoving()) {
                            targetYaw = mc.thePlayer.rotationYaw;
                            canPlace = false;
                        }
                    } else {
                        mc.entityRenderer.getMouseOver(1);

                        if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                            if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                                getBaseRotations();
                            }
                        }
                    }
                }
                break;
            case Hypixel:
                if (recursion == 0) {
                    if (!mc.thePlayer.onGround) {
                        mc.entityRenderer.getMouseOver(1);

                        if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                            if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                                getBaseRotations();
                            }
                        }
                    } else {
                        float candidatePos = 60f;
                        float candidateNeg = -60f;
                        float playerYaw = mc.thePlayer.rotationYaw;
                        float diffPos = Math.abs(MathHelper.wrapAngleTo180_float(candidatePos - playerYaw));
                        float diffNeg = Math.abs(MathHelper.wrapAngleTo180_float(candidateNeg - playerYaw));
                        targetYaw = (diffPos <= diffNeg) ? candidatePos : candidateNeg;
                        canPlace = false;
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
            case Best -> {
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
            case Extra -> {
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

        if (rayCast.getValue() == RayCast.Strict) {
            mc.rightClickMouse();
        } else if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockFace, enumFacing.getEnumFacing(), hitVec)) {
            if (swing.getValue()) mc.thePlayer.swingItem();
            else PacketUtils.sendPacket(new C0APacketAnimation());
        }
        lastPlaceTime = System.currentTimeMillis();
    }

    public void jump() {
        if (mc.gameSettings.keyBindJump.isPressed()) return;

        if (mc.thePlayer.onGroundTicks < jumpDelayTicks.getValue().intValue()) return;

        if (jump.getValue()) {
            if (mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.Hypixel) {
                jump.setValue(false);
            }
        }
        if (keepY.getValue() && jump.getValue() || (mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.Hypixel && keepY.getValue())) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving() && mc.thePlayer.posY == startY && (!edge.getValue() || isNearEdge())) {
                mc.thePlayer.jump();
            }
        }
        if (jump.getValue() || (mode.getValue() == Mode.FastTelly || mode.getValue() == Mode.SlowTelly || mode.getValue() == Mode.Hypixel) && !keepY.getValue() && (!edge.getValue() || isNearEdge())) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.jump();
            }
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
