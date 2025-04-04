package net.akarisakai.basicmobsmod.entity.ai.alligator;

import java.util.EnumSet;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class AlligatorSurfaceRestGoal extends Goal {
    private final MobEntity mob;
    private BlockPos surfacePos;
    private boolean isResting = false;
    private int restTicks = 0;
    private final int minRestDuration;
    private final int maxRestDuration;
    private int restDuration;
    private double surfaceYLevel;

    private static final double MAX_RISE_SPEED = 0.08;
    private static final double MIN_RISE_SPEED = 0.02;
    private static final double DEPTH_THRESHOLD = 8.0;
    private static final double SURFACE_SLOW_RANGE = 4.0;

    public AlligatorSurfaceRestGoal(MobEntity mob, int minRestSeconds, int maxRestSeconds) {
        this.mob = mob;
        this.minRestDuration = minRestSeconds * 20;
        this.maxRestDuration = maxRestSeconds * 20;
        this.setControls(EnumSet.of(Control.JUMP));
    }


    @Override
    public boolean canStart() {
        return isResting || this.mob.getAir() < 140;
    }

    @Override
    public boolean shouldContinue() {
        return isResting || this.mob.getAir() < 180;
    }

    @Override
    public void start() {
        if (!isResting) {
            moveToSurface();
            isResting = false;
            restTicks = 0;
            restDuration = minRestDuration + this.mob.getRandom().nextInt(maxRestDuration - minRestDuration + 1);
            if (mob instanceof AlligatorEntity) {
                ((AlligatorEntity) mob).setResting(false);
            }
        }
    }

    @Override
    public void stop() {
        isResting = false;
        restTicks = 0;
        surfacePos = null;
        if (mob instanceof AlligatorEntity) {
            ((AlligatorEntity) mob).setResting(false);
        }
    }

    @Override
    public void tick() {
        if (surfacePos == null) {
            moveToSurface();
            return;
        }

        if (this.mob.isSubmergedInWater()) {

            double riseSpeed = calculateRiseSpeedByDepth();

            this.mob.updateVelocity((float)riseSpeed, new Vec3d(0, 1.0, 0));
            this.mob.move(MovementType.SELF, this.mob.getVelocity());
        } else if (!isResting) {
            isResting = true;
            surfaceYLevel = this.mob.getY();
            if (mob instanceof AlligatorEntity) {
                ((AlligatorEntity) mob).setResting(true);
            }
        }

        if (isResting) {
            restTicks++;

            maintainSurfaceLevel();

            if (this.mob.getTarget() != null && this.mob.getTarget().isAlive() &&
                    this.mob.distanceTo(this.mob.getTarget()) < 8.0) {
                stop();
                return;
            }

            if (restTicks >= restDuration || this.mob.isOnGround()) {
                stop();
            }
        }
    }

    private double calculateRiseSpeedByDepth() {
        if (surfacePos == null) {
            return MIN_RISE_SPEED;
        }

        double distanceToSurface = surfacePos.getY() - this.mob.getY();

        if (distanceToSurface <= 0) {
            return MIN_RISE_SPEED;
        } else if (distanceToSurface <= SURFACE_SLOW_RANGE) {
            return MIN_RISE_SPEED + (MAX_RISE_SPEED - MIN_RISE_SPEED) *
                    (distanceToSurface / SURFACE_SLOW_RANGE) * 0.5;
        } else {
            double depthFactor = Math.min(distanceToSurface / DEPTH_THRESHOLD, 1.0);
            return MIN_RISE_SPEED + (MAX_RISE_SPEED - MIN_RISE_SPEED) * depthFactor;
        }
    }

    private void maintainSurfaceLevel() {
        Vec3d currentVelocity = this.mob.getVelocity();

        double adjustedSurfaceYLevel = surfaceYLevel + 0.20;

        if (Math.abs(this.mob.getY() - adjustedSurfaceYLevel) > 0.1) {
            double yAdjustment = (adjustedSurfaceYLevel - this.mob.getY()) * 0.1;
            this.mob.setVelocity(currentVelocity.x, yAdjustment, currentVelocity.z);
        } else {
            if (currentVelocity.y < -0.03) {
                this.mob.setVelocity(currentVelocity.x, 0, currentVelocity.z);
            }
        }
    }

    private void moveToSurface() {
        double scanHeight = Math.min(12.0, Math.max(8.0, this.mob.getY() - this.getWaterDepth() + 4.0));

        Iterable<BlockPos> iterable = BlockPos.iterate(
                MathHelper.floor(this.mob.getX() - 1.0),
                this.mob.getBlockY(),
                MathHelper.floor(this.mob.getZ() - 1.0),
                MathHelper.floor(this.mob.getX() + 1.0),
                MathHelper.floor(this.mob.getY() + scanHeight),
                MathHelper.floor(this.mob.getZ() + 1.0)
        );

        surfacePos = null;
        for (BlockPos pos : iterable) {
            if (isAirAboveWater(this.mob.getWorld(), pos)) {
                surfacePos = pos;
                break;
            }
        }

        if (surfacePos == null) {
            surfacePos = BlockPos.ofFloored(this.mob.getX(), this.mob.getY() + scanHeight, this.mob.getZ());
        }

        float navSpeed = 1.0F + (float)Math.min(0.5, getWaterDepth() / DEPTH_THRESHOLD * 0.5);

        this.mob.getNavigation().startMovingTo(
                surfacePos.getX(),
                surfacePos.getY() + 0.5,
                surfacePos.getZ(),
                navSpeed
        );
    }

    private double getWaterDepth() {
        BlockPos entityPos = this.mob.getBlockPos();
        int depth = 0;

        for (int y = 0; y < 16; y++) {
            BlockPos checkPos = entityPos.down(y);
            if (!this.mob.getWorld().getFluidState(checkPos).isEmpty()) {
                depth = y;
            } else {
                break;
            }
        }

        return depth;
    }

    private boolean isAirAboveWater(WorldView world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState currentState = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(belowPos);

        return currentState.canPathfindThrough(NavigationType.LAND) &&
                !world.getFluidState(belowPos).isEmpty() &&
                world.getFluidState(pos).isEmpty();
    }
}