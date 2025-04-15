package net.akarisakai.basicmobsmod.entity;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.akarisakai.basicmobsmod.entity.custom.TortoiseEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<AlligatorEntity> ALLIGATOR = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BasicMobsMod.MOD_ID, "alligator"),
            EntityType.Builder.create(AlligatorEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1.65f, 1f).build());
    public static final EntityType<TortoiseEntity> TORTOISE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(BasicMobsMod.MOD_ID, "tortoise"),
            EntityType.Builder.create(TortoiseEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1.18f, 1.26f).build());

    public static void registerModEntities(){
        BasicMobsMod.LOGGER.info("Registering Mod Entities for "+ BasicMobsMod.MOD_ID);
    }
}
