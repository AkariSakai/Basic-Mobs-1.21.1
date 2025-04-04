package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class BaskInSunGoal extends Goal {
    private static final Logger LOGGER = Logger.getLogger("BasicMobsMod");
    private static final boolean DEBUG = false;

    private final AlligatorEntity alligator;
    private int baskingTime;
    private boolean isBasking;
    private int baskingCooldown = 0;
    @Nullable
    private BlockPos baskingSpot;
    private int chanceCooldown = 0;
    private static final int COOLDOWN_TICKS = 600;
    private static final float CHANCE = 0.5F;
    private static final int MAX_BASKING_TIME = 2000;
    private static final int SEARCH_RADIUS = 16;
    private static final int WATER_NEARBY_RADIUS = 6;
    private static final double MIN_WATER_DISTANCE = 2.0;
    private static final double PREFERRED_WATER_DISTANCE = 4.0;
    private static final double BASKING_SPOT_REACH_DISTANCE = 3.0;
    private static final int REGENERATION_DURATION = 100;
    private static final int REGENERATION_AMPLIFIER = 0;

    public BaskInSunGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (alligator.getBaskingCooldown() > 0) return false;

        if (alligator.isBaby() || alligator.getWorld().isNight() || alligator.getWorld().isRaining()) return false;
        if (!alligator.isTouchingWater() || alligator.hasPassengers() || alligator.isAttacking() || alligator.getTarget() != null || !alligator.isResting()) return false;
        if (chanceCooldown > 0) {
            chanceCooldown--;
            return false;
        }

        if (alligator.getRandom().nextFloat() > CHANCE) {
            chanceCooldown = 100;
            return false;
        }
        this.baskingSpot = findBaskingSpot();

        if (DEBUG && baskingSpot != null) {
            LOGGER.info("BaskInSunGoal: Goal started, moving to basking spot at " + baskingSpot);
        }

        return baskingSpot != null;
    }

    @Nullable
    private BlockPos findBaskingSpot() {
        BlockPos startPos = alligator.getBlockPos();
        World world = alligator.getWorld();

        Optional<BlockPos> closestSpot = BlockPos.findClosest(startPos, SEARCH_RADIUS, SEARCH_RADIUS, pos -> {
            BlockPos groundPos = findGroundPosition(world, pos);
            return groundPos != null && isValidBaskingSpot(world, groundPos);
        });

        if (closestSpot.isEmpty()) return null;

        BlockPos bestSpot = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (BlockPos pos : BlockPos.iterateOutwards(closestSpot.get(), 4, 1, 4)) {
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

        if (!isNearWaterEdge(world, pos)) score += 4.0;

        double waterDistance = getNearestWaterDistance(world, pos);
        if (waterDistance < MIN_WATER_DISTANCE) {

            score -= 6.0;
        } else if (waterDistance >= PREFERRED_WATER_DISTANCE) {

            score += Math.min(waterDistance * 2.0, 8.0);
        }

        double distanceFromStart = Math.sqrt(startPos.getSquaredDistance(pos));
        double proximityBonus = 4.0 * (1.0 - (distanceFromStart / SEARCH_RADIUS));
        score += proximityBonus;

        return score;
    }

    private double getNearestWaterDistance(World world, BlockPos pos) {
        Optional<BlockPos> closestWater = BlockPos.findClosest(pos, WATER_NEARBY_RADIUS, 3,
                checkPos -> world.getFluidState(checkPos).isIn(FluidTags.WATER));

        if (closestWater.isEmpty()) {
            return WATER_NEARBY_RADIUS;
        }

        return Math.sqrt(pos.getSquaredDistance(closestWater.get()));
    }

    private boolean isNearWaterEdge(World world, BlockPos pos) {
        int edgeThreshold = 3;
        int waterCount = 0;
        int totalChecks = (2 * edgeThreshold + 1) * (2 * edgeThreshold + 1);
        int landCount = 0;

        for (int x = -edgeThreshold; x <= edgeThreshold; x++) {
            for (int z = -edgeThreshold; z <= edgeThreshold; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (world.getFluidState(checkPos).isIn(FluidTags.WATER)) {
                    waterCount++;
                } else if (world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                    landCount++;
                }
            }
        }

        return waterCount > 0 && landCount > 0 && waterCount < totalChecks * 0.4;
    }

    private BlockPos findGroundPosition(World world, BlockPos pos) {
        for (int y = 5; y >= -5; y--) {
            BlockPos checkPos = pos.add(0, y, 0);
            if (world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {

                double waterDistance = getNearestWaterDistance(world, checkPos);
                if (waterDistance >= MIN_WATER_DISTANCE && waterDistance <= WATER_NEARBY_RADIUS) {
                    return checkPos;
                }
            }
        }
        return null;
    }

    private boolean isValidBaskingSpot(World world, BlockPos pos) {

        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }

        boolean waterNearby = isWaterNearby(world, pos, WATER_NEARBY_RADIUS);
        if (!waterNearby && !alligator.isTouchingWater()) {
            return false;
        }

        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        return world.isSkyVisible(pos) && world.getLightLevel(LightType.SKY, pos) > 10;
    }

    private boolean isWaterNearby(World world, BlockPos pos, int radius) {

        for (BlockPos checkPos : BlockPos.iterateOutwards(pos, radius, 2, radius)) {
            if (world.getFluidState(checkPos).isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (baskingSpot == null) {

            stop();
            return;
        }

        boolean pathResult = alligator.getNavigation().startMovingTo(
                baskingSpot.getX() + 0.5, baskingSpot.getY(), baskingSpot.getZ() + 0.5, 1.2
        );

        if (!pathResult) {
            if (DEBUG) {
                LOGGER.warning("BaskInSunGoal: Failed to create path to basking spot");
            }
            stop();
            return;
        }

        baskingTime = 0;
        isBasking = false;
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getWorld().isRaining() || alligator.getWorld().isNight()) return false;
        if (baskingTime > MAX_BASKING_TIME) return false;
        if (alligator.hurtTime > 0) return false;
        if (alligator.getTarget() != null) return false;

        if (baskingSpot == null) return false;

        double distance = alligator.squaredDistanceTo(
                baskingSpot.getX() + 0.5, baskingSpot.getY(), baskingSpot.getZ() + 0.5
        );

        if (isBasking && distance > BASKING_SPOT_REACH_DISTANCE) {
            return false;
        }

        return true;
    }

    @Override
    public void tick() {
        if (baskingCooldown > 0) {
            baskingCooldown--;
        }

        if (baskingSpot == null) {
            stop();
            return;
        }

        baskingTime++;

        if (alligator.hurtTime > 0 || alligator.getTarget() != null) {
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

            if (!isBasking && alligator.getNavigation().isIdle()) {
                boolean pathResult = alligator.getNavigation().startMovingTo(
                        baskingSpot.getX() + 0.5,
                        baskingSpot.getY(),
                        baskingSpot.getZ() + 0.5,
                        1.2
                );

                if (!pathResult) {
                    if (DEBUG) {
                        LOGGER.warning("BaskInSunGoal: Failed to recreate path to basking spot");
                    }
                    stop();
                    return;
                }
            }
        }

        double distance = alligator.squaredDistanceTo(
                baskingSpot.getX() + 0.5,
                baskingSpot.getY(),
                baskingSpot.getZ() + 0.5
        );

        if (isBasking && distance > BASKING_SPOT_REACH_DISTANCE) {
            stop();
            return;
        }

        if (distance < BASKING_SPOT_REACH_DISTANCE) {
            if (!alligator.isTouchingWater() &&
                    alligator.getWorld().getBlockState(alligator.getBlockPos().down()).isSolidBlock(alligator.getWorld(), alligator.getBlockPos().down())) {
                if (!isBasking) {
                    if (DEBUG) {
                        LOGGER.info("BaskInSunGoal: Alligator reached basking spot, starting to bask");
                    }
                    alligator.setBasking(true);
                    isBasking = true;
                    baskingTime = 0;
                    alligator.getNavigation().stop();
                }
            }
        }

        if (isBasking && alligator.getWorld().getTime() % 20 == 0) {
            alligator.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    REGENERATION_DURATION,
                    REGENERATION_AMPLIFIER,
                    true,
                    true
            ));
        }

        if (isBasking && baskingTime > MAX_BASKING_TIME) {
            if (DEBUG) {
                LOGGER.info("BaskInSunGoal: Maximum basking time reached");
            }
            stop();
        }
    }

    @Override
    public void stop() {
        if (isBasking) {
            alligator.setBasking(false);
            isBasking = false;
            alligator.removeStatusEffect(StatusEffects.REGENERATION);
            if (DEBUG) {
                LOGGER.info("BaskInSunGoal: Stopped basking");
            }
        }
        baskingSpot = null;
        alligator.setBaskingCooldown(COOLDOWN_TICKS);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}