package net.akarisakai.basicmobsmod.entity.ai.alligator;


import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;


public class AlligatorMoveControl extends MoveControl {
    private static final float MAX_PITCH_CHANGE = 5.0F;
    private static final float MAX_YAW_CHANGE = 90.0F;
    private final int pitchChange;
    private final int yawChange;
    private final float speedInWater;
    private final float speedOnLand;
    private final boolean buoyant;
    private boolean wasPreviouslyInWater;

    public AlligatorMoveControl(MobEntity entity, int pitchChange, int yawChange, float speedInWater, float speedOnLand, boolean buoyant) {
        super(entity);
        this.pitchChange = pitchChange;
        this.yawChange = yawChange;
        this.speedInWater = speedInWater;
        this.speedOnLand = speedOnLand;
        this.buoyant = buoyant;
        this.wasPreviouslyInWater = entity.isTouchingWater();
    }

    public void tick() {
        boolean isInWater = this.entity.isTouchingWater();

        if (isInWater && !wasPreviouslyInWater) {
            stopMovement();
            handleWaterMovement();
        } else if (!isInWater && wasPreviouslyInWater) {
            stopMovement();
            handleLandMovement();
        } else {
            if (isInWater) {
                handleWaterMovement();
            } else {
                handleLandMovement();
            }
        }

        wasPreviouslyInWater = isInWater;
    }



    private void handleWaterMovement() {
        boolean targetIsOnLand = this.targetY > this.entity.getWorld().getSeaLevel() &&
                !this.entity.getWorld().getBlockState(new BlockPos((int) this.targetX, (int) this.targetY, (int) this.targetZ)).isLiquid();

        if (targetIsOnLand) {
            this.targetY = this.entity.getWorld().getSeaLevel() - 0.5;

            if (this.entity.getY() >= this.entity.getWorld().getSeaLevel() - 1.0) {
                handleLandMovement();
                return;
            }
        }
        if (this.buoyant && !(entity instanceof AlligatorEntity && ((AlligatorEntity)entity).isResting())) {
            if(this.entity.isSubmergedInWater()) {
                this.entity.setVelocity(this.entity.getVelocity().add(0.0, 0.005, 0.0));
            }
        }

        if (this.state == State.MOVE_TO && !this.entity.getNavigation().isIdle()) {
            double dx = this.targetX - this.entity.getX();
            double dy = this.targetY - this.entity.getY();
            double dz = this.targetZ - this.entity.getZ();
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared < 2.5000003E-7F) {
                this.entity.setForwardSpeed(0.0F);
                return;
            }

            float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
            this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), targetYaw, this.yawChange));
            this.entity.bodyYaw = this.entity.getYaw();
            this.entity.headYaw = this.entity.getYaw();

            float baseSpeed = (float) (this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
            this.entity.setMovementSpeed(baseSpeed * this.speedInWater);

            if (Math.abs(dy) > 1.0E-5F) {
                float targetPitch = -((float) (MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180F / Math.PI)));
                targetPitch = MathHelper.clamp(MathHelper.wrapDegrees(targetPitch), -this.pitchChange, this.pitchChange);
                this.entity.setPitch(this.wrapDegrees(this.entity.getPitch(), targetPitch, MAX_PITCH_CHANGE));
            }

            float pitchRad = this.entity.getPitch() * (float) (Math.PI / 180F);
            this.entity.forwardSpeed = MathHelper.cos(pitchRad) * baseSpeed;
            this.entity.upwardSpeed = -MathHelper.sin(pitchRad) * baseSpeed;
        } else {
            stopMovement();
        }
    }

    private void handleLandMovement() {
        boolean targetIsInWater = this.entity.getWorld().getBlockState(new BlockPos((int) this.targetX, (int) this.targetY, (int) this.targetZ)).isLiquid();

        if (targetIsInWater) {
            if (this.entity.isTouchingWater()) {
                handleWaterMovement();
                return;
            }
        }

        if (this.state == State.STRAFE) {
            handleStrafing();
        } else if (this.state == State.MOVE_TO) {
            handleMoveTo();
        } else if (this.state == State.JUMPING) {
            handleJumping();
        } else {
            stopMovement();
        }
    }

    private void handleStrafing() {
        float movementSpeed = (float) this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        float adjustedSpeed = (float) this.speed * movementSpeed * this.speedOnLand;
        float forward = this.forwardMovement;
        float sideways = this.sidewaysMovement;
        float magnitude = MathHelper.sqrt(forward * forward + sideways * sideways);

        if (magnitude < 1.0F) {
            magnitude = 1.0F;
        }

        magnitude = adjustedSpeed / magnitude;
        forward *= magnitude;
        sideways *= magnitude;

        float sinYaw = MathHelper.sin(this.entity.getYaw() * (float) (Math.PI / 180F));
        float cosYaw = MathHelper.cos(this.entity.getYaw() * (float) (Math.PI / 180F));
        float strafeX = forward * cosYaw - sideways * sinYaw;
        float strafeZ = sideways * cosYaw + forward * sinYaw;

        if (!isWalkablePosition(strafeX, strafeZ)) {
            this.forwardMovement = 1.0F;
            this.sidewaysMovement = 0.0F;
        }

        this.entity.setMovementSpeed(adjustedSpeed);
        this.entity.setForwardSpeed(this.forwardMovement);
        this.entity.setSidewaysSpeed(this.sidewaysMovement);
        this.state = State.WAIT;
    }

    private void handleMoveTo() {
        this.state = State.WAIT;
        double dx = this.targetX - this.entity.getX();
        double dy = this.targetY - this.entity.getY();
        double dz = this.targetZ - this.entity.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        if (distanceSquared < 2.5000003E-7F) {
            this.entity.setForwardSpeed(0.0F);
            return;
        }

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), targetYaw, MAX_YAW_CHANGE));

        float baseSpeed = (float) (this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        this.entity.setMovementSpeed(baseSpeed * this.speedOnLand);

        BlockPos pos = this.entity.getBlockPos();
        BlockState state = this.entity.getWorld().getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(this.entity.getWorld(), pos);

        if ((dy > this.entity.getStepHeight() && dx * dx + dz * dz < Math.max(1.0F, this.entity.getWidth())) ||
                (!shape.isEmpty() && this.entity.getY() < shape.getMax(Direction.Axis.Y) + pos.getY() &&
                        !state.isIn(BlockTags.DOORS) && !state.isIn(BlockTags.FENCES))) {
            this.entity.getJumpControl().setActive();
            this.state = State.JUMPING;
        }
    }

    private void handleJumping() {
        this.entity.setMovementSpeed((float) (this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * this.speedOnLand));
        if (this.entity.isOnGround()) {
            this.state = State.WAIT;
        }
    }

    private void stopMovement() {
        this.entity.setMovementSpeed(0.0F);
        this.entity.setSidewaysSpeed(0.0F);
        this.entity.setUpwardSpeed(0.0F);
        this.entity.setForwardSpeed(0.0F);
    }

    private boolean isWalkablePosition(float x, float z) {
        EntityNavigation entityNavigation = this.entity.getNavigation();
        if (entityNavigation != null) {
            PathNodeMaker pathNodeMaker = entityNavigation.getNodeMaker();
            return pathNodeMaker == null || pathNodeMaker.getDefaultNodeType(this.entity, BlockPos.ofFloored(this.entity.getX() + (double) x, this.entity.getBlockY(), this.entity.getZ() + (double) z)) == PathNodeType.WALKABLE;
        }

        return true;
    }
}