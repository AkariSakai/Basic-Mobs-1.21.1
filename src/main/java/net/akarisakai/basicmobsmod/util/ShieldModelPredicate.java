package net.akarisakai.basicmobsmod.util;

import net.akarisakai.basicmobsmod.item.ModItems;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public class ShieldModelPredicate {
    public static void registerShieldModels() {
        registerShield(ModItems.SCUTE_REINFORCED_SHIELD);
    }
    private static void registerShield(Item shield) {
        ModelPredicateProviderRegistry.register(
                shield, Identifier.of("blocking"),
                (stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F
        );
    }
}