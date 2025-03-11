package net.akarisakai.basicmobs.entity.client;

import net.akarisakai.basicmobs.BasicMobs;
import net.akarisakai.basicmobs.entity.custom.AlligatorEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AlligatorRenderer extends GeoEntityRenderer<AlligatorEntity> {
    public AlligatorRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new AlligatorModel());
    }

    @Override
    public Identifier getTexture(AlligatorEntity animatable) {
        return Identifier.of(BasicMobs.MOD_ID, "textures/entity/alligator.png");
    }

    @Override
    public void render(AlligatorEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()){
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
