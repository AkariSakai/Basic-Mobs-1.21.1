package net.akarisakai.basicmobsmod.block;

import net.akarisakai.basicmobsmod.BasicMobsMod;
import net.akarisakai.basicmobsmod.block.custom.TortoiseEggBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block TORTOISE_EGG = registerBlock("tortoise_egg",
            new TortoiseEggBlock(AbstractBlock.Settings.create().strength(0.5F).sounds(BlockSoundGroup.STONE)
                    .nonOpaque().ticksRandomly()));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(BasicMobsMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(BasicMobsMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        BasicMobsMod.LOGGER.info("Registering Mod Blocks for " + BasicMobsMod.MOD_ID);
    }
}