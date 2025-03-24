package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.goal.WanderAroundGoal;

public class WanderOnLandGoal extends WanderAroundGoal {
    private final AlligatorEntity alligator;

    public WanderOnLandGoal(AlligatorEntity alligator, double speed, int chance) {
        super(alligator, speed, chance);
        this.alligator = alligator;
    }

    @Override
    public boolean canStart() {
        return !this.mob.isTouchingWater() && this.alligator.isLandBound() ? super.canStart() : false;
    }
}