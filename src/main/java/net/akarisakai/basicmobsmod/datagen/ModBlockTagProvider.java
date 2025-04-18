package net.akarisakai.basicmobsmod.datagen;

import net.akarisakai.basicmobsmod.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup lookup) {
        getOrCreateTagBuilder(ModTags.Blocks.ALLIGATORS_SPAWNABLE_ON)
                .add(Blocks.MUD)
                .add(Blocks.MUDDY_MANGROVE_ROOTS)
                .add(Blocks.MANGROVE_ROOTS)
                .add(Blocks.DIRT)
                .add(Blocks.STONE)
                .add(Blocks.CLAY);

        getOrCreateTagBuilder(ModTags.Blocks.TORTOISE_EGG_LAYABLE_ON)
                .add(Blocks.MUD)
                .add(Blocks.DIRT)
                .add(Blocks.GRASS_BLOCK)
                .add(Blocks.SAND);

        getOrCreateTagBuilder(ModTags.Blocks.ALLIGATOR_EGG_LAYABLE_ON)
                .add(Blocks.MUD)
                .add(Blocks.DIRT)
                .add(Blocks.GRASS_BLOCK)
                .add(Blocks.SAND);
    }
}