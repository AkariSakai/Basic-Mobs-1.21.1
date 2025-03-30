package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BaskInSunGoal extends Goal {
    private final AlligatorEntity alligator;
    private int baskingTime;
    private boolean isBasking;
    private int baskingCooldown = 0;
    @Nullable
    private BlockPos baskingSpot;
    private int chanceCooldown = 0;
    private static final int COOLDOWN_TICKS = 600;
    private static final float CHANCE = 0.1F;

    public BaskInSunGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (alligator.getBaskingCooldown() > 0) return false;

        if (alligator.isBaby() || alligator.getWorld().isNight() || alligator.getWorld().isRaining()) return false;
        if (!alligator.isTouchingWater() || alligator.hasPassengers() || alligator.isAttacking() || alligator.getTarget() != null) return false;
        if (chanceCooldown > 0) {
            chanceCooldown--;
            return false;
        }

        if (alligator.getRandom().nextFloat() > CHANCE) {
            chanceCooldown = 100;
            return false;
        }
        this.baskingSpot = findBaskingSpot();
        System.out.println("BaskInSunGoal: Goal started, moving to basking spot at " + baskingSpot);
        return baskingSpot != null;
    }

    @Nullable
    private BlockPos findBaskingSpot() {
        BlockPos startPos = alligator.getBlockPos();
        World world = alligator.getWorld();
        int searchRadius = 16;

        Optional<BlockPos> closestSpot = BlockPos.findClosest(startPos, searchRadius, searchRadius, pos -> {
            BlockPos groundPos = findGroundPosition(world, pos);
            return groundPos != null && isValidBaskingSpot(world, groundPos);
        });

        if (closestSpot.isEmpty()) return null;

        BlockPos bestSpot = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (BlockPos pos : BlockPos.iterateOutwards(closestSpot.get(), 4, 1, 4)) { // Expanded search area
            BlockPos groundPos = findGroundPosition(world, pos);
            if (groundPos != null && isValidBaskingSpot(world, groundPos)) {
                double score = calculateBaskingScore(world, groundPos, startPos);
                if (score > bestScore) {
                    bestScore = score;
                    bestSpot = groundPos;
                }
            }
        }
        return bestSpot;
    }

    private double calculateBaskingScore(World world, BlockPos pos, BlockPos startPos) {
        double score = 0.0;

        if (world.isSkyVisible(pos)) score += 5.0;
        if (!isNearEdge(world, pos)) score += 4.0;

        double waterDistance = getNearestWaterDistance(world, pos);
        if (waterDistance < 2.0) {
            score -= 6.0;
        } else if (waterDistance >= 4.0) {
            score += Math.min(waterDistance * 2.0, 8.0);
        }

        double distanceFromStart = Math.sqrt(startPos.getSquaredDistance(pos));
        double maxDistance = 16.0; // Match the search radius
        double proximityBonus = 4.0 * (1.0 - (distanceFromStart / maxDistance));
        score += proximityBonus;

        return score;
    }

    private double getNearestWaterDistance(World world, BlockPos pos) {
        BlockPos closestWater = BlockPos.findClosest(pos, 6, 3,
                checkPos -> world.getFluidState(checkPos).isIn(FluidTags.WATER)
        ).orElse(null);

        return closestWater == null ? 6.0 : Math.sqrt(pos.getSquaredDistance(closestWater));
    }


    private boolean isNearEdge(World world, BlockPos pos) {
        int edgeThreshold = 3;
        int waterCount = 0;
        for (int x = -edgeThreshold; x <= edgeThreshold; x++) {
            for (int z = -edgeThreshold; z <= edgeThreshold; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (world.getFluidState(checkPos).isIn(FluidTags.WATER)) {
                    waterCount++;
                    if (waterCount > 1) return true;
                }
            }
        }
        return false;
    }

    private BlockPos findGroundPosition(World world, BlockPos pos) {
        for (int y = 5; y >= -5; y--) {
            BlockPos checkPos = pos.add(0, y, 0);
            if (world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                if (getNearestWaterDistance(world, checkPos) >= 4.0) {
                    return checkPos;
                }
            }
        }
        return null;
    }

    private boolean isValidBaskingSpot(World world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) return false;
        if (!isWaterNearby(world, pos) && !alligator.isTouchingWater()) return false;
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
        if (baskingSpot == null) return;
        alligator.getNavigation().startMovingTo(
                baskingSpot.getX() + 0.5, baskingSpot.getY(), baskingSpot.getZ() + 0.5, 1.2
        );
        baskingTime = 0;
        isBasking = false;
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getWorld().isRaining() || alligator.getWorld().isNight()) return false;
        if (baskingTime > 2000) return false;
        if (alligator.hurtTime > 0) return false;

        if (baskingSpot != null) {
            double distance = alligator.squaredDistanceTo(baskingSpot.getX() + 0.5, baskingSpot.getY(), baskingSpot.getZ() + 0.5);
            if (isBasking && distance > 3.0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void tick() {
        if (baskingCooldown > 0) {
            baskingCooldown--;
        }
        baskingTime++;

        if (alligator.hurtTime > 0) {
            stop();
            return;
        }

        if (alligator.isTouchingWater()) {
            if (alligator.getNavigation().isIdle() || alligator.getVelocity().lengthSquared() < 0.02) {
                BlockPos frontPos = alligator.getBlockPos().add(alligator.getMovementDirection().getVector());
                if (alligator.getWorld().getBlockState(frontPos).isSolidBlock(alligator.getWorld(), frontPos)) {
                    alligator.setVelocity(alligator.getVelocity().x, 0.5, alligator.getVelocity().z);
                }
            }
        } else {
            if (!isBasking && baskingSpot != null && alligator.getNavigation().isIdle()) {
                alligator.getNavigation().startMovingTo(
                        baskingSpot.getX() + 0.5,
                        baskingSpot.getY(),
                        baskingSpot.getZ() + 0.5,
                        1.2
                );
            }
        }

        if (baskingSpot != null) {
            double distance = alligator.squaredDistanceTo(
                    baskingSpot.getX() + 0.5,
                    baskingSpot.getY(),
                    baskingSpot.getZ() + 0.5
            );

            if (isBasking && distance > 3.0) {
                stop();
                return;
            }

            if (distance < 3.0) {
                if (!alligator.isTouchingWater() && alligator.getWorld().getBlockState(alligator.getBlockPos().down()).isSolidBlock(alligator.getWorld(), alligator.getBlockPos().down())) {
                    if (!isBasking) {
                        alligator.setBasking(true);
                        isBasking = true;
                        baskingTime = 0;
                        alligator.getNavigation().stop();
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        alligator.setBasking(false);
        isBasking = false;
        baskingSpot = null;
        alligator.setBaskingCooldown(COOLDOWN_TICKS);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}