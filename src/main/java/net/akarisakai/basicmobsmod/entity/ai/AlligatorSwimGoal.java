package net.akarisakai.basicmobsmod.entity.ai;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AlligatorSwimGoal extends Goal {
    private final MobEntity alligator;
    private double targetX;
    private double targetY;
    private double targetZ;
    private final double speed;
    private int cooldown;
    private boolean isAttacking;

    public AlligatorSwimGoal(MobEntity alligator, double speed) {
        this.alligator = alligator;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        alligator.getNavigation().setCanSwim(true);
    }

    @Override
    public boolean canStart() {
        return this.alligator.isTouchingWater() && this.alligator.getFluidHeight(FluidTags.WATER) > 0.2;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.pickNewTarget();
    }

    @Override
    public void tick() {
        LivingEntity target = this.alligator.getTarget();
        isAttacking = target != null;

        if (target != null) {
            handleChaseTarget(target);
        } else {
            handleIdleSwimming();
        }
    }

    private void handleChaseTarget(LivingEntity target) {
        Vec3d targetPos = target.getPos();
        boolean targetInWater = target.isTouchingWater();
        if (targetInWater) {
            targetY = targetPos.y;
        } else {
            targetY = this.alligator.getY() + 0.2;
        }
        moveToTarget(targetPos);
        double distance = this.alligator.squaredDistanceTo(target);
        double attackRange = 2.0;

        if (distance < attackRange * attackRange) {
            this.alligator.tryAttack(target);
        }
    }

    private void handleIdleSwimming() {
        BlockPos currentPos = this.alligator.getBlockPos();
        BlockPos surfacePos = findNearestSurface(currentPos);

        if (surfacePos != null) {
            double alligatorHeight = this.alligator.getHeight();
            targetY = surfacePos.getY() - 1.0 + (alligatorHeight * 0.2);
        } else {
            targetY = this.alligator.getY();
        }

        // Move towards new target Y smoothly
        double yDiff = targetY - this.alligator.getY();
        if (Math.abs(yDiff) > 0.05) {
            this.alligator.setVelocity(this.alligator.getVelocity().add(0, MathHelper.clamp(yDiff * 0.1, -0.1, 0.1), 0));
        }

        // Reduce unnecessary target recalculations
        if (cooldown-- <= 0 || this.alligator.squaredDistanceTo(targetX, targetY, targetZ) < 1.0) {
            cooldown = 100 + this.alligator.getRandom().nextInt(100);
            pickNewTarget();
        }

        moveToTarget(new Vec3d(targetX, targetY, targetZ));
    }

    private void moveToTarget(Vec3d targetPos) {
        Vec3d movement = new Vec3d(targetPos.x - alligator.getX(), targetY - alligator.getY(), targetPos.z - alligator.getZ());

        if (movement.lengthSquared() > 0.5) {
            movement = movement.normalize().multiply(speed);
            this.alligator.setVelocity(movement);

            float angle = (float) (MathHelper.atan2(movement.z, movement.x) * (180.0F / Math.PI)) - 90.0F;
            this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), angle, 5.0F));
            this.alligator.bodyYaw = this.alligator.getYaw();
        }
    }

    private BlockPos findNearestSurface(BlockPos pos) {
        for (int i = 0; i < 20; i++) {
            BlockPos checkPos = pos.up(i);
            if (!this.alligator.getWorld().getBlockState(checkPos).getFluidState().isIn(FluidTags.WATER)) {
                return checkPos;
            }
        }
        return null;
    }

    private void pickNewTarget() {
        double angle = this.alligator.getRandom().nextDouble() * Math.PI * 2;
        double distance = 5 + this.alligator.getRandom().nextDouble() * 5;
        targetX = this.alligator.getX() + Math.cos(angle) * distance;
        targetZ = this.alligator.getZ() + Math.sin(angle) * distance;
        targetY = this.alligator.getY();
    }

    private float wrapDegrees(float current, float target, float maxChange) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + MathHelper.clamp(delta, -maxChange, maxChange);
    }

    public boolean isAttacking() {
        return isAttacking;
    }
}
