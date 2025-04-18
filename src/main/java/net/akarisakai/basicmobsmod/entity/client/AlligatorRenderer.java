package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class AlligatorRenderer extends GeoEntityRenderer<AlligatorEntity> {
    public AlligatorRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new AlligatorModel());
        this.shadowRadius = 0.8F;
        this.addRenderLayer(new AlligatorHeldItemLayer(this));
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public Identifier getTexture(AlligatorEntity animatable) {
        return Identifier.of(BasicMobsMod.MOD_ID, "textures/entity/alligator.png");
    }

    @Override
    public void render(AlligatorEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.3f, 0.3f, 0.3f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}