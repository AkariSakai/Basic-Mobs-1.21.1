package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class WanderInWaterGoal extends MoveToTargetPosGoal {
    private static final int MAX_TRYING_TIME = 1200;
    private final AlligatorEntity alligator;

    public WanderInWaterGoal(AlligatorEntity alligator, double speed) {
        super(alligator, alligator.isBaby() ? 2.0 : speed, 24);
        this.alligator = alligator;
        this.lowestY = -1;
    }

    @Override
    public boolean shouldContinue() {
        return !this.alligator.isTouchingWater() && this.tryingTime <= 1200 && this.isTargetPos(this.alligator.getWorld(), this.targetPos);
    }

    @Override
    public boolean canStart() {
        if (this.alligator.isBaby() && !this.alligator.isTouchingWater()) {
            return super.canStart();
        } else {
            return !this.alligator.isLandBound() && !this.alligator.isTouchingWater() && super.canStart();
        }
    }

    @Override
    public boolean shouldResetPath() {
        return this.tryingTime % 160 == 0;
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.WATER);
    }
}