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

    public AlligatorSurfaceRestGoal(MobEntity mob, int minRestSeconds, int maxRestSeconds) {
        this.mob = mob;
        this.minRestDuration = minRestSeconds * 20; // Convert seconds to ticks
        this.maxRestDuration = maxRestSeconds * 20;
        // Only control JUMP to prevent blocking movement goals
        this.setControls(EnumSet.of(Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // Start when air is getting low or already resting
        return isResting || this.mob.getAir() < 140;
    }

    @Override
    public boolean shouldContinue() {
        // Continue if still resting or if air is still low
        return isResting || this.mob.getAir() < 180;
    }

    @Override
    public void start() {
        if (!isResting) {
            moveToSurface();
            isResting = false;
            restTicks = 0;
            // Random duration between min and max
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
            // If still underwater, keep moving up
            this.mob.updateVelocity(0.02F, new Vec3d(0, 1.0, 0));
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

            // Check for targets that would interrupt resting
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

    private void maintainSurfaceLevel() {
        Vec3d currentVelocity = this.mob.getVelocity();

        double adjustedSurfaceYLevel = surfaceYLevel + 0.25;

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
        Iterable<BlockPos> iterable = BlockPos.iterate(
                MathHelper.floor(this.mob.getX() - 1.0),
                this.mob.getBlockY(),
                MathHelper.floor(this.mob.getZ() - 1.0),
                MathHelper.floor(this.mob.getX() + 1.0),
                MathHelper.floor(this.mob.getY() + 8.0),
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
            surfacePos = BlockPos.ofFloored(this.mob.getX(), this.mob.getY() + 8.0, this.mob.getZ());
        }

        this.mob.getNavigation().startMovingTo(
                surfacePos.getX(),
                surfacePos.getY() + 0.5,
                surfacePos.getZ(),
                1.0F
        );
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