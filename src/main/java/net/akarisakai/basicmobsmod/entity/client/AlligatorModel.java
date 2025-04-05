package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class AlligatorModel extends GeoModel<AlligatorEntity> {

    @Override
    public Identifier getModelResource(AlligatorEntity alligatorEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "geo/alligator.geo.json");
    }

    @Override
    public Identifier getTextureResource(AlligatorEntity alligatorEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "textures/entity/alligator.png");
    }

    @Override
    public Identifier getAnimationResource(AlligatorEntity alligatorEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "animations/alligator.animation.json");
    }

    @Override
    public void setCustomAnimations(AlligatorEntity alligatorEntity, long instanceId, AnimationState<AlligatorEntity> animationState) {
        resetBones();
        GeoBone tiltHelper = getAnimationProcessor().getBone("tiltHelper");
        GeoBone head = getAnimationProcessor().getBone("head");

        if (tiltHelper != null) {
            tiltHelper.setRotX(alligatorEntity.getBodyPitch());
        }

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX((entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE) - (tiltHelper != null ? tiltHelper.getRotX() : 0));
            head.setRotY(entityData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);

            if (alligatorEntity instanceof AlligatorEntity alligator) {
                float headScale = alligator.isBaby() ? 1.5F : 1.0F;
                head.setScaleX(headScale);
                head.setScaleY(headScale);
                head.setScaleZ(headScale);
            }
        }
    }

    private void resetBones() {
        resetBone("tiltHelper");
    }

    private void resetBone(String boneName) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setRotX(0);
            bone.setRotY(0);
            bone.setRotZ(0);
        }
    }
}