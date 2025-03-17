package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class AlligatorModel extends GeoModel {
    @Override
    public Identifier getModelResource(GeoAnimatable animatable) {
        return Identifier.of(BasicMobsMod.MOD_ID, "geo/alligator.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoAnimatable animatable) {
        return Identifier.of(BasicMobsMod.MOD_ID, "textures/entity/alligator.png");
    }

    @Override
    public Identifier getAnimationResource(GeoAnimatable animatable) {
        return Identifier.of(BasicMobsMod.MOD_ID, "animations/alligator.animation.json");
    }
    @Override
    public void setCustomAnimations(GeoAnimatable animatable, long instanceId, AnimationState animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            if (animatable instanceof AlligatorEntity alligator) {
                boolean isInWater = alligator.isTouchingWater();

                float targetRotX = entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE;
                float targetRotY = entityData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE;

                if (isInWater) {
                    head.setRotX(MathHelper.lerp(0.1F, head.getRotX(), targetRotX * 0.25F));
                    head.setRotY(MathHelper.lerp(0.1F, head.getRotY(), targetRotY * 0.25F));
                } else {
                    head.setRotX(MathHelper.lerp(0.3F, head.getRotX(), targetRotX));
                    head.setRotY(MathHelper.lerp(0.3F, head.getRotY(), targetRotY));
                }

                // Blend custom animations with target tracking
                LivingEntity target = alligator.getTarget();
                if (target != null && target.isAlive()) {
                    double deltaX = target.getX() - alligator.getX();
                    double deltaY = target.getEyeY() - alligator.getEyeY();
                    double deltaZ = target.getZ() - alligator.getZ();
                    double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                    float lookYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180F / Math.PI)) - 90F;
                    float lookPitch = (float) -(MathHelper.atan2(deltaY, distance) * (180F / Math.PI));

                    head.setRotY(MathHelper.lerp(0.1F, head.getRotY(), lookYaw * MathHelper.RADIANS_PER_DEGREE));
                    head.setRotX(MathHelper.lerp(0.1F, head.getRotX(), lookPitch * MathHelper.RADIANS_PER_DEGREE));
                }
            }
        }
    }
}