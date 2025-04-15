package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.world.BlockView;

import java.util.function.Function;

public class FleeAtLowHealthGoal extends EscapeDangerGoal {
    private final float healthThreshold;

    public FleeAtLowHealthGoal(PathAwareEntity mob, double speed, float healthThreshold) {
        this(mob, speed, healthThreshold, DamageTypeTags.PANIC_CAUSES);
    }

    public FleeAtLowHealthGoal(PathAwareEntity mob, double speed, float healthThreshold, TagKey<DamageType> dangerousDamageTypes) {
        this(mob, speed, healthThreshold, (entity) -> dangerousDamageTypes);
    }

    public FleeAtLowHealthGoal(PathAwareEntity mob, double speed, float healthThreshold, Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes) {
        super(mob, speed, entityToDangerousDamageTypes);
        this.healthThreshold = healthThreshold;
    }

    @Override
    protected boolean isInDanger() {
        return (this.mob.getHealth() <= this.healthThreshold || this.mob.isOnFire()) && super.isInDanger();
    }

    private BlockPos findPositionInWater(BlockPos edgePos) {
        Vec3d direction = new Vec3d(edgePos.getX() - mob.getX(), 0, edgePos.getZ() - mob.getZ()).normalize();
        return new BlockPos(
                (int)(edgePos.getX() + direction.x * 2),
                edgePos.getY(),
                (int)(edgePos.getZ() + direction.z * 2)
        );
    }

    @Override
    public boolean canStart() {
        if (this.mob.isOnFire()) {
            BlockPos blockPos = this.locateClosestWater(this.mob.getWorld(), this.mob, 15);
            if (blockPos != null) {
                BlockPos waterPos = findPositionInWater(blockPos);
                this.targetX = waterPos.getX() + 0.5;
                this.targetY = waterPos.getY();
                this.targetZ = waterPos.getZ() + 0.5;
                return true;
            }

        }
        if (this.isInDanger()) {
            ((AlligatorEntity) this.mob).setPanicking(true);
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        super.stop();
        ((AlligatorEntity) this.mob).setPanicking(false);
    }

    @Override
    protected boolean findTarget() {
        Vec3d vec3d = NoPenaltyTargeting.find(this.mob, 5, 4);
        if (vec3d == null) {
            return false;
        } else {
            this.targetX = vec3d.x;
            this.targetY = vec3d.y;
            this.targetZ = vec3d.z;
            return true;
        }
    }
    @Override
    protected BlockPos locateClosestWater(BlockView world, Entity entity, int rangeX) {
        return BlockPos.findClosest(entity.getBlockPos(), rangeX, 5,
                        pos -> world.getFluidState(pos).isIn(FluidTags.WATER))
                .orElse(null);
    }
}