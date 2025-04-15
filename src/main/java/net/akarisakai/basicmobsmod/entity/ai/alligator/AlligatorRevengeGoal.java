package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class AlligatorRevengeGoal extends RevengeGoal {
    private static final TargetPredicate VALID_AVOIDABLES_PREDICATE = TargetPredicate.createAttackable()
            .ignoreVisibility()
            .ignoreDistanceScalingFactor();

    private final AlligatorEntity alligator;
    private int lastAttackedTime;
    private static final double PARENT_PROTECTION_RANGE = 32.0;
    private static final int MEMORY_TICKS = 200;

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
            return !alligator.isBaby() && tryProtectBabies();
        }

        LivingEntity attacker = alligator.getAttacker();
        if (attacker != null) {
            lastAttackedTime = alligator.age;
        }
        return canAlligatorAttackTarget(attacker);
    }

    private boolean tryProtectBabies() {
        if (alligator.isBaby()) {
            return false;
        }

        Box searchBox = alligator.getBoundingBox().expand(PARENT_PROTECTION_RANGE);
        List<AlligatorEntity> nearbyAlligators = alligator.getWorld().getEntitiesByClass(
                AlligatorEntity.class, searchBox, AlligatorEntity::isBaby);

        for (AlligatorEntity baby : nearbyAlligators) {
            LivingEntity babyAttacker = baby.getAttacker();

            if (babyAttacker != null && baby.getLastAttackedTime() > baby.age - 100 &&
                    canAlligatorAttackTarget(babyAttacker)) {

                this.mob.setTarget(babyAttacker);
                this.lastAttackedTime = this.mob.age;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (alligator.getAir() < 100) {
            return false;
        }

        // Continue targeting for a while after being attacked
        if (alligator.getTarget() != null && alligator.age - lastAttackedTime < MEMORY_TICKS) {
            return canAlligatorAttackTarget(alligator.getTarget());
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

            if (mob == this.alligator) {
                this.lastAttackedTime = this.alligator.age;
            }
        }
    }

    @Override
    public void start() {
        super.start();

        if (alligator.isBaby()) {
            alertNearbyAdults();
        }
    }

    private void alertNearbyAdults() {
        Box searchBox = alligator.getBoundingBox().expand(PARENT_PROTECTION_RANGE);
        List<AlligatorEntity> nearbyAlligators = alligator.getWorld().getEntitiesByClass(
                AlligatorEntity.class, searchBox, entity -> !entity.isBaby());

        LivingEntity attacker = alligator.getAttacker();
        if (attacker != null) {
            for (AlligatorEntity adult : nearbyAlligators) {
                if (canAlligatorAttackTarget(attacker)) {
                    adult.setTarget(attacker);
                }
            }
        }
    }
}