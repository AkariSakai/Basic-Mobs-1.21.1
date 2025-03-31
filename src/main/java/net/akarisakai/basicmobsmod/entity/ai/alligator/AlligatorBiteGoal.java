package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;

public class AlligatorBiteGoal extends Goal {
    private final AlligatorEntity alligator;
    private final double range;
    private LivingEntity target;
    private final TargetPredicate targetPredicate;
    private int delayCounter;
    private static final int DELAY_TICKS = 8;

    public AlligatorBiteGoal(AlligatorEntity alligator, double range) {
        this.alligator = alligator;
        this.range = range;
        this.targetPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(range);
    }

    @Override
    public boolean canStart() {
        if (this.alligator.isBaby() || this.alligator.canHunt(null)) {
            return false;
        }
        this.target = this.alligator.getWorld().getClosestEntity(LivingEntity.class, targetPredicate, this.alligator, this.alligator.getX(), this.alligator.getEyeY(), this.alligator.getZ(), this.alligator.getBoundingBox().expand(this.range));
        if (this.target instanceof AlligatorEntity) {
            return false;
        }
        return this.target != null && this.target.canTakeDamage();
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.alligator.lookAtEntity(this.target, 30.0F, 30.0F);
            this.alligator.swingHand(this.alligator.getActiveHand());
            this.alligator.triggerAnim("biteController", "bite"); // Trigger the bite animation
            this.delayCounter = DELAY_TICKS; // Initialize the delay counter
        }
    }

    @Override
    public void tick() {
        if (this.target != null && this.delayCounter > 0) {
            this.delayCounter--;
            if (this.delayCounter == 0) {
                this.alligator.tryAttack(this.target);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null && this.target.isAlive() && this.alligator.squaredDistanceTo(this.target) <= this.range * this.range;
    }
}