package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class AlligatorMoveControl extends MoveControl {
    private final AlligatorEntity alligator;
    private int swimDuration = 0; // Remaining duration of the current movement
    private double swimDirectionX = 0;
    private double swimDirectionZ = 0;

    public AlligatorMoveControl(AlligatorEntity alligator) {
        super(alligator);
        this.alligator = alligator;
    }

    @Override
    public void tick() {
        LivingEntity target = this.alligator.getTarget();
        World world = this.alligator.getWorld();

        if (this.alligator.isTouchingWater()) {
            double targetY = (target != null) ? target.getY() : this.alligator.getWaterSurfaceY() + 0.2; // Add a small offset to raise above the surface
            double currentY = this.alligator.getY();
            double verticalSpeed = 0.02;

            // Adjust height to naturally float at the surface
            if (currentY < targetY - 0.2) {
                this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, verticalSpeed, 0.0));
            } else if (currentY > targetY + 0.2) {
                this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, -verticalSpeed, 0.0));
            }

            if (target == null) {
                // If there is no target, move randomly in the water
                if (swimDuration <= 0) {
                    chooseNewDirection();
                    swimDuration = 80 + this.alligator.getRandom().nextInt(60); // 4 to 7 seconds
                } else {
                    swimDuration--;
                }

                // Apply continuous movement in the current direction
                this.alligator.setVelocity(swimDirectionX * 0.1, this.alligator.getVelocity().y, swimDirectionZ * 0.1);

                // Smooth rotation toward movement direction
                float desiredYaw = (float) (MathHelper.atan2(swimDirectionZ, swimDirectionX) * 180.0F / Math.PI) - 90.0F;
                this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), desiredYaw, 5.0F));
                this.alligator.bodyYaw = this.alligator.getYaw();

            } else {
                // Move toward the target
                double d = this.targetX - this.alligator.getX();
                double e = this.targetY - this.alligator.getY();
                double f = this.targetZ - this.alligator.getZ();
                double g = Math.sqrt(d * d + f * f);
                e /= g;

                // Smooth rotation
                if (g > 0.1) {
                    float h = (float) (MathHelper.atan2(f, d) * 180.0F / Math.PI) - 90.0F;
                    this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), h, 5.0F));
                    this.alligator.bodyYaw = this.alligator.getYaw();
                }

                float i = (float) (this.speed * this.alligator.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                float j = MathHelper.lerp(0.125F, this.alligator.getMovementSpeed(), i);
                this.alligator.setMovementSpeed(j);

                // Apply smooth movement toward the target
                this.alligator.setVelocity(this.alligator.getVelocity().add(j * d * 0.005, j * e * 0.05, j * f * 0.005));
            }
        } else {
            super.tick();
        }
    }

    private void chooseNewDirection() {
        float randomAngle = this.alligator.getRandom().nextFloat() * 360;
        swimDirectionX = Math.cos(Math.toRadians(randomAngle));
        swimDirectionZ = Math.sin(Math.toRadians(randomAngle));
    }
}
