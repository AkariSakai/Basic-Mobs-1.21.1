package net.akarisakai.basicmobs;

import net.akarisakai.basicmobs.entity.ModEntities;
import net.akarisakai.basicmobs.entity.custom.AlligatorEntity;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicMobsMod implements ModInitializer {
	public static final String MOD_ID = "tutorialmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.registerModEntities();
		FabricDefaultAttributeRegistry.register(ModEntities.ALLIGATOR, AlligatorEntity.createAttributes());
	}
}