package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.util.math.MathHelper;

public class AlligatorMoveControl extends MoveControl {
    private final AlligatorEntity alligator;

    public AlligatorMoveControl(AlligatorEntity alligator) {
        super(alligator);
        this.alligator = alligator;
    }

    private void updateVelocity() {
        if (this.alligator.isTouchingWater()) {
            this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, 0.005, 0.0));
            if (!this.alligator.getHomePos().isWithinDistance(this.alligator.getPos(), 16.0)) {
                this.alligator.setMovementSpeed(Math.max(this.alligator.getMovementSpeed() / 2.0F, 0.08F));
            }

            if (this.alligator.isBaby()) {
                this.alligator.setMovementSpeed(Math.max(this.alligator.getMovementSpeed() / 3.0F, 0.06F));
            }
        } else if (this.alligator.isOnGround()) {
            this.alligator.setMovementSpeed(Math.max(this.alligator.getMovementSpeed() / 2.0F, 0.06F));
        }

        // Reduce movement speed when mounted
        if (this.alligator.hasVehicle()) {
            this.alligator.setMovementSpeed(this.alligator.getMovementSpeed() * 0.5F);
        }
    }

    @Override
    public void tick() {
        this.updateVelocity();
        if (this.state == MoveControl.State.MOVE_TO && !this.alligator.getNavigation().isIdle()) {
            double d = this.targetX - this.alligator.getX();
            double e = this.targetY - this.alligator.getY();
            double f = this.targetZ - this.alligator.getZ();
            double g = Math.sqrt(d * d + e * e + f * f);
            if (g < 1.0E-5F) {
                this.entity.setMovementSpeed(0.0F);
            } else {
                e /= g;
                float h = (float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F;
                this.alligator.bodyYaw = this.alligator.getYaw();
                this.alligator.setMovementSpeed((float) MathHelper.lerp(0.125F, this.alligator.getMovementSpeed(), this.speed));
                this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, this.alligator.getMovementSpeed() * e * 0.1, 0.0));
            }
        } else {
            this.alligator.setMovementSpeed(0.0F);
        }
    }
}