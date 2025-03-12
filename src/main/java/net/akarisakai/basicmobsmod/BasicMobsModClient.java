package net.akarisakai.basicmobsmod;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.client.AlligatorRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;


public class BasicMobsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.ALLIGATOR, AlligatorRenderer::new);
    }
}
