package net.akarisakai.basicmobsmod.item;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BASIC_MOBS_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(BasicMobsMod.MOD_ID, "basic_mobs_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.RAW_ALLIGATOR_MEAT))
                    .displayName(Text.translatable("itemgroup.basicmobsmod.basic_mobs_item"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.SCUTE_REINFORCED_SHIELD);
                        entries.add(ModItems.RAW_ALLIGATOR_MEAT);
                        entries.add(ModItems.COOKED_ALLIGATOR_MEAT);
                        entries.add(ModItems.ALLIGATOR_SCUTE);
                        entries.add(ModItems.BUCKET_OF_BABY_ALLIGATOR);
                        entries.add(ModBlocks.TORTOISE_EGG);
                        entries.add(ModBlocks.ALLIGATOR_EGG);
                        entries.add(ModSpawnEggs.ALLIGATOR_SPAWN_EGG);
                        entries.add(ModSpawnEggs.TORTOISE_SPAWN_EGG);
                    }).build());


    public static void registerItemGroups() {
        BasicMobsMod.LOGGER.info("[BasicMobs] Registering Item Groups.");
    }
}