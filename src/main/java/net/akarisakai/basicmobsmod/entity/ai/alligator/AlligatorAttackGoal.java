package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;

public class AlligatorAttackGoal extends MeleeAttackGoal {
    private int ticks;
    private final AlligatorEntity alligator;

    public AlligatorAttackGoal(AlligatorEntity alligator,double speed, boolean pauseWhenMobIdle) {
        super(alligator, speed, pauseWhenMobIdle);
        this.alligator = alligator;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && canAlligatorAttackTarget(alligator.getTarget());
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && canAlligatorAttackTarget(alligator.getTarget());
    }

    private boolean canAlligatorAttackTarget(LivingEntity target) {
        // Custom attack conditions for alligator (e.g., must be in water, certain range, etc.)
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        super.start();
        this.ticks = 0;
    }

    @Override
    public void stop() {
        super.stop();
        alligator.setAttacking(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
        alligator.setAttacking(this.ticks >= 5 && this.getCooldown() < this.getMaxCooldown() / 2);
    }

}
