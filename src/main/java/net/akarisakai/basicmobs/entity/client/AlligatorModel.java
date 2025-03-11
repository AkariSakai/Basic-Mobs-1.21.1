package net.akarisakai.basicmobs.entity.client;

import net.akarisakai.basicmobs.BasicMobs;
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
        return Identifier.of(BasicMobs.MOD_ID, "geo/alligator.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoAnimatable animatable) {
        return Identifier.of(BasicMobs.MOD_ID, "textures/entity/alligator.png");
    }

    @Override
    public Identifier getAnimationResource(GeoAnimatable animatable) {
        return Identifier.of(BasicMobs.MOD_ID, "animations/alligator.animation.json");
    }
    @Override
    public void setCustomAnimations(GeoAnimatable animatable, long instanceId, AnimationState animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            ((GeoBone) head).setRotX(entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
            ((GeoBone) head).setRotY(((EntityModelData) entityData).netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
        }
    }

}
