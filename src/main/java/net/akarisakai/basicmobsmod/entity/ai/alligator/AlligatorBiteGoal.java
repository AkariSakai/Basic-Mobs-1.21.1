package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

public class AlligatorBiteGoal extends Goal {
    private final AlligatorEntity alligator;
    private final double range;
    private LivingEntity target;
    private final TargetPredicate targetPredicate;
    private int delayCounter;
    private static final int DELAY_TICKS = 8;
    private static final double MAX_FACING_ANGLE = 90.0;

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
        return this.target != null && this.target.canTakeDamage() && isTargetInFront();
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.alligator.lookAtEntity(this.target, 30.0F, 30.0F);
            this.alligator.swingHand(this.alligator.getActiveHand());
            this.alligator.triggerAnim("biteController", "bite");
            this.delayCounter = DELAY_TICKS;
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
        return this.target != null && this.target.isAlive() && this.alligator.squaredDistanceTo(this.target) <= this.range * this.range && isTargetInFront();
    }

    private boolean isTargetInFront() {
        if (this.target == null) {
            return false;
        }
        Vec3d direction = this.alligator.getRotationVector();
        Vec3d targetVector = new Vec3d(this.target.getX() - this.alligator.getX(), this.target.getY() - this.alligator.getY(), this.target.getZ() - this.alligator.getZ()); // Get vector from alligator to target

        double dotProduct = direction.dotProduct(targetVector.normalize());
        double angle = Math.acos(dotProduct);
        angle = Math.toDegrees(angle);
        return angle <= MAX_FACING_ANGLE;
    }
}