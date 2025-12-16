package cc.simp.modules.impl.player;

import cc.simp.Simp;
import cc.simp.api.events.impl.game.PreUpdateEvent;
import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.Render3DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.FontProcess;
import cc.simp.processes.RotationProcess;
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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
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
    private final NumberProperty maxRotationSpeed = new NumberProperty("Max Rotation Speed", 8, 0, 10, 1);
    private final ModeProperty<RayCast> rayCast = new ModeProperty<>("Ray Cast", RayCast.Normal);
    public static Property<Boolean> limitRotations = new Property<>("Limit Rotations", false);
    private final NumberProperty rotationLimiterYawMax = new NumberProperty("Yaw Max", 30, limitRotations::getValue, 30, 180, 1);
    private final NumberProperty rotationLimiterYawMin = new NumberProperty("Yaw Min", 30, limitRotations::getValue, 30, 180, 1);
    private final NumberProperty rotationLimiterPitchMax = new NumberProperty("Pitch Max", 30, limitRotations::getValue, 20, 90, 1);
    private final NumberProperty rotationLimiterPitchMin = new NumberProperty("Pitch Min", 30, limitRotations::getValue, 20, 90, 1);

    // Placing Settings
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 10, 1);
    private static final ModeProperty<SlotMode> slotMode = new ModeProperty<>("Slot Mode", SlotMode.Switch);
    public Property<Boolean> packetPlace = new Property<>("Packet Place", false);
    public Property<Boolean> packetSwing = new Property<>("Packet Swing", false);

    // Movement Settings
    private static final ModeProperty<SprintMode> sprintMode = new ModeProperty<>("Sprint Mode", SprintMode.None);
    private static final ModeProperty<TowerMode> towerMode = new ModeProperty<>("Tower Mode", TowerMode.None);
    private static final Property<Boolean> towerMove = new Property<>("Tower Move", true, () -> towerMode.getValue() != TowerMode.None);
    public static Property<Boolean> jump = new Property<>("Auto Jump", false);
    private final NumberProperty jumpDelayTicks = new NumberProperty("Auto Jump Delay", 0, jump::getValue, 0, 5, 1);
    public Property<Boolean> sameY = new Property<>("Same Y", false);
    public static Property<Boolean> moveFix = new Property<>("Move Fix", false);
    private static final Property<Boolean> safeWalk = new Property<>("Safe Walk", false);
    private static final Property<Boolean> safeWalkOnAir = new Property<>("Safe Walk On Air", false, safeWalk::getValue);

    // Render Settings
    private static final ModeProperty<BlockCounter> blockCounter = new ModeProperty<>("Block Counter", BlockCounter.None);
    private final Property<Boolean> render = new Property<>("Render Block Selection", true);

    private enum Mode {
        Normal,
        Telly,
    }

    private enum Rotations {
        Normal("Normal"),
        Radium("Direct"),
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
        NCP,
        None
    }

    private enum BlockCounter {
        Tenacity,
        None
    }

    private Animation anim = new DecelerateAnimation(250, 1);
    private final Timer delayTimer = new Timer();
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private int placeY = 256;
    private int stage = 0;
    private boolean shouldSameY = false;
    private boolean towering = false;
    private float rotSpeed;

    @EventLink
    public final Listener<PreUpdateEvent> preUpdateEventListener = event -> {

        // Setting Variables
        if (!this.checkedModulesNotEnabled()) {
            return;
        }

        this.rotSpeed = (float) MathUtils.getRandom(this.minRotationSpeed.getValue(), this.maxRotationSpeed.getValue());

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
        if (getBlockData() == null || getHitVec() == null) {
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

        // Jumping Logic
        this.jump();

        // Sprinting Logic
        this.sprint();

        // Tower Logic
        this.tower();

        // Safe Walk Logic
        if (safeWalk.getValue()) {
            if (!safeWalkOnAir.getValue() && !mc.thePlayer.onGround) mc.thePlayer.safeWalk = false;
            mc.thePlayer.safeWalk = true;
        }
    };

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
            if (this.getBlockData() != null && this.getBlockData().blockPos() != null) {
                if (mc.thePlayer.getDistance(this.getBlockData().blockPos().getX(), this.getBlockData().blockPos().getY(), this.getBlockData().blockPos().getZ()) > 1.0) {
                    double d0 = 1.0 - mc.thePlayer.getDistance(this.getBlockData().blockPos().getX(), this.getBlockData().blockPos().getY(), this.getBlockData().blockPos().getZ()) / 20.0;
                    if (d0 < 0.3) {
                        d0 = 0.3;
                    }
                    lineWidth *= (float) d0;
                }
                RenderUtils.drawBlockESP(this.getBlockData().blockPos(), red, green, blue, 0.3137255f, 1.0f, lineWidth);
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

    private void jump() {
        if (mc.gameSettings.keyBindJump.isPressed()) return;

        if (mc.thePlayer.onGroundTicks < jumpDelayTicks.getValue().intValue()) return;

        if (jump.getValue()) {
            if (mode.getValue() == Mode.Telly) {
                jump.setValue(false);
            }
        }
        if (sameY.getValue() && jump.getValue() || (mode.getValue() == Mode.Telly && sameY.getValue())) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving() && mc.thePlayer.posY == placeY) {
                mc.thePlayer.jump();
            }
        }
        if (jump.getValue() && !sameY.getValue() || (mode.getValue() == Mode.Telly) && !sameY.getValue()) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.jump();
            }
        }
    }

    private void tower() {
        if (towerMode.getValue() == TowerMode.None) {
            this.towering = false;
            return;
        }

        if (!mc.gameSettings.keyBindJump.isPressed()) {
            this.towering = false;
            return;
        }

        if (!towerMove.getValue() || MovementUtils.isMoving()) {
            switch (towerMode.getValue()) {
                case Vanilla:
                    mc.thePlayer.motionY = 0.42;
                    break;
                case Watchdog:
                    // TODO: Implement Watchdog Tower
                    break;
                case NCP:
                    PacketUtils.sendSilentPacket(new C08PacketPlayerBlockPlacement(null));
                    if (mc.thePlayer.posY % 1 <= 0.00153598) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                        mc.thePlayer.motionY = 0.42F;
                    } else if (mc.thePlayer.posY % 1 < 0.1 && mc.thePlayer.offGroundTicks != 0) {
                        mc.thePlayer.motionY = 0;
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    }
                    break;
            }
            this.towering = true;
        }
    }

    private void getRotations() {
        float currentYaw = mc.thePlayer.rotationYaw;

        switch (rotations.getValue()) {
            case Normal:
                getBaseRotations();
                break;
            case Radium:
                final Vec3 hitVec = getHitVec();

                final double xDif = hitVec.xCoord - mc.thePlayer.posX;
                final double zDif = hitVec.zCoord - mc.thePlayer.posZ;

                final double yDif = hitVec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
                final double xzDist = StrictMath.sqrt(xDif * xDif + zDif * zDif);
                yaw = (float) (StrictMath.atan2(zDif, xDif) * 180.0D / StrictMath.PI) - 90.0F;
                pitch = (float) (-(StrictMath.atan2(yDif, xzDist) * 180.0D / StrictMath.PI));
                break;
            case GodBridge:
                getBaseRotations();
                yaw = currentYaw - 165.0f;
                break;
        }

        if (mode.getValue() == Mode.Telly && !towering) {
            if (!(mc.thePlayer.offGroundTicks >= 3 && mc.thePlayer.offGroundTicks <= (sameY.getValue() ? 7 : 10))) {
                yaw = mc.thePlayer.rotationYaw;
            }
        }

        Vector2f limitedRotations = applyRotationLimits(yaw, pitch);
        yaw = limitedRotations.x;
        pitch = limitedRotations.y;
        if (getBlockData() != null || getHitVec() != null) {
            RotationProcess.setRotations(new Vector2f(yaw, pitch), rotSpeed, moveFix.getValue() ? MovementFix.NORMAL : MovementFix.OFF);
        }
    }

    private void getBaseRotations() {
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
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (delayTimer.hasTimeElapsed(placeDelay.getValue() * 20)) {
            ItemStack itemStack = mc.thePlayer.inventory.getCurrentItem();

            if (slotMode.getValue() == SlotMode.Spoof) {
                for (int i = 0; i < 9; i++) {
                    ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(i);
                    if (InventoryUtils.isBlock(candidate)) {
                        itemStack = candidate;
                        break;
                    }
                }
            }

            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && this.blockCount > 0) {
                if (!packetPlace.getValue()) {
                    mc.rightClickMouse();
                } else if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, blockPos, enumFacing, vec3)) {
                    if (!packetSwing.getValue()) {
                        mc.thePlayer.swingItem();
                    } else {
                        PacketUtils.sendPacket(new C0APacketAnimation());
                    }
                }
                delayTimer.reset();
            }
        }
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

        //Set initial targetPos
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldSameY ? Math.min(startY, this.placeY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );

        ArrayList<BlockPos> positions = new ArrayList<>();

        //Find an adjacent blockPos for scaffold
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 0; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.add(x, y, z);

                    if (!PlayerUtils.isReplaceable(pos)
                            && !PlayerUtils.isInteractable(pos)
                            && mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                            <= mc.playerController.getBlockReachDistance()
                            && (this.stage == 0 || this.shouldSameY || pos.getY() < this.placeY)) {

                        for (EnumFacing facing : EnumFacing.VALUES) {
                            if (facing != EnumFacing.DOWN) {
                                BlockPos offset = pos.offset(facing);
                                if (PlayerUtils.isReplaceable(offset)) {
                                    positions.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        //Fix the rot flick issue by keeping a fallback block? and making sure its not air so the rest of the logic works
        if (mc.theWorld.getBlockState(targetPos).getBlock() != Blocks.air && !positions.contains(targetPos)) {
            positions.add(targetPos);
        }

        //Sorting logic
        if (positions.isEmpty()) {
            return null;
        }

        positions.sort(Comparator.comparingDouble(
                o -> o.distanceSqToCenter(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5)
        ));

        BlockPos blockPos = positions.get(0);
        EnumFacing facing = this.getBestFacing(blockPos, targetPos);

        //Return the block position :exploding_head:
        return new BlockData(blockPos, facing != null ? facing : EnumFacing.UP);
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

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            anim = new DecelerateAnimation(250, 1);
            lastSlot = mc.thePlayer.inventory.currentItem;
            yaw = mc.thePlayer.rotationYaw - 180;
            pitch = 90;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            anim = new DecelerateAnimation(250, 1);
            mc.thePlayer.safeWalk = false;
            this.towering = false;
            if (slotMode.getValue() == SlotMode.Spoof && lastSlot != -1) {
                PacketUtils.sendPacket(new C09PacketHeldItemChange(lastSlot));
            }
            if (slotMode.getValue() == SlotMode.Switch && lastSlot != -1) {
                mc.thePlayer.inventory.currentItem = lastSlot;
            }
        }
        super.onDisable();
    }

}
