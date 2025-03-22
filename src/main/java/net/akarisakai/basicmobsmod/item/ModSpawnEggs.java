package net.akarisakai.basicmobsmod.item;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModSpawnEggs {
    public static final Item ALLIGATOR_SPAWN_EGG = registerSpawnEgg("spawn_egg/alligator_spawn_egg", new SpawnEggItem(ModEntities.ALLIGATOR,3162151, 9606514,  new Item.Settings()));
    private static Item registerSpawnEgg(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BasicMobsMod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BasicMobsMod.LOGGER.info("[BasicMobsMod] Registering Mod Spawn Eggs.");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(ModSpawnEggs.ALLIGATOR_SPAWN_EGG);
        });
    }
}