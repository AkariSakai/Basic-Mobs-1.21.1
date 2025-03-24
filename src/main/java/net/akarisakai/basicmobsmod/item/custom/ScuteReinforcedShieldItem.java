package net.akarisakai.basicmobsmod.item.custom;

import net.akarisakai.basicmobsmod.item.ModItems;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ItemStack;

public class ScuteReinforcedShieldItem extends ShieldItem {
    public ScuteReinforcedShieldItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(ModItems.ALLIGATOR_SCUTE) || super.canRepair(stack, ingredient);
    }
}