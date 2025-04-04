package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.MobEntity;

public class AlligatorRevengeGoal extends RevengeGoal {
    private static final TargetPredicate VALID_AVOIDABLES_PREDICATE = TargetPredicate.createAttackable()
            .ignoreVisibility()
            .ignoreDistanceScalingFactor();

    private final AlligatorEntity alligator;

    public AlligatorRevengeGoal(AlligatorEntity alligator, Class<?>... noRevengeTypes) {
        super(alligator, noRevengeTypes);
        this.alligator = alligator;
    }

    @Override
    public boolean canStart() {
        if (alligator.getAir() < 300) {
            return false;
        }

        if (!super.canStart()) {
            return false;
        }

        LivingEntity attacker = alligator.getAttacker();
        return canAlligatorAttackTarget(attacker);
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getAir() < 100) {
            return false;
        }

        return super.shouldContinue() && canAlligatorAttackTarget(alligator.getTarget());
    }

    private boolean canAlligatorAttackTarget(LivingEntity target) {
        if (alligator.hasVehicle() || target instanceof AlligatorEntity) {
            return false;
        }
        return target != null && target.isAlive();
    }

    @Override
    protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
        if (mob instanceof AlligatorEntity alligatorMob &&
                alligatorMob.getAir() >= 300 &&
                !alligatorMob.hasVehicle() &&
                !(target instanceof AlligatorEntity)) {
            super.setMobEntityTarget(mob, target);
        }
    }
}