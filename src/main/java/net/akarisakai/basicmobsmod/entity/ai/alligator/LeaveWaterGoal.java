package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class LeaveWaterGoal extends MoveToTargetPosGoal {
    private final AlligatorEntity alligator;

    public LeaveWaterGoal(AlligatorEntity alligator, double speed) {
        super(alligator, speed, 8, 2);
        this.alligator = alligator;
    }

    @Override
    public boolean canStart() {
        BlockPos frontBlockPos = this.alligator.getBlockPos().offset(this.alligator.getMovementDirection());

        return this.alligator.isTouchingWater() // Must be in water
                && this.alligator.getY() < this.alligator.getWorld().getSeaLevel() - 2 // Below sea level -2
                && this.alligator.getWorld().getBlockState(frontBlockPos).isSolidBlock(this.alligator.getWorld(), frontBlockPos); // Solid block in front
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.up();
        return world.isAir(blockPos) && world.isAir(blockPos.up()) && world.getBlockState(pos).hasSolidTopSurface(world, pos, this.alligator);
    }

    @Override
    public void start() {
        this.alligator.setTargetingUnderwater(false);
        this.alligator.setNavigation(this.alligator.landNavigation);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}