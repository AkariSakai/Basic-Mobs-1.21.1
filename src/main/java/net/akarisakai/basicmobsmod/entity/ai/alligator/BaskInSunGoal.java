package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BaskInSunGoal extends Goal {
    private final AlligatorEntity alligator;
    private int cooldown;
    private int baskingTime;
    private boolean isBasking;
    @Nullable
    private BlockPos baskingSpot;

    public BaskInSunGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (alligator.isBaby() || alligator.getWorld().isNight() || alligator.getWorld().isRaining()) return false;
        if (!alligator.isTouchingWater() || alligator.hasPassengers() || alligator.isAttacking() || alligator.getTarget() != null) return false;

        this.baskingSpot = findBaskingSpot();
        return baskingSpot != null;
    }

    @Nullable
    private BlockPos findBaskingSpot() {
        BlockPos currentPos = alligator.getBlockPos();
        World world = alligator.getWorld();

        for (int radius = 3; radius <= 15; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;

                    BlockPos testPos = currentPos.add(x, 0, z);
                    BlockPos groundPos = findGroundPosition(world, testPos);
                    if (groundPos != null && isValidBaskingSpot(world, groundPos) && isFarEnoughFromWater(world, groundPos)) {
                        return groundPos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isFarEnoughFromWater(World world, BlockPos pos) {
        int minDistance = 3;
        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getFluidState(checkPos).isIn(FluidTags.WATER) && pos.getSquaredDistance(checkPos) < minDistance * minDistance) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private BlockPos findGroundPosition(World world, BlockPos pos) {
        for (int y = 2; y >= -2; y--) {
            BlockPos checkPos = pos.add(0, y, 0);
            if (world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                return checkPos;
            }
        }
        return null;
    }

    private boolean isValidBaskingSpot(World world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) return false;
        if (!isWaterNearby(world, pos)) return false;
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return false;
        return world.isSkyVisible(pos) && world.getLightLevel(LightType.SKY, pos) > 10;
    }

    private boolean isWaterNearby(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.getFluidState(pos.add(x, y, z)).isIn(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        alligator.setLandBound(true);
        alligator.getNavigation().startMovingTo(baskingSpot.getX(), baskingSpot.getY(), baskingSpot.getZ(), 1.0);
        baskingTime = 0;
        isBasking = false;
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getWorld().isRaining() || alligator.getWorld().isNight() || baskingTime > 1200) return false;
        if (baskingSpot != null && alligator.squaredDistanceTo(baskingSpot.getX(), baskingSpot.getY(), baskingSpot.getZ()) < 2.0) return true;
        return baskingTime < 200 && baskingSpot != null && !alligator.getNavigation().isIdle();
    }

    @Override
    public void tick() {
        baskingTime++;

        if (baskingSpot != null && alligator.squaredDistanceTo(baskingSpot.getX(), baskingSpot.getY(), baskingSpot.getZ()) < 2.0) {
            alligator.getNavigation().stop();
            if (!alligator.isTouchingWater() && alligator.getWorld().getBlockState(alligator.getBlockPos().down()).isSolidBlock(alligator.getWorld(), alligator.getBlockPos().down())) {
                if (!isBasking) {
                    alligator.setBasking(true);
                    isBasking = true;
                }
            }
        } else if (alligator.isTouchingWater() && alligator.getNavigation().isIdle()) {
            BlockPos frontPos = alligator.getBlockPos().add(alligator.getMovementDirection().getVector());
            if (alligator.getWorld().getBlockState(frontPos).isSolidBlock(alligator.getWorld(), frontPos)) {
                alligator.setVelocity(alligator.getVelocity().x, 0.5, alligator.getVelocity().z);
            }
        }
    }

    @Override
    public void stop() {
        alligator.setBasking(false);
        alligator.setLandBound(false);
        isBasking = false;
        baskingSpot = null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
