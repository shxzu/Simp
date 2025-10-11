package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.api.events.impl.packet.PacketSendEvent;
import cc.simp.api.events.impl.player.MotionEvent;
import cc.simp.api.events.impl.player.MoveEvent;
import cc.simp.api.events.impl.player.SprintEvent;
import cc.simp.api.events.impl.player.StrafeEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.modules.impl.movement.SpeedModule;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.EnumFacingOffset;
import cc.simp.utils.client.Logger;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.Objects;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Scaffold Walk", category = ModuleCategory.PLAYER)
public final class ScaffoldWalkModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Normal);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 0, 0, 10, 1);
    public final NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 5, 1);
    private final ModeProperty<YawOffset> yawOffset = new ModeProperty<>("Yaw Offset", YawOffset.Zero);
    public static Property<Boolean> sprint = new Property<>("Sprint", false);
    public static Property<Boolean> moveFix = new Property<>("Move Fix", false);
    private final ModeProperty<RayCast> raycast = new ModeProperty<>("Ray Cast", RayCast.None);
    public static Property<Boolean> jump = new Property<>("Auto Jump", false);
    public static Property<Boolean> keepY = new Property<>("Keep Y", false);
    private final Property<Boolean> sneak = new Property<>("Sneak", false);
    public final NumberProperty startSneaking = new NumberProperty("Start Sneaking", 0, sneak::getValue, 0, 5, 1);
    public final NumberProperty stopSneaking = new NumberProperty("Stop Sneaking", 0, sneak::getValue, 0, 5, 1);
    public final NumberProperty sneakEvery = new NumberProperty("Blocks To Sneak", 1, sneak::getValue, 1, 10, 1);
    public final NumberProperty sneakingSpeed = new NumberProperty("Sneaking Speed", 0.2, sneak::getValue, 0.2, 1, 0.05);
    private final NumberProperty expand = new NumberProperty("Expand", 0, 0, 4, 1);
    private final Property<Boolean> render = new Property<>("Render", true);

    private enum Mode {
        Normal,
        Telly,
        Breeze,
        God
    }

    private enum RayCast {
        None,
        Normal,
        Strict
    }

    private enum YawOffset {
        Zero("0"),
        FortyFive("45"),
        NegativeFortyFive("-45");
        public String name;

        YawOffset(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private Vec3 targetBlock;
    private EnumFacingOffset enumFacing;
    public Vec3i offset = new Vec3i(0, 0, 0);
    private BlockPos blockFace;
    private float targetYaw, targetPitch, forward, strafe, yawDrift, pitchDrift;
    @Getter
    @Setter
    private int ticksOnAir;
    private int sneakingTicks;
    private int placements;
    private int slow;
    private int pause;
    public int recursions, recursion;
    public double startY;
    private boolean canPlace;
    private int directionalChange;
    private int blockCount;

    @EventLink
    public final Listener<MotionEvent> motionEventListener = event -> {
        if (!event.isPre()) return;
        this.offset = new Vec3i(0, 0, 0);

        if (targetBlock == null || enumFacing == null || blockFace == null) {
            return;
        }
    };

    public void runMode() {
        if (jump.getValue() && !keepY.getValue()) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.jump();
            }
        }
    }

    @EventLink
    public final Listener<PreUpdateEvent> onPreUpdate = event -> {
        for (recursion = 0; recursion <= recursions; recursion++) {

            resetBinds(false, false, true, true, false, false);

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

            // Getting ItemSlot
            mc.thePlayer.inventory.currentItem = InventoryUtils.findBlock();

            // Used to detect when to place a block, if over air, allow placement of blocks
            if (doesNotContainBlock(1) && (!sameY || (doesNotContainBlock(2) && doesNotContainBlock(3) && doesNotContainBlock(4)))) {
                ticksOnAir++;
            } else {
                ticksOnAir = 0;
            }

            canPlace = mc.thePlayer.inventory.currentItem == InventoryUtils.findBlock() &&
                    ticksOnAir > placeDelay.getValue() * Math.random();

            if (recursion == 0) this.calculateSneaking();

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

            this.calculateRotations();

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
                if (canPlace && (RayCastUtils.overBlock(enumFacing.getEnumFacing(), blockFace, raycast.getValue() == RayCast.Strict) || raycast.getValue() == RayCast.None)) {
                    this.place();

                    // mc.rightClickDelayTimer = 0;
                    ticksOnAir = 0;

                    if (!(mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock)) {
                        mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null;
                    }
                } else if (Math.random() > 0.3 && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit != null &&
                        mc.objectMouseOver.getBlockPos().equals(blockFace) && mc.objectMouseOver.sideHit ==
                        EnumFacing.UP && raycast.getValue() == RayCast.Strict && !(PlayerUtils.blockRelativeToPlayer(0, -1, 0) instanceof BlockAir)) {
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
    public final Listener<MoveEvent> onMove = this::calculateSneaking;

    @EventLink
    public final Listener<StrafeEvent> onStrafe = event -> {
        this.runMode();

        if (!Objects.equals(yawOffset.getValue().name, "0") && !moveFix.getValue()) {
            MovementUtils.useDiagonalSpeed();
        }

        if (keepY.getValue() && jump.getValue()) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving() && mc.thePlayer.posY == startY) {
                mc.thePlayer.jump();
            }
        }
    };

    @EventLink
    public final Listener<PacketSendEvent> onPacketSend = event -> {
        Packet<?> packet = event.getPacket();

        if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08PacketPlayerBlockPlacement = (C08PacketPlayerBlockPlacement) packet;

            if (!c08PacketPlayerBlockPlacement.getPosition().equalsVector(new Vector3d(-1, -1, -1))) {
                placements--;
            }
        }
    };

    @EventLink
    public final Listener<SprintEvent> sprintEventListener = event -> {
        if (sprint.getValue()) {
            mc.thePlayer.setSprinting(MovementUtils.canSprint(true));
            event.setSprinting(MovementUtils.canSprint(true));
        }
    };

    @EventLink
    public final Listener<PacketReceiveEvent> onPacketReceiveEvent = PacketUtils::correctBlockCount;

    @Override
    public void onEnable() {
        targetYaw = mc.thePlayer.rotationYaw - 180 + Integer.parseInt(yawOffset.getValue().toString());
        targetPitch = 90;

        pitchDrift = (float) ((Math.random() - 0.5) * (Math.random() - 0.5) * 10);
        yawDrift = (float) ((Math.random() - 0.5) * (Math.random() - 0.5) * 10);

        startY = Math.floor(mc.thePlayer.posY);
        targetBlock = null;

        this.sneakingTicks = -1;
        recursions = 0;
        placements = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetBinds();
        super.onDisable();
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

    public void calculateSneaking() {
        if (ticksOnAir == 0) mc.gameSettings.keyBindSneak.setPressed(false);

        this.sneakingTicks--;

        if (!this.sneak.getValue() && pause <= 0) {
            return;
        }

        int ahead = startSneaking.getRandomBetween().intValue();
        int place = placeDelay.getRandomBetween().intValue();
        int after = stopSneaking.getRandomBetween().intValue();

        if (pause > 0) {
            pause--;

            sneakingTicks = 0;
            placements = 0;
        }

        if (this.sneakingTicks >= 0) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            return;
        }

        if (ticksOnAir > 0) {
            this.sneakingTicks = (int) (double) (after);
        }

        if (ticksOnAir > 0 || PlayerUtils.blockRelativeToPlayer(mc.thePlayer.motionX * ahead, MovementUtils.HEAD_HITTER_MOTION, mc.thePlayer.motionZ * ahead) instanceof BlockAir) {
            if (placements <= 0) {
                this.sneakingTicks = (int) (double) (ahead + place + after);
                placements = sneakEvery.getRandomBetween().intValue();
            }
        }
    }

    public void calculateSneaking(MoveEvent moveEvent) {
        forward = moveEvent.getForward();
        strafe = moveEvent.getStrafe();

        if (slow-- > 0) {
            moveEvent.setForward(0);
            moveEvent.setStrafe(0);
        }

        if (!this.sneak.getValue()) {
            return;
        }

        double speed = this.sneakingSpeed.getValue().doubleValue();

        if (speed <= 0.2) {
            return;
        }

        moveEvent.setSneakSlowDownMultiplier(speed);
    }

    public void calculateRotations() {
        int yawOffset = Integer.parseInt(this.yawOffset.getValue().name);

        /* Smoothing rotations */
        final double minRotationSpeed = this.rotationSpeed.getValue();
        final double maxRotationSpeed = this.rotationSpeed.getValue() * Math.random();
        float rotationSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);

        MovementFix movementFix = moveFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF;

        /* Calculating target rotations */
        switch (mode.getValue()) {
            case Normal:
                mc.entityRenderer.getMouseOver(1);

                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        getRotations(yawOffset);
                    }
                }

                /* else {
                    Vector3d position = MoveUtil.predictedPosition();
                    Vector3d current = new Vector3d(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    mc.thePlayer.setPosition(position.getX(), position.getY(), position.getZ());

                    if (PlayerUtil.blockRelativeToPlayer(0,-0.1,0) instanceof BlockAir) {
                        ChatUtil.display("Set");
                        getRotations(yawOffset);
                    }

                    mc.thePlayer.setPosition(current.getX(), current.getY(), current.getZ());
                }*/
                break;

            case Breeze:
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
            case Telly:
                if (recursion == 0) {
                    int time = mc.thePlayer.offGroundTicks;

                    if (time == 2 || time == 0) mc.rightClickMouse();

                    if (time >= 3 && mc.thePlayer.offGroundTicks <= (!keepY.getValue() ? 7 : 10)) {
                        if (!RayCastUtils.overBlock(RotationProcess.rotations, enumFacing.getEnumFacing(), blockFace, raycast.getValue().equals(RayCast.Strict))) {
                            getRotations(0);
                        }
                    } else {
                        getRotations(Integer.parseInt(String.valueOf(this.yawOffset.getValue().name)));
                        targetYaw = mc.thePlayer.rotationYaw;
                    }

                    if (mc.thePlayer.offGroundTicks <= 3) {
                        canPlace = false;
                    }
                }
                break;

            case God:
                if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock && canPlace) {
                    mc.rightClickMouse();
                }

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

        if (rotationSpeed != 0 && blockFace != null && enumFacing != null) {
            RotationProcess.setRotations(new Vector2f(targetYaw, targetPitch), rotationSpeed, movementFix);
        }
    }

    public void getRotations(final int yawOffset) {
        EntityPlayer player = mc.thePlayer;
        double difference = player.posY + player.getEyeHeight() - targetBlock.yCoord -
                0.5 - (Math.random() - 0.5) * 0.1;

        MovingObjectPosition movingObjectPosition = null;

        for (int offset = -180 + yawOffset; offset <= 180; offset += 45) {
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
        if (pause > 3) return;

        Vec3 hitVec = this.getHitVec();

        if (raycast.getValue() == RayCast.Strict) {
            mc.rightClickMouse();
        } else if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockFace, enumFacing.getEnumFacing(), hitVec)) {
            mc.thePlayer.swingItem();
        }
    }


    public boolean doesNotContainBlock(int down) {
        return PlayerUtils.blockRelativeToPlayer(offset.getX(), -down + offset.getY(), offset.getZ()).isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down(down));
    }

}

