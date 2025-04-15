package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.TortoiseEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class TortoiseModel extends GeoModel<TortoiseEntity> {

    @Override
    public Identifier getModelResource(TortoiseEntity tortoiseEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "geo/tortoise.geo.json");
    }

    @Override
    public Identifier getTextureResource(TortoiseEntity tortoiseEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "textures/entity/tortoise.png");
    }

    @Override
    public Identifier getAnimationResource(TortoiseEntity tortoiseEntity) {
        return Identifier.of(BasicMobsMod.MOD_ID, "animations/tortoise.animation.json");
    }

    @Override
    public void setCustomAnimations(TortoiseEntity tortoiseEntity, long instanceId, AnimationState<TortoiseEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
            float clampedYaw = MathHelper.clamp(
                    entityData.netHeadYaw(),
                    -60.0F,
                    60.0F
            );
            head.setRotY(clampedYaw * MathHelper.RADIANS_PER_DEGREE);

        }
    }
}