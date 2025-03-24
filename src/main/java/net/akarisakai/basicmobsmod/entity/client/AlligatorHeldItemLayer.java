package net.akarisakai.basicmobsmod.entity.client;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class AlligatorHeldItemLayer extends BlockAndItemGeoLayer<AlligatorEntity> {
    public AlligatorHeldItemLayer(GeoRenderer<AlligatorEntity> renderer) {
        // Provide empty BlockState function instead of null
        super(renderer,
                (bone, alligator) -> alligator.getHeldItem(),
                (bone, alligator) -> null // Safe null return for blocks
        );
    }

    @Override
    protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, AlligatorEntity animatable) {
        return ModelTransformationMode.GROUND; // Better for held items
    }

    @Override
    protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, AlligatorEntity animatable, VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
        if ("item".equals(bone.getName())) {
            poseStack.push();

            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

            poseStack.translate(0.0, -0.1, 0.0);

            poseStack.scale(0.85f, 0.85f, 0.85f);

            // Render with corrected transform type
            MinecraftClient.getInstance().getItemRenderer().renderItem(
                    animatable,
                    stack,
                    ModelTransformationMode.GROUND,
                    false,
                    poseStack,
                    bufferSource,
                    animatable.getWorld(),
                    packedLight,
                    packedOverlay,
                    animatable.getId()
            );
            poseStack.pop();
        }
    }
}