package net.akarisakai.basicmobsmod.block;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class AlligatorEggBlock extends Block {
    private static final VoxelShape SMALL_SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 12.0, 7.0, 12.0);
    private static final VoxelShape LARGE_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);
    public static final IntProperty HATCH = Properties.HATCH;
    public static final IntProperty EGGS = Properties.EGGS;

    public AlligatorEggBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HATCH, 0)
                .with(EGGS, 1));
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects()) {
            this.tryBreakEgg(world, state, pos, entity, 100);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof ZombieEntity)) {
            this.tryBreakEgg(world, state, pos, entity, 3);
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    private void tryBreakEgg(World world, BlockState state, BlockPos pos, Entity entity, int inverseChance) {
        if (this.breaksEgg(world, entity)) {
            if (!world.isClient && world.random.nextInt(inverseChance) == 0 && state.isOf(this)) {
                this.breakEgg(world, pos, state);
            }
        }
    }

    private void breakEgg(World world, BlockPos pos, BlockState state) {
        world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
        int eggCount = state.get(EGGS);
        if (eggCount <= 1) {
            world.breakBlock(pos, false);
        } else {
            world.setBlockState(pos, state.with(EGGS, eggCount - 1), 2);
            world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.shouldHatchProgress(world, pos) && this.isMudBelow(world, pos)) {
            int hatchStage = state.get(HATCH);
            if (hatchStage < 2) {
                world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                world.setBlockState(pos, state.with(HATCH, hatchStage + 1), 2);
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
            } else {
                this.hatchEggs(world, pos, state);
            }
        }
    }

    private void hatchEggs(ServerWorld world, BlockPos pos, BlockState state) {
        world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
        world.removeBlock(pos, false);
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));

        int eggCount = state.get(EGGS);
        for (int i = 0; i < eggCount; ++i) {
            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
            AlligatorEntity alligator = ModEntities.ALLIGATOR.create(world);
            if (alligator != null) {
                alligator.setBaby(true);
                alligator.setBreedingAge(-24000); // Baby alligator
                alligator.setHomePos(pos);
                alligator.refreshPositionAndAngles(
                        pos.getX() + 0.3 + i * 0.2,
                        pos.getY(),
                        pos.getZ() + 0.3,
                        0.0F, 0.0F
                );
                world.spawnEntity(alligator);
            }
        }
    }

    private boolean isMudBelow(BlockView world, BlockPos pos) {
        return world.getBlockState(pos.down()).isIn(BlockTags.DIRT);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (this.isMudBelow(world, pos) && !world.isClient) {
            world.syncWorldEvent(2012, pos, 15);
        }
    }

    private boolean shouldHatchProgress(World world, BlockPos pos) {
        // Alligator eggs hatch more often at night and during rain
        float timeOfDay = world.getSkyAngle(1.0F);
        boolean isNight = timeOfDay < 0.25 || timeOfDay > 0.75;
        boolean isRaining = world.isRaining() && world.isSkyVisible(pos);

        if (isNight && isRaining) {
            return world.random.nextInt(10) == 0; // Faster hatching during rainy nights
        } else if (isNight) {
            return world.random.nextInt(20) == 0; // Normal night hatching
        } else {
            return world.random.nextInt(100) == 0; // Slow daytime hatching
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        this.breakEgg(world, pos, state);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        return !context.shouldCancelInteraction() &&
                context.getStack().isOf(this.asItem()) &&
                state.get(EGGS) < 4 || super.canReplace(state, context);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
        return blockState.isOf(this) ?
                blockState.with(EGGS, Math.min(4, blockState.get(EGGS) + 1)) :
                super.getPlacementState(ctx);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(EGGS) > 1 ? LARGE_SHAPE : SMALL_SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HATCH, EGGS);
    }

    private boolean breaksEgg(World world, Entity entity) {
        if (entity instanceof AlligatorEntity || entity instanceof BatEntity) {
            return false;
        }
        return entity instanceof LivingEntity &&
                (entity instanceof PlayerEntity ||
                        world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING));
    }
}