package net.akarisakai.basicmobs.entity;

import net.akarisakai.basicmobs.BasicMobsMod;
import net.akarisakai.basicmobs.entity.custom.AlligatorEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<AlligatorEntity> ALLIGATOR = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BasicMobsMod.MOD_ID, "alligator"),
            EntityType.Builder.create(AlligatorEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1f, 2.5f).build());

    public static void registerModEntities(){
        BasicMobsMod.LOGGER.info("Registering Mod Entities for "+ BasicMobsMod.MOD_ID);
    }
}
