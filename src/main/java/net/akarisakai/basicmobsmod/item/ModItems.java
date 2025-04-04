package net.akarisakai.basicmobsmod.item;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.item.custom.BabyAlligatorBucketItem;
import net.akarisakai.basicmobsmod.item.custom.ScuteReinforcedShieldItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {

    public static final Item RAW_ALLIGATOR_MEAT = registerItem("raw_alligator_meat", new Item(new Settings().food(ModFoodComponents.RAW_ALLIGATOR_MEAT)));
    public static final Item COOKED_ALLIGATOR_MEAT = registerItem("cooked_alligator_meat", new Item(new Settings().food(ModFoodComponents.COOKED_ALLIGATOR_MEAT)));
    public static final Item SCUTE_REINFORCED_SHIELD =registerItem("scute_reinforced_shield", new ScuteReinforcedShieldItem(new Settings().maxDamage(600).rarity(Rarity.RARE)));
    public static final Item ALLIGATOR_SCUTE = registerItem("alligator_scute", new Item(new Settings()));
    public static final Item BUCKET_OF_BABY_ALLIGATOR = registerItem("bucket_of_baby_alligator",
            new BabyAlligatorBucketItem(
                    ModEntities.ALLIGATOR,
                    Fluids.WATER,
                    SoundEvents.ITEM_BUCKET_EMPTY_FISH,
                    new Settings().maxCount(1)
            ));
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BasicMobsMod.MOD_ID, name), item);
    }

    public static void  registerModItems() {
        BasicMobsMod.LOGGER.info("[BasicMobs] Registering Mod Items.");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(ModItems.ALLIGATOR_SCUTE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(ModItems.RAW_ALLIGATOR_MEAT);
            entries.add(ModItems.COOKED_ALLIGATOR_MEAT);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(ModItems.SCUTE_REINFORCED_SHIELD);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(ModItems.BUCKET_OF_BABY_ALLIGATOR);
        });
    }
}
