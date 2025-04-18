package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.entity.custom.TortoiseEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TortoiseRenderer extends GeoEntityRenderer<TortoiseEntity> {
    public TortoiseRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new TortoiseModel());
        this.shadowRadius = 0.8F;
    }

    @Override
    public Identifier getTexture(TortoiseEntity animatable) {
        return this.getGeoModel().getTextureResource(animatable);
    }

    @Override
    public void render(TortoiseEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}