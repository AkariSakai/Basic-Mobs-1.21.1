package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;

public class AlligatorSwimNavigation extends AmphibiousSwimNavigation {
    public AlligatorSwimNavigation(MobEntity owner, World world) {
        super(owner, world);
    }

    @Override
    public boolean isValidPosition(BlockPos pos) {
        return this.entity instanceof AlligatorEntity alligatorEntity && alligatorEntity.isActivelyTraveling()
                ? this.world.getBlockState(pos).isOf(Blocks.WATER)
                : !this.world.getBlockState(pos.down()).isAir();
    }
}