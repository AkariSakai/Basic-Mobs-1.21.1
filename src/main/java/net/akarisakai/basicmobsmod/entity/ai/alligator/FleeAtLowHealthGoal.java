package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ai.NoPenaltyTargeting;

public class FleeAtLowHealthGoal extends EscapeDangerGoal {
    private final float healthThreshold;

    public FleeAtLowHealthGoal(PathAwareEntity mob, double speed, float healthThreshold) {
        super(mob, speed);
        this.healthThreshold = healthThreshold;
    }

    @Override
    protected boolean isInDanger() {
        return this.mob.getHealth() <= this.healthThreshold && super.isInDanger();
    }

    @Override
    public boolean canStart() {
        return this.isInDanger();
    }

    @Override
    protected boolean findTarget() {
        Vec3d vec3d = NoPenaltyTargeting.find(this.mob, 10, 6); // Increased range
        if (vec3d == null) {
            return false;
        }
        this.targetX = vec3d.x;
        this.targetY = vec3d.y;
        this.targetZ = vec3d.z;
        return true;
    }
}