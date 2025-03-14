package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class AlligatorMoveControl extends MoveControl {
    private final AlligatorEntity alligator;
    private int swimDuration = 0;
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
            if (target == null) {
                // Pas de cible → L'alligator flotte près de la surface
                double targetY = this.alligator.getWaterSurfaceY() + 0.22;
                double currentY = this.alligator.getY();
                double verticalSpeed = 0.015;

                if (currentY < targetY - 0.2) {
                    this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, verticalSpeed, 0.0));
                } else if (currentY > targetY + 0.2) {
                    this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, -verticalSpeed, 0.0));
                }

                // Choix aléatoire d'une direction toutes les quelques secondes
                if (swimDuration <= 0) {
                    chooseNewDirection();
                    swimDuration = 80 + this.alligator.getRandom().nextInt(60);
                    System.out.println("[Alligator] Swimming randomly...");
                } else {
                    swimDuration--;
                }

                this.alligator.setVelocity(
                        MathHelper.lerp(0.2, this.alligator.getVelocity().x, swimDirectionX * 0.15),
                        this.alligator.getVelocity().y,
                        MathHelper.lerp(0.2, this.alligator.getVelocity().z, swimDirectionZ * 0.15)
                );

                float desiredYaw = (float) (MathHelper.atan2(swimDirectionZ, swimDirectionX) * 180.0F / Math.PI) - 90.0F;
                this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), desiredYaw, 7.0F));
                this.alligator.bodyYaw = this.alligator.getYaw();

            } else {
                // L'alligator a une cible → Il plonge vers elle
                double dx = target.getX() - this.alligator.getX();
                double dz = target.getZ() - this.alligator.getZ();
                double horizontalDistance = dx * dx + dz * dz;

                double targetY = target.getY();
                double currentY = this.alligator.getY();
                double verticalSpeed = 0.025;

                if (currentY < targetY - 0.2) {
                    this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, verticalSpeed, 0.0));
                } else if (currentY > targetY + 0.2) {
                    this.alligator.setVelocity(this.alligator.getVelocity().add(0.0, -verticalSpeed, 0.0));
                }

                double d = this.targetX - this.alligator.getX();
                double e = this.targetY - this.alligator.getY();
                double f = this.targetZ - this.alligator.getZ();
                double g = Math.sqrt(d * d + f * f);
                e /= g;

                if (g > 0.1) {
                    float h = (float) (MathHelper.atan2(f, d) * 180.0F / Math.PI) - 90.0F;
                    this.alligator.setYaw(this.wrapDegrees(this.alligator.getYaw(), h, 5.0F));
                    this.alligator.bodyYaw = this.alligator.getYaw();
                }

                float i = (float) (this.speed * this.alligator.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                float j = MathHelper.lerp(0.125F, this.alligator.getMovementSpeed(), i);
                this.alligator.setMovementSpeed(j);

                this.alligator.setVelocity(this.alligator.getVelocity().add(j * d * 0.005, j * e * 0.05, j * f * 0.005));

                System.out.println("[Alligator] Hunting target: " + target.getName().getString());
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
