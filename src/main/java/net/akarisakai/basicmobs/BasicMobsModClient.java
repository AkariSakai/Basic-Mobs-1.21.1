package net.akarisakai.basicmobs;

import net.akarisakai.basicmobs.entity.ModEntities;
import net.akarisakai.basicmobs.entity.client.AlligatorRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;


public class BasicMobsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.ALLIGATOR, AlligatorRenderer::new);
    }
}
