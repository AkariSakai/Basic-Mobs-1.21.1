package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WideMobNavigation extends MobNavigation {

    public WideMobNavigation(MobEntity mobEntity, World world) {
        super(mobEntity, world);
    }

    @Override
    protected PathNodeNavigator createPathNodeNavigator(int range) {
        LandPathNodeMaker nodeMaker = new LandPathNodeMaker();
        nodeMaker.setCanEnterOpenDoors(true);
        nodeMaker.setCanSwim(true);
        nodeMaker.setCanWalkOverFences(false);
        return new PathNodeNavigator(nodeMaker, range);
    }

    @Override
    public boolean isValidPosition(BlockPos pos) {
        return super.isValidPosition(pos) && !this.world.getBlockState(pos).isSolidBlock(world, pos);
    }

    @Override
    protected boolean isAtValidPosition() {
        return this.entity.getWidth() <= 1.65F && super.isAtValidPosition();
    }

    @Override
    protected Vec3d getPos() {
        return new Vec3d(this.entity.getX(), this.entity.getBodyY(0.5), this.entity.getZ());
    }
}
