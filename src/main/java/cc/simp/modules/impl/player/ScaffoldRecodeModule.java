package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.RotationProcess;
import cc.simp.utils.client.MathUtils;
import cc.simp.utils.mc.*;
import cc.simp.utils.misc.MovementFix;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Scaffold Recode", category = ModuleCategory.PLAYER)
public final class ScaffoldRecodeModule extends Module {

    // Mode Setting
    private static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.Normal);

    // Rotation Settings
    private static final ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.Normal);
    private final NumberProperty minRotationSpeed = new NumberProperty("Min Rotation Speed", 5, 0, 10, 1);
    private final NumberProperty maxRotationSpeed = new NumberProperty("Max Rotation Speed", 8, 0, 20, 1);
    private final ModeProperty<RayCast> rayCast = new ModeProperty<>("Ray Cast", RayCast.Normal);
    public Property<Boolean> snapRotations = new Property<>("Snap Rotations", false);
    public static Property<Boolean> limitRotations = new Property<>("Limit Rotations", false);
    private NumberProperty rotationLimiterYawMax = new NumberProperty("Yaw Max", 0f, limitRotations::getValue, 30f, 180f, 1f);
    private NumberProperty rotationLimiterYawMin = new NumberProperty("Yaw Min", 0f, limitRotations::getValue, 30f, 180f, 1f);
    private NumberProperty rotationLimiterPitchMax = new NumberProperty("Pitch Max", 0f, limitRotations::getValue, 20f, 90f, 1f);
    private NumberProperty rotationLimiterPitchMin = new NumberProperty("Pitch Min", 0f, limitRotations::getValue, 20f, 90f, 1f);

    // Placing Settings
    private NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 5, 1);
    private static final ModeProperty<SlotMode> slotMode = new ModeProperty<>("Slot Mode", SlotMode.Switch);
    public Property<Boolean> packetPlace = new Property<>("Packet Place", false);
    public Property<Boolean> packetSwing = new Property<>("Packet Swing", false);

    // Movement Settings
    private static final ModeProperty<SprintMode> sprintMode = new ModeProperty<>("Sprint Mode", SprintMode.None);
    private static final ModeProperty<TowerMode> towerMode = new ModeProperty<>("Tower Mode", TowerMode.None);
    public static Property<Boolean> jump = new Property<>("Auto Jump", false);
    private final NumberProperty jumpDelayTicks = new NumberProperty("Auto Jump Delay", 0, jump::getValue, 0, 5, 1);
    public Property<Boolean> sameY = new Property<>("Same Y", false);
    public static Property<Boolean> moveFix = new Property<>("Move Fix", false);
    private static final Property<Boolean> safeWalk = new Property<>("Safe Walk", false);

    // Render Settings
    private final Property<Boolean> render = new Property<>("Render Selection", true);
    private final Property<Boolean> counter = new Property<>("Block Counter", true);

    private enum Mode {
        Normal,
        Telly,
    }

    private enum Rotations {
        Normal("Normal"),
        Direct("Direct"),
        GodBridge("God Bridge");

        public String name;

        Rotations(String name) {
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

    private enum SlotMode {
        Switch,
        Spoof
    }

    private enum SprintMode {
        Vanilla,
        Legit,
        None
    }

    private enum TowerMode {
        Vanilla,
        Watchdog,
        Verus,
        None
    }

    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private int placeY = 256;
    private int stage = 0;
    private boolean shouldSameY = false;
    private boolean towering = false;
    private float rotSpeed;
    private boolean overrided;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {

        // Setting Variables
        if (this.checkedModulesNotEnabled()) {
            return;
        }

        if (!overrided) {
            this.rotSpeed = (float) MathUtils.getRandom(this.minRotationSpeed.getValue(), this.maxRotationSpeed.getValue());
        }

        if (mc.thePlayer.onGround) {
            if (this.stage > 0) {
                this.stage--;
            }
            if (this.stage < 0) {
                this.stage++;
            }
            if (this.stage == 0
                    && !sameY.getValue()
                    && !mc.thePlayer.isUsingItem()
                    && !mc.gameSettings.keyBindJump.isKeyDown()) {
                this.stage = 1;
            }
            this.placeY = this.shouldSameY ? this.placeY : MathHelper.floor_double(mc.thePlayer.posY);
            this.shouldSameY = false;
            this.towering = false;
        }

        // Slot Management
        ItemStack stack = mc.thePlayer.getHeldItem();
        int count = InventoryUtils.isBlock(stack) ? stack.stackSize : 0;
        this.blockCount = Math.min(this.blockCount, count);
        if (this.blockCount <= 0) {
            int slot = mc.thePlayer.inventory.currentItem;
            if (this.blockCount == 0) {
                slot--;
            }
            for (int i = slot; i > slot - 9; i--) {
                int hotbarSlot = (i % 9 + 9) % 9;
                ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                if (InventoryUtils.isBlock(candidate)) {
                    if (slotMode.getValue() == SlotMode.Spoof) {
                        PacketUtils.sendPacket(new C09PacketHeldItemChange(hotbarSlot));
                    } else if (slotMode.getValue() == SlotMode.Switch) {
                        mc.thePlayer.inventory.currentItem = hotbarSlot;
                    }
                    this.blockCount = candidate.stackSize;
                    break;
                }
            }
        }

        // Null Check
        if (getBlockData() == null || getBlockData().blockPos() == null || getHitVec() == null || getBlockData().facing() == null) {
            return;
        }

        // Setting Rotations
        getRotations();

        // More Null Checks
        if (placeY - 1 != Math.floor(getHitVec().yCoord) && sameY.getValue()) {
            return;
        }

        if (mc.thePlayer.inventory.getCurrentItem() == null || !(mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock)) {
            return;
        }

        // Placing Logic
        if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
            if (RayCastUtils.overBlock(getBlockData().facing(), getBlockData().blockPos(), rayCast.getValue() == RayCast.Strict) || rayCast.getValue() == RayCast.None) {
                this.place(getBlockData().blockPos(), getBlockData().facing(), getHitVec());
            }
        }
    };

    private void getRotations() {
        float currentYaw = mc.thePlayer.rotationYaw;
        if (getBlockData() == null || getHitVec() == null) {
            yaw = currentYaw - 180.0f;
            pitch = 82.5f;
            return;
        }
        switch (rotations.getValue()) {
            case Normal:
                EntityPlayer player = mc.thePlayer;
                double difference = player.posY + player.getEyeHeight() - getHitVec().yCoord -
                        0.5 - (Math.random() - 0.5) * 0.1;

                MovingObjectPosition movingObjectPosition = null;

                for (int offset = -180; offset <= 180; offset += 45) {
                    player.setPosition(player.posX, player.posY - difference, player.posZ);
                    movingObjectPosition = RayCastUtils.rayCast(new Vector2f((float) (player.rotationYaw + (offset * 3)), 0), 4.5);
                    player.setPosition(player.posX, player.posY + difference, player.posZ);

                    if (movingObjectPosition == null || movingObjectPosition.hitVec == null) return;

                    Vector2f rotations = RotationUtils.calculate(movingObjectPosition.hitVec);

                    if (RayCastUtils.overBlock(rotations, getBlockData().blockPos(), getBlockData().facing())) {
                        yaw = rotations.x;
                        pitch = rotations.y;
                        return;
                    }
                }

                final Vector2f rotations = RotationUtils.calculate(
                        new Vector3d(getBlockData().blockPos().getX(), getBlockData().blockPos().getY(), getBlockData().blockPos().getZ()), getBlockData().facing());

                if (!RayCastUtils.overBlock(new Vector2f(yaw, pitch), getBlockData().blockPos(), getBlockData().facing())) {
                    yaw = rotations.x;
                    pitch = rotations.y;
                }
                break;
            case Direct:
                final Vec3 hitVec = getHitVec();

                final double xDif = hitVec.xCoord - mc.thePlayer.posX;
                final double zDif = hitVec.zCoord - mc.thePlayer.posZ;

                final double yDif = hitVec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
                final double xzDist = StrictMath.sqrt(xDif * xDif + zDif * zDif);
                yaw = (float) (StrictMath.atan2(zDif, xDif) * 180.0D / StrictMath.PI) - 90.0F;
                pitch = (float) (-(StrictMath.atan2(yDif, xzDist) * 180.0D / StrictMath.PI));
                break;
            case GodBridge:
                float optimalPitch = 82.5f;
                if (!RayCastUtils.overBlock(new Vector2f(yaw, pitch), getBlockData().blockPos(), getBlockData().facing())) {
                    optimalPitch = RotationUtils.calculate(new Vector3d(getBlockData().blockPos().getX(), getBlockData().blockPos().getY(), getBlockData().blockPos().getZ()), getBlockData().facing()).getX();
                }
                pitch = optimalPitch;
                yaw = currentYaw - 165.0f;
                break;
        }

        if (mode.getValue() == Mode.Telly) {
            if (!(mc.thePlayer.offGroundTicks >= 3 && mc.thePlayer.offGroundTicks <= (sameY.getValue() ? 7 : 10))) {
                yaw = mc.thePlayer.rotationYaw;
            }
        }

        RotationProcess.setRotations(new Vector2f(yaw, pitch), rotSpeed, moveFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock && this.blockCount > 0) {
            if (!packetPlace.getValue()) {
                mc.rightClickMouse();
            } else if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockPos, enumFacing, vec3)) {
                if (packetSwing.getValue()) {
                    mc.thePlayer.swingItem();
                } else {
                    PacketUtils.sendPacket(new C0APacketAnimation());
                }
            }
        }
    }

    private boolean checkedModulesNotEnabled() {
        return !Simp.INSTANCE.getModuleManager().getModule(BedNukerModule.class).isEnabled() || BedNukerModule.bedPos == null;
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldSameY ? Math.min(startY, this.placeY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );
        if (!PlayerUtils.isReplaceable(targetPos)) {
            return null;
        } else {
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!PlayerUtils.isReplaceable(pos)
                                && !PlayerUtils.isInteractable(pos)
                                && !(
                                mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5)
                                        > (double) mc.playerController.getBlockReachDistance()
                        )
                                && (this.stage == 0 || this.shouldSameY || pos.getY() < this.placeY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (PlayerUtils.isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
                        )
                );
                BlockPos blockPos = positions.get(0);
                EnumFacing facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    public Vec3 getHitVec() {

        if (getBlockData() == null) {
            return null;
        }

        Vec3 hitVec = new Vec3(getBlockData().blockPos().getX() + Math.random(), getBlockData().blockPos().getY() + Math.random(), getBlockData().blockPos().getZ() + Math.random());

        final MovingObjectPosition movingObjectPosition = RayCastUtils.rayCast(RotationProcess.rotations, mc.playerController.getBlockReachDistance());

        switch (getBlockData().facing()) {
            case DOWN:
                hitVec.yCoord = getBlockData().blockPos().getY();
                break;

            case UP:
                hitVec.yCoord = getBlockData().blockPos().getY() + 1;
                break;

            case NORTH:
                hitVec.zCoord = getBlockData().blockPos().getZ();
                break;

            case EAST:
                hitVec.xCoord = getBlockData().blockPos().getX() + 1;
                break;

            case SOUTH:
                hitVec.zCoord = getBlockData().blockPos().getZ() + 1;
                break;

            case WEST:
                hitVec.xCoord = getBlockData().blockPos().getX();
                break;
        }

        if (movingObjectPosition != null && movingObjectPosition.getBlockPos() != null &&
                movingObjectPosition.hitVec != null && movingObjectPosition.getBlockPos().equals(getBlockData().blockPos()) &&
                movingObjectPosition.sideHit == getBlockData().facing()) {
            hitVec = movingObjectPosition.hitVec;
        }

        return hitVec;
    }

    private double getRandomOffset() {
        return 0.2155 - MathUtils.getRandom(1.0E-4, 9.0E-4);
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            case WEST:
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }

}
