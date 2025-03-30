package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

public class AlligatorLookControl extends LookControl {
    private final AlligatorEntity alligator;
    private final int yawAdjustThreshold = 10; // Threshold for yaw adjustments
    private static final int ADDED_PITCH = 10;
    private static final int ADDED_YAW = 20;

    public AlligatorLookControl(MobEntity entity) {
        super(entity);
        this.alligator = (AlligatorEntity) entity;
    }

    @Override
    public void tick() {
        // If the alligator is basking, modify look behavior
        if (alligator.isBasking()) {
            this.entity.setPitch(0); // Keep head level when basking
            this.entity.setHeadYaw(this.entity.getYaw()); // Don't turn head independently when basking
        } else {
            // Normal look behavior (yaw adjustments and pitch)
            super.tick();

            // Apply yaw adjustments (similar to YawAdjustingLookControl)
            float f = MathHelper.wrapDegrees(this.entity.headYaw - this.entity.bodyYaw);
            if (f < (float)(-this.yawAdjustThreshold)) {
                this.entity.bodyYaw -= 4.0F; // Adjust body yaw if the head is too far left
            } else if (f > (float)this.yawAdjustThreshold) {
                this.entity.bodyYaw += 4.0F; // Adjust body yaw if the head is too far right
            }

            // Adjust head yaw and pitch if needed
            this.getTargetYaw().ifPresent((yaw) -> this.entity.headYaw = this.changeAngle(this.entity.headYaw, yaw + ADDED_YAW, this.maxYawChange));
            this.getTargetPitch().ifPresent((pitch) -> this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch + ADDED_PITCH, this.maxPitchChange)));
        }
    }
}