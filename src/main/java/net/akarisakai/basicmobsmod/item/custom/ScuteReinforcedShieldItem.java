package net.akarisakai.basicmobsmod.item.custom;

import net.akarisakai.basicmobsmod.item.ModItems;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ScuteReinforcedShieldItem extends ShieldItem {
    public ScuteReinforcedShieldItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(ModItems.ALLIGATOR_SCUTE) || super.canRepair(stack, ingredient);
    }


    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.basicmobsmod.scute_shield.special_properties")
                .formatted(Formatting.GOLD));
        tooltip.add(Text.literal(" ★ ").append(Text.translatable("tooltip.basicmobsmod.scute_shield.pushback")
                .formatted(Formatting.GRAY)));
        tooltip.add(Text.literal(" ★ ").append(Text.translatable("tooltip.basicmobsmod.scute_shield.reduced_cooldown")
                .formatted(Formatting.GRAY)));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.basicmobsmod.scute_shield.repair")
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

    }
}