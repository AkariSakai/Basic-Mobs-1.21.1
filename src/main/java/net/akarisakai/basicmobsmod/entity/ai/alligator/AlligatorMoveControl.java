package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class AlligatorMoveControl extends MoveControl {
    private final AlligatorEntity alligator;
    private int swimDuration = 0;
    private double swimDirectionX = 0;
    private double swimDirectionZ = 0;
    private int restTimer = 0;
    private boolean isDecelerating = false;

    public AlligatorMoveControl(AlligatorEntity alligator) {
        super(alligator);
        this.alligator = alligator;
    }

    @Override
    public void tick() {
        LivingEntity target = alligator.getTarget();
        World world = alligator.getWorld();

        if (alligator.isTouchingWater()) {
            handleWaterMovement(target, world);
        } else {
            super.tick();
        }
    }

    private void handleWaterMovement(LivingEntity target, World world) {
        if (shouldLeaveWater()) {
            leaveWater();
            return;
        }

        if (restTimer > 0) {
            restTimer--;
            adjustVerticalPosition(alligator.getWaterSurfaceY() + 0.22, 0.015);
            return;
        }

        if (target == null || alligator.squaredDistanceTo(target) > 9.0) {
            adjustVerticalPosition(alligator.getWaterSurfaceY() + 0.22, 0.015);
        } else {
            adjustVerticalPosition(target.getY(), 0.025);
        }

        if (target == null || swimDuration > 10) {
            handleSwimRest();
        } else {
            moveTowardTarget(target);
        }
    }

    private void leaveWater() {
        double yawRad = Math.toRadians(alligator.getYaw());
        alligator.setVelocity(alligator.getVelocity().add(-Math.sin(yawRad) * 0.1, 0.1, Math.cos(yawRad) * 0.1));
    }

    private void handleSwimRest() {
        if (swimDuration <= 0) {
            if (alligator.getRandom().nextFloat() < 0.3) {
                isDecelerating = true;
            } else {
                chooseNewDirection();
                swimDuration = 80 + alligator.getRandom().nextInt(60);
                isDecelerating = false;
            }
        } else {
            swimDuration--;
        }

        if (isDecelerating) {
            decelerate();
        } else {
            swim();
        }
    }

    private void decelerate() {
        alligator.setVelocity(alligator.getVelocity().multiply(0.85));
        if (alligator.getVelocity().lengthSquared() < 0.001) {
            isDecelerating = false;
            restTimer = 60 + alligator.getRandom().nextInt(40);
        }
    }

    private void swim() {
        alligator.setVelocity(
                MathHelper.lerp(0.2, alligator.getVelocity().x, swimDirectionX * 0.15),
                alligator.getVelocity().y,
                MathHelper.lerp(0.2, alligator.getVelocity().z, swimDirectionZ * 0.15)
        );

        float desiredYaw = (float) (MathHelper.atan2(swimDirectionZ, swimDirectionX) * 180.0F / Math.PI) - 90.0F;
        alligator.setYaw(wrapDegrees(alligator.getYaw(), desiredYaw, 7.0F));
        alligator.bodyYaw = alligator.getYaw();
    }

    private void moveTowardTarget(LivingEntity target) {
        double dx = target.getX() - alligator.getX();
        double dz = target.getZ() - alligator.getZ();
        double horizontalDistance = dx * dx + dz * dz;

        double targetY = target.getY();
        double currentY = alligator.getY();
        double verticalSpeed = 0.025;

        if (currentY < targetY - 0.2) {
            alligator.setVelocity(alligator.getVelocity().add(0.0, verticalSpeed, 0.0));
        } else if (currentY > targetY + 0.2) {
            alligator.setVelocity(alligator.getVelocity().add(0.0, -verticalSpeed, 0.0));
        }

        double d = target.getX() - alligator.getX();
        double e = target.getY() - alligator.getY();
        double f = target.getZ() - alligator.getZ();
        double g = Math.sqrt(d * d + f * f);
        e /= g;

        if (g > 0.1) {
            float h = (float) (MathHelper.atan2(f, d) * 180.0F / Math.PI) - 90.0F;
            alligator.setYaw(wrapDegrees(alligator.getYaw(), h, 5.0F));
            alligator.bodyYaw = alligator.getYaw();
        }

        float movementSpeed = (float) (alligator.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        float movement = MathHelper.lerp(0.125F, alligator.getMovementSpeed(), movementSpeed);
        alligator.setMovementSpeed(movement);

        alligator.setVelocity(alligator.getVelocity().add(movement * d * 0.005, movement * e * 0.05, movement * f * 0.005));
    }

    private void adjustVerticalPosition(double targetY, double verticalSpeed) {
        double currentY = alligator.getY();

        if (currentY < targetY - 0.2) {
            alligator.setVelocity(alligator.getVelocity().add(0.0, verticalSpeed, 0.0));
        } else if (currentY > targetY + 0.2) {
            alligator.setVelocity(alligator.getVelocity().add(0.0, -verticalSpeed, 0.0));
        }
    }

    private void chooseNewDirection() {
        float randomAngle = alligator.getRandom().nextFloat() * 360;
        swimDirectionX = Math.cos(Math.toRadians(randomAngle));
        swimDirectionZ = Math.sin(Math.toRadians(randomAngle));
    }

    private boolean shouldLeaveWater() {
        LivingEntity target = alligator.getTarget();
        World world = alligator.getWorld();

        if (!world.isDay() || (target != null && target.isTouchingWater())) {
            return false;
        }

        BlockPos pos = alligator.getBlockPos();
        BlockPos below = pos.down();
        BlockPos above = pos.up();
        BlockPos front = pos.offset(alligator.getMovementDirection());
        BlockPos frontAbove = front.up();

        return isBlockSolid(world, below) && isBlockAir(world, above) && isBlockSolid(world, front) && isBlockAir(world, frontAbove);
    }

    private boolean isBlockSolid(World world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos);
    }

    private boolean isBlockAir(World world, BlockPos pos) {
        return world.getBlockState(pos).isAir();
    }
}