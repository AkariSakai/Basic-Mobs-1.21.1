package net.akarisakai.basicmobsmod.entity.core;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;

public interface EggLayingAnimal {
    boolean hasEgg();
    void setHasEgg(boolean hasEgg);
    boolean isLayingEgg();
    void setLayingEgg(boolean isLayingEgg);
    int getLayEggCounter();
    void setLayEggCounter(int layEggCounter);
    Block getEggBlock();
    TagKey<Block> getEggLayableBlockTag();
}