package net.akarisakai.basicmobsmod.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents {
    public static final FoodComponent RAW_ALLIGATOR_MEAT = new FoodComponent.Builder()
            .nutrition(4)
            .saturationModifier(0.3F)
            .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER,600, 0), 0.3F)
            .build();
    public static final FoodComponent COOKED_ALLIGATOR_MEAT = new FoodComponent.Builder().nutrition(8).saturationModifier(0.8F).build();
}
