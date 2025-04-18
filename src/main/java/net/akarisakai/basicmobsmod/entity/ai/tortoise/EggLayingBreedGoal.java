package net.akarisakai.basicmobsmod.entity.ai.tortoise;

import net.akarisakai.basicmobsmod.entity.core.EggLayingAnimal;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.world.GameRules;

public class EggLayingBreedGoal<T extends AnimalEntity & EggLayingAnimal> extends AnimalMateGoal {
    private final T animal;

    public EggLayingBreedGoal(T animal, double speedModifier) {
        super(animal, speedModifier);
        this.animal = animal;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && !this.animal.hasEgg();
    }

    @Override
    protected void breed() {
        ServerPlayerEntity serverPlayer = this.animal.getLovingPlayer();
        if (serverPlayer == null && this.mate.getLovingPlayer() != null) {
            serverPlayer = this.mate.getLovingPlayer();
        }

        if (serverPlayer != null) {
            serverPlayer.incrementStat(Stats.ANIMALS_BRED);
        }

        this.animal.setHasEgg(true);
        this.animal.resetLoveTicks();
        this.mate.resetLoveTicks();

        if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            this.world.spawnEntity(new ExperienceOrbEntity(this.world,
                    this.animal.getX(), this.animal.getY(), this.animal.getZ(),
                    this.animal.getRandom().nextInt(7) + 1));
        }
    }
}