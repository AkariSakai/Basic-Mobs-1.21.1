package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HybridSwimNavigation extends SwimNavigation {
    private final AlligatorEntity alligator;

    public HybridSwimNavigation(AlligatorEntity alligator, World world) {
        super(alligator, world);
        this.alligator = alligator;
    }

    @Override
    public void tick() {
        super.tick();

        if (isNearLand()) {
            steerAwayFromLand();
        }
    }

    private boolean isNearLand() {
        Vec3d direction = alligator.getRotationVec(1.0F);
        BlockPos pos1 = alligator.getBlockPos().add((int) (direction.x * 2), 0, (int) (direction.z * 2));
        BlockPos pos2 = alligator.getBlockPos().add((int) (direction.x * 3), 0, (int) (direction.z * 3));

        return isSolid(pos1) || isSolid(pos2);
    }

    private boolean isSolid(BlockPos pos) {
        return !world.getBlockState(pos).isOf(Blocks.WATER) && world.getBlockState(pos).isSolid();
    }

    private void steerAwayFromLand() {
        Vec3d velocity = alligator.getVelocity();

        double angle = Math.toRadians(30);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double newX = velocity.x * cos - velocity.z * sin;
        double newZ = velocity.x * sin + velocity.z * cos;

        Vec3d newVelocity = new Vec3d(newX, velocity.y, newZ).normalize().multiply(0.5);

        alligator.setVelocity(newVelocity);
    }
}
