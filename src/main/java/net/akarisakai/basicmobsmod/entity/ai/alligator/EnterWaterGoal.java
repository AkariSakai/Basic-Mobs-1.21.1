package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Random;

public class EnterWaterGoal extends Goal {
    private final AlligatorEntity alligator;
    private final World world;
    private int swimmingTime;
    private final Random random = new Random();
    private BlockPos waterPos;
    private int failTimer = 0;
    private BlockPos lastPosition;

    public EnterWaterGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.world = alligator.getWorld();
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {

        if (alligator.getTarget() != null) {
            return false;
        }

        if (!world.isDay()) {
            return false;
        }

        if (alligator.isTouchingWater()) {
            return false;
        }

        if (!alligator.isWaterCooldownOver()) {
            return false;
        }

        return random.nextInt(100) < 20;
    }

    @Override
    public void start() {
        waterPos = findNearbyWater();

        if (waterPos != null) {
            alligator.setNavigation(alligator.landNavigation);
            boolean canMove = alligator.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2);
            failTimer = 0;
        } else {
            this.startShortCooldown();
        }

        swimmingTime = 200; // Temps de nage
    }

    public void startShortCooldown() {
        this.alligator.waterCooldown = 400; // 20 secondes (400 ticks)
    }

    @Override
    public void stop() {
        this.alligator.waterCooldown = 400;

        if (alligator.getTarget() != null) {
            LivingEntity target = alligator.getTarget();

            alligator.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.5);
        }
    }

    @Override
    public void tick() {
        if (alligator.isTouchingWater()) {
            alligator.setNavigation(alligator.waterNavigation); // Passe en navigation aquatique
            swimmingTime--;

            if (swimmingTime <= 0) {
                alligator.targetingUnderwater = false;
            }

            failTimer = 0;
        } else if (waterPos != null) {
            if (lastPosition != null && alligator.getBlockPos().equals(lastPosition)) {
                failTimer++;
            } else {
                failTimer = 0;
            }

            if (failTimer > 100) {
                boolean canMove = alligator.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2);
                failTimer = 0;
            }

            lastPosition = alligator.getBlockPos();
        }
    }

    @Override
    public boolean shouldContinue() {
        return alligator.isAlive() && alligator.waterCooldown <= 0 && (!alligator.isTouchingWater() || swimmingTime > 0) &&
                this.alligator.getTarget() != null;
    }

    private BlockPos findNearbyWater() {
        BlockPos alligatorPos = alligator.getBlockPos();
        BlockPos bestWaterPos = null;
        int bestWaterScore = -1;

        int range = 15;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos pos = alligatorPos.add(x, y, z);

                    if (world.getBlockState(pos).isOf(Blocks.WATER)) {
                        int waterCount = 0;
                        if (world.getBlockState(pos.north()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.south()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.east()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.west()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.down()).isOf(Blocks.WATER)) waterCount++;

                        boolean isNearLand =
                                (!world.getBlockState(pos.north()).isOf(Blocks.WATER) && world.getBlockState(pos.north()).isSolid()) ||
                                        (!world.getBlockState(pos.south()).isOf(Blocks.WATER) && world.getBlockState(pos.south()).isSolid()) ||
                                        (!world.getBlockState(pos.east()).isOf(Blocks.WATER) && world.getBlockState(pos.east()).isSolid()) ||
                                        (!world.getBlockState(pos.west()).isOf(Blocks.WATER) && world.getBlockState(pos.west()).isSolid());

                        boolean isDeepEnough =
                                world.getBlockState(pos.down()).isOf(Blocks.WATER) &&
                                        world.getBlockState(pos.down(2)).isOf(Blocks.WATER);

                        if (!isNearLand && isDeepEnough && waterCount > bestWaterScore) {
                            bestWaterPos = pos;
                            bestWaterScore = waterCount;
                        }
                    }
                }
            }
        }

        return bestWaterPos;
    }
}
