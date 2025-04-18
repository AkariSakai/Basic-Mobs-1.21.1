package net.akarisakai.basicmobsmod.util;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");
        public static final TagKey<Item> TORTOISE_TEMPT_ITEMS = createTag("tortoise_tempt_items");

        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(BasicMobsMod.MOD_ID, name));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> ALLIGATORS_SPAWNABLE_ON = createTag("alligators_spawnable_on");
        public static final TagKey<Block> TORTOISE_EGG_LAYABLE_ON = createTag("tortoise_layable_on");

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(BasicMobsMod.MOD_ID, name));
        }
    }
}