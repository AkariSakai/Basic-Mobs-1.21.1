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
    private int stuckTicks = 0;
    private static final int COOLDOWN_TICKS = 600; // 30 seconds
    private static final int MAX_STUCK_TICKS = 100;
    private static final int MAX_SEARCH_ITERATIONS = 100;
    private static final Random RANDOM = new Random();

    public BaskInSunGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (baskingCooldown > 0) return false; // On cooldown
        if (alligator.isBaby() || alligator.getWorld().isNight() || alligator.getWorld().isRaining()) return false;
        if (!alligator.isTouchingWater() || alligator.hasPassengers() || alligator.isAttacking() || alligator.getTarget() != null) return false;
        if (RANDOM.nextFloat() > 0.7f) return false;

        this.baskingSpot = findBaskingSpot();
        return baskingSpot != null;
    }

    @Nullable
    private BlockPos findBaskingSpot() {
        BlockPos startPos = alligator.getBlockPos();
        World world = alligator.getWorld();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int maxRadius = 12;
        BlockPos bestSpot = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int iterations = 0;

        while (!queue.isEmpty() && iterations++ < MAX_SEARCH_ITERATIONS) {
            BlockPos currentPos = queue.poll();
            double distanceFromStart = Math.sqrt(startPos.getSquaredDistance(currentPos));

            // Prioritize closer spots with a weighting factor
            double proximityWeight = 1.0 - (distanceFromStart / maxRadius);

            BlockPos groundPos = findGroundPosition(world, currentPos);
            if (groundPos != null && isValidBaskingSpot(world, groundPos)) {
                double score = calculateBaskingScore(world, groundPos) * (0.5 + 0.5 * proximityWeight);
                if (score > bestScore) {
                    bestScore = score;
                    bestSpot = groundPos;
                }
            }
            List<BlockPos> neighbors = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos nextPos = currentPos.add(dx, 0, dz);
                    if (!visited.contains(nextPos)) {
                        double dist = startPos.getSquaredDistance(nextPos);
                        if (dist <= maxRadius * maxRadius) {
                            neighbors.add(nextPos);
                        }
                    }
                }
            }

            neighbors.sort(Comparator.comparingDouble(startPos::getSquaredDistance));
            queue.addAll(neighbors);
            visited.addAll(neighbors);
        }

        return bestSpot;
    }

    private double calculateBaskingScore(World world, BlockPos pos) {
        double score = 0.0;

        if (world.isSkyVisible(pos)) score += 5.0;
        if (!isNearEdge(world, pos)) score += 4.0;

        double waterDistance = getNearestWaterDistance(world, pos);
        if (waterDistance < 2.0) {
            score -= 6.0;
        } else if (waterDistance >= 4.0) {
            score += Math.min(waterDistance * 2.0, 8.0);
        }

        return score;
    }

    private double getNearestWaterDistance(World world, BlockPos pos) {
        int searchRadius = 6;
        double minDistance = searchRadius;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getFluidState(checkPos).isIn(FluidTags.WATER)) {
                        double dist = Math.sqrt(pos.getSquaredDistance(checkPos));
                        if (dist < minDistance) {
                            minDistance = dist;
                        }
                    }
                }
            }
        }
        return minDistance;
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
        alligator.setLandBound(true);
        alligator.getNavigation().startMovingTo(
                baskingSpot.getX() + 0.5, baskingSpot.getY(), baskingSpot.getZ() + 0.5, 1.2
        );
        baskingTime = 0;
        isBasking = false;
        stuckTicks = 0;
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getWorld().isRaining() || alligator.getWorld().isNight()) return false;
        if (baskingTime > 2000) return false;

        if (baskingSpot != null) {
            double distance = alligator.squaredDistanceTo(baskingSpot.getX(), baskingSpot.getY(), baskingSpot.getZ());
            if (distance < 2.25) {
                return true;
            }

            if (alligator.getNavigation().isIdle()) {
                stuckTicks++;
                return stuckTicks <= MAX_STUCK_TICKS;
            } else {
                stuckTicks = 0;
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

        if (alligator.isTouchingWater()) {

            if (alligator.getNavigation().isIdle() || alligator.getVelocity().lengthSquared() < 0.02) {
                BlockPos frontPos = alligator.getBlockPos().add(alligator.getMovementDirection().getVector());

                if (alligator.getWorld().getBlockState(frontPos).isSolidBlock(alligator.getWorld(), frontPos)) {
                    alligator.setVelocity(alligator.getVelocity().x, 0.5, alligator.getVelocity().z);
                }
            }
        }

        if (baskingSpot != null) {
            double distance = alligator.squaredDistanceTo(
                    baskingSpot.getX() + 0.5,
                    baskingSpot.getY(),
                    baskingSpot.getZ() + 0.5
            );
            if (distance < 4.0) {
                alligator.getNavigation().stop();
                if (!alligator.isTouchingWater() && alligator.getWorld().getBlockState(alligator.getBlockPos().down()).isSolidBlock(alligator.getWorld(), alligator.getBlockPos().down())) {
                    if (!isBasking) {
                        alligator.setBasking(true);
                        isBasking = true;
                        baskingTime = 0;
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        alligator.setBasking(false);
        alligator.setLandBound(false);
        isBasking = false;
        baskingSpot = null;
        stuckTicks = 0;
        baskingCooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}