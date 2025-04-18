package net.akarisakai.basicmobsmod.block.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.custom.TortoiseEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class TortoiseEggBlock extends TurtleEggBlock {
    public TortoiseEggBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.shouldUpdateHatchLevel(world)) {
            int hatchLevel = state.get(HATCH);
            if (hatchLevel < 2) {
                world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.2f);
                world.setBlockState(pos, state.with(HATCH, hatchLevel + 1), 2);
            } else {
                world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.2f);
                world.removeBlock(pos, false);
                for (int j = 0; j < state.get(EGGS); ++j) {
                    world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
                    TortoiseEntity tortoise = ModEntities.TORTOISE.create(world);
                    tortoise.setBaby(true);
                    tortoise.refreshPositionAndAngles((double)pos.getX() + 0.3 + (double)j * 0.2, pos.getY(), (double)pos.getZ() + 0.3, 0.0f, 0.0f);
                    world.spawnEntity(tortoise);
                }
            }
        }
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient) {
            world.syncWorldEvent(2005, pos, 0);
        }
    }

    private boolean shouldUpdateHatchLevel(World world) {
        return world.random.nextInt(500) == 0;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects()) {
            this.destroyEgg(world, state, pos, entity, 100);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof ZombieEntity)) {
            this.destroyEgg(world, state, pos, entity, 3);
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    private void destroyEgg(World world, BlockState state, BlockPos pos, Entity entity, int chance) {
        if (!this.canDestroyEgg(world, entity)) {
            return;
        }
        if (!world.isClient && world.random.nextInt(chance) == 0 && state.isOf(this)) {
            this.decreaseEggs(world, pos, state);
        }
    }

    private boolean canDestroyEgg(World world, Entity entity) {
        if (entity instanceof TortoiseEntity) {
            return false;
        }

        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        return entity instanceof PlayerEntity || world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }

    private void decreaseEggs(World world, BlockPos pos, BlockState state) {
        world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
        int eggCount = state.get(EGGS);
        if (eggCount <= 1) {
            world.breakBlock(pos, false);
        } else {
            world.setBlockState(pos, state.with(EGGS, eggCount - 1), 2);
            world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
        }
    }
}