package net.akarisakai.basicmobsmod;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.client.AlligatorRenderer;
import net.akarisakai.basicmobsmod.entity.client.TortoiseRenderer;
import net.akarisakai.basicmobsmod.util.ShieldModelPredicate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;


public class BasicMobsModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ShieldModelPredicate.registerShieldModels();
        EntityRendererRegistry.register(ModEntities.ALLIGATOR, AlligatorRenderer::new);
        EntityRendererRegistry.register(ModEntities.TORTOISE, TortoiseRenderer::new);
    }
}
