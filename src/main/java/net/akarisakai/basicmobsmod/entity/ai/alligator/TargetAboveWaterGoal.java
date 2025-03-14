package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

public class TargetAboveWaterGoal extends Goal {
    private final AlligatorEntity alligator;
    private final double speed;
    private final int minY;
    private boolean foundTarget;

    public TargetAboveWaterGoal(AlligatorEntity alligator, double speed, int minY) {
        this.alligator = alligator;
        this.speed = speed;
        this.minY = minY;
    }

    @Override
    public boolean canStart() {
        return this.alligator.isTouchingWater() && this.alligator.getY() < this.minY - 2;
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() && !this.foundTarget;
    }

    @Override
    public void tick() {
        if (this.alligator.getY() < this.minY - 1 && (this.alligator.getNavigation().isIdle() || this.alligator.hasFinishedCurrentPath())) {
            Vec3d vec3d = NoPenaltyTargeting.findTo(this.alligator, 4, 8, new Vec3d(this.alligator.getX(), this.minY - 1, this.alligator.getZ()), (float) (Math.PI / 2));
            if (vec3d == null) {
                this.foundTarget = true;
                return;
            }

            this.alligator.getNavigation().startMovingTo(vec3d.x, vec3d.y, vec3d.z, this.speed);
        }
    }

    @Override
    public void start() {
        this.alligator.setTargetingUnderwater(true);
        this.foundTarget = false;
    }

    @Override
    public void stop() {
        this.alligator.setTargetingUnderwater(false);
    }
}

