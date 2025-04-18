package net.akarisakai.basicmobsmod.datagen;

import net.akarisakai.basicmobsmod.item.ModItems;
import net.akarisakai.basicmobsmod.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.SCUTE_REINFORCED_SHIELD);

        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .add(ModItems.SCUTE_REINFORCED_SHIELD);

        getOrCreateTagBuilder(ModTags.Items.TORTOISE_TEMPT_ITEMS)
                .add(Items.MELON_SLICE)
                .add(Items.BEETROOT)
                .add(Items.CARROT);
    }
}