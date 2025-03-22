package net.akarisakai.basicmobsmod;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.akarisakai.basicmobsmod.item.ModItemGroups;
import net.akarisakai.basicmobsmod.item.ModItems;
import net.akarisakai.basicmobsmod.item.ModSpawnEggs;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicMobsMod implements ModInitializer {
	public static final String MOD_ID = "basicmobsmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.registerModEntities();
		FabricDefaultAttributeRegistry.register(ModEntities.ALLIGATOR, AlligatorEntity.createAttributes());
		ModSpawnEggs.registerModItems();
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
	}
}