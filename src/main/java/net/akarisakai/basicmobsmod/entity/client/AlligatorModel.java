package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
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
            head.setRotX(entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
            head.setRotY(entityData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);

            // Scale head for baby alligators
            if (animatable instanceof AlligatorEntity alligator) {
                float headScale = alligator.isBaby() ? 1.5F : 1.0F;
                head.setScaleX(headScale);
                head.setScaleY(headScale);
                head.setScaleZ(headScale);
            }
        }
    }
}
