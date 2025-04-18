package net.akarisakai.basicmobsmod.entity.ai.tortoise;

import net.akarisakai.basicmobsmod.entity.core.EggLayingAnimal;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class LayEggGoal<T extends AnimalEntity & EggLayingAnimal> extends MoveToTargetPosGoal {
    private final T animal;

    public LayEggGoal(T animal, double speedModifier) {
        super(animal, speedModifier, 16);
        this.animal = animal;
    }

    @Override
    public boolean canStart() {
        if (this.animal.hasEgg()) {
            return super.canStart();
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && this.animal.hasEgg();
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos blockPos = this.animal.getBlockPos();

        if (this.animal instanceof TameableEntity tameableEntity) {
            if (tameableEntity.isSitting()) {
                this.animal.setLayingEgg(false);
                return;
            }
        }

        if (!this.animal.isTouchingWater() && this.hasReached()) {
            if (this.animal.getLayEggCounter() < 1) {
                this.animal.setLayingEgg(true);
            } else if (this.animal.getLayEggCounter() > this.getTickCount(60)) {
                World world = this.animal.getWorld();
                world.playSound(null, blockPos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3f, 0.9f + world.random.nextFloat() * 0.2f);

                world.setBlockState(this.targetPos.up(), this.animal.getEggBlock().getDefaultState()
                        .with(TurtleEggBlock.EGGS, this.animal.getRandom().nextInt(4) + 1), 3);

                this.animal.setHasEgg(false);
                this.animal.setLayingEgg(false);
                this.animal.setLoveTicks(600);

                if (this.animal instanceof net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity alligator) {
                    alligator.hasBredThisCycle = true;
                }
            }

            if (this.animal.isLayingEgg()) {
                this.animal.setLayEggCounter(this.animal.getLayEggCounter() + 1);
            }
        }
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        return world.isAir(pos.up()) && world.getBlockState(pos).isIn(this.animal.getEggLayableBlockTag());
    }
}