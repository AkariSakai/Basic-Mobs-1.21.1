package net.akarisakai.basicmobsmod.world.gen;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public class ModEntitySpawns {
    public static void addSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.MANGROVE_SWAMP)
                        .or(BiomeSelectors.tag(BiomeTags.IS_RIVER)),
                SpawnGroup.CREATURE,
                ModEntities.ALLIGATOR,
                40,
                1,
                2
        );

        SpawnRestriction.register(
                ModEntities.ALLIGATOR,
                SpawnLocationTypes.IN_WATER,
                Heightmap.Type.OCEAN_FLOOR,
                AnimalEntity::isValidNaturalSpawn
        );
    }
}
