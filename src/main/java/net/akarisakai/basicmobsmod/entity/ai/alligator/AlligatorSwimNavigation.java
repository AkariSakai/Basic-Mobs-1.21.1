package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class AlligatorSwimNavigation extends AmphibiousSwimNavigation {
    public AlligatorSwimNavigation(MobEntity owner, World world) {
        super(owner, world);
    }

    @Override
    public boolean isValidPosition(BlockPos pos) {
        BlockState state = this.world.getBlockState(pos);
        return state.isOf(Blocks.WATER) || state.isAir();
    }

    @Override
    protected boolean canPathDirectlyThrough(Vec3d origin, Vec3d target) {
        return this.entity.isTouchingWater() || super.canPathDirectlyThrough(origin, target);
    }

    @Override
    protected Vec3d getPos() {
        return new Vec3d(this.entity.getX(), this.entity.getY() + 0.5, this.entity.getZ());
    }
}