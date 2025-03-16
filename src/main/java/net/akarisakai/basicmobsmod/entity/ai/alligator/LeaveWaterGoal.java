package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class LeaveWaterGoal extends Goal {
    private final AlligatorEntity alligator;
    private final World world;
    private BlockPos landPos;
    private boolean isLeavingWater = false;

    public LeaveWaterGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.world = alligator.getWorld();
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        boolean shouldStart = alligator.isTouchingWater() && !alligator.targetingUnderwater;

        if (alligator.getTarget() != null) {
            return false;
        }
        return shouldStart;
    }

    @Override
    public void start() {
        landPos = findNearbyLand();
        if (landPos != null) {
            alligator.startLeavingWater();
            alligator.getNavigation().startMovingTo(landPos.getX(), landPos.getY(), landPos.getZ(), 1.2);
        }
    }


    @Override
    public void tick() {
        if (isLeavingWater) {
            if (!alligator.isTouchingWater()) {
                alligator.startWaterCooldown();
                isLeavingWater = false;
            } else {
                jumpOutOfWater();
                moveToLand();
            }
        }
    }

    private void jumpOutOfWater() {
        BlockPos pos = alligator.getBlockPos();

        boolean isNearLand =
                !world.getBlockState(pos.north()).isOf(Blocks.WATER) && world.getBlockState(pos.north()).isSolid() ||
                        !world.getBlockState(pos.south()).isOf(Blocks.WATER) && world.getBlockState(pos.south()).isSolid() ||
                        !world.getBlockState(pos.east()).isOf(Blocks.WATER) && world.getBlockState(pos.east()).isSolid() ||
                        !world.getBlockState(pos.west()).isOf(Blocks.WATER) && world.getBlockState(pos.west()).isSolid();

        if (isNearLand) {
            alligator.setVelocity(alligator.getVelocity().x, 0.6, alligator.getVelocity().z);
        }
    }


    @Override
    public boolean shouldContinue() {
        boolean continueGoal = alligator.isTouchingWater();

        if (!continueGoal) {
            alligator.setNavigation(alligator.landNavigation);
            alligator.startWaterCooldown();
            isLeavingWater = false;
        }

        if (alligator.getTarget() != null)
            return false;

        return continueGoal;
    }

    @Override
    public void stop() {
        alligator.getNavigation().stop();

        this.landPos = null;
        this.isLeavingWater = false;

        if (alligator.getTarget() != null) {
            LivingEntity target = alligator.getTarget();

            alligator.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.5);
        }
    }


    private void moveToLand() {
        if (landPos != null) {
            alligator.getNavigation().startMovingTo(landPos.getX(), landPos.getY(), landPos.getZ(), 1.2);
        }
    }

    private BlockPos findNearbyLand() {
        BlockPos alligatorPos = alligator.getBlockPos();
        BlockPos bestLandPos = null;
        int bestLandScore = -1;

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = alligatorPos.add(x, y, z);

                    if (!world.getBlockState(pos).isOf(Blocks.WATER) && world.getBlockState(pos.down()).isSolid()) {
                        int landScore = 0;
                        if (world.getBlockState(pos.up()).isAir()) landScore++;
                        if (world.getBlockState(pos.north()).isAir()) landScore++;
                        if (world.getBlockState(pos.south()).isAir()) landScore++;
                        if (world.getBlockState(pos.east()).isAir()) landScore++;
                        if (world.getBlockState(pos.west()).isAir()) landScore++;

                        if (landScore > bestLandScore) {
                            bestLandPos = pos;
                            bestLandScore = landScore;
                        }
                    }
                }
            }
        }

        return bestLandPos;
    }
}
