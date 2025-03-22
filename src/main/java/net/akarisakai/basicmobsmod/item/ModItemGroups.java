package net.akarisakai.basicmobsmod.item;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BASIC_MOBS_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(BasicMobsMod.MOD_ID, "basic_mobs_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(Items.PUFFERFISH))
                    .displayName(Text.translatable("itemgroup.basicmobsmod.basic_mobs_item"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModSpawnEggs.ALLIGATOR_SPAWN_EGG);
                        entries.add(ModItems.ALLIGATOR_MEAT);
                        entries.add(ModItems.COOKED_ALLIGATOR_MEAT);
                        entries.add(ModItems.ALLIGATOR_SCUTE);

                    }).build());


    public static void registerItemGroups() {
        BasicMobsMod.LOGGER.info("[BasicMobs] Registering Item Groups.");
    }
}