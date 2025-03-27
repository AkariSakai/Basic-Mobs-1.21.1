package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AlligatorMoveControl extends MoveControl {
    private static final float MAX_WATER_SPEED = 1.2F;
    private static final float MIN_WATER_SPEED = 0.08F;
    private static final float BABY_SPEED_MODIFIER = 0.33F;
    private static final float LAND_SPEED_MODIFIER = 0.5F;
    private static final float VEHICLE_SPEED_MODIFIER = 0.5F;
    private static final float HOME_AREA_RADIUS = 16.0F;
    private static final float BUOYANCY_FACTOR = 0.005F;
    private static final float MOVEMENT_THRESHOLD = 1.0E-5F;

    private final AlligatorEntity alligator;
    private float targetYaw;

    public AlligatorMoveControl(AlligatorEntity alligator) {
        super(alligator);
        this.alligator = alligator;
        this.targetYaw = alligator.getYaw();
    }

    private void updateMovementSpeed() {
        float baseSpeed = (float) this.speed;

        if (this.alligator.isTouchingWater()) {
            // Apply buoyancy effect
            this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, BUOYANCY_FACTOR, 0.0));

            // Slow down when far from home
            if (!this.alligator.getHomePos().isWithinDistance(this.alligator.getPos(), HOME_AREA_RADIUS)) {
                baseSpeed *= LAND_SPEED_MODIFIER;
            }

            // Babies are slower
            if (this.alligator.isBaby()) {
                baseSpeed *= BABY_SPEED_MODIFIER;
            }

            // Clamp water speed
            baseSpeed = MathHelper.clamp(baseSpeed, MIN_WATER_SPEED, MAX_WATER_SPEED);
        } else if (this.alligator.isOnGround()) {
            // Slower on land
            baseSpeed *= LAND_SPEED_MODIFIER;
        }

        // Slow down when carrying passengers
        if (this.alligator.hasVehicle()) {
            baseSpeed *= VEHICLE_SPEED_MODIFIER;
        }

        this.alligator.setMovementSpeed(baseSpeed);
    }

    @Override
    public void tick() {
        this.updateMovementSpeed();

        if (this.state == State.MOVE_TO && !this.alligator.getNavigation().isIdle()) {
            Vec3d targetVec = new Vec3d(this.targetX, this.targetY, this.targetZ);
            Vec3d currentPos = this.alligator.getPos();
            Vec3d direction = targetVec.subtract(currentPos);
            double distance = direction.length();

            if (distance < MOVEMENT_THRESHOLD) {
                this.alligator.setMovementSpeed(0.0F);
                return;
            }

            // Normalize direction vector
            direction = direction.multiply(1.0 / distance);

            // Calculate target yaw (horizontal rotation)
            this.targetYaw = (float)(MathHelper.atan2(direction.z, direction.x) * (180F / Math.PI)) - 90F;

            // Smooth rotation
            this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), this.targetYaw, 5.0F));
            this.alligator.bodyYaw = this.alligator.getYaw();
            this.alligator.headYaw = this.alligator.getYaw();

            // Calculate vertical movement factor
            float verticalFactor = (float)direction.y * 0.1F;

            // Apply movement
            float currentSpeed = (float) MathHelper.lerp(0.125F, this.alligator.getMovementSpeed(), this.speed);
            this.alligator.setMovementSpeed(currentSpeed);

            // Adjust velocity
            Vec3d currentVelocity = this.alligator.getVelocity();
            this.alligator.setVelocity(
                    currentVelocity.add(
                            direction.x * currentSpeed * 0.1,
                            verticalFactor * currentSpeed,
                            direction.z * currentSpeed * 0.1
                    )
            );
        } else {
            this.alligator.setMovementSpeed(0.0F);
        }
    }

    protected float wrapDegrees(float current, float target, float step) {
        float angle = MathHelper.wrapDegrees(target - current);
        if (angle > step) {
            angle = step;
        } else if (angle < -step) {
            angle = -step;
        }
        return current + angle;
    }
}