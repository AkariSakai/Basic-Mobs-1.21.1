package net.akarisakai.basicmobsmod.mixin;

import net.akarisakai.basicmobsmod.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ItemStack shield = player.getActiveItem();
            World world = player.getWorld();

            if (player.isBlocking() && shield.isOf(ModItems.SCUTE_REINFORCED_SHIELD)) {
                if (source.getAttacker() instanceof LivingEntity attacker) {

                    Vec3d playerLook = player.getRotationVec(1.0F);
                    Vec3d attackDirection = attacker.getPos().subtract(player.getPos()).normalize();
                    double dotProduct = playerLook.dotProduct(attackDirection);

                    if (dotProduct > 0.0) {
                        if (!world.isClient) {
                            EquipmentSlot slot = player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

                            int shieldDamage = (int) Math.ceil(amount * 0.1);
                            shield.damage(shieldDamage, player, slot);

                            if (attacker.disablesShield()) {
                                player.disableShield();
                                player.getItemCooldownManager().set(ModItems.SCUTE_REINFORCED_SHIELD, 30);
                            }
                        }

                        Vec3d knockbackVector = attackDirection;
                        double knockbackStrength = 0.4;
                        attacker.setVelocity(knockbackVector.multiply(knockbackStrength).add(0, 0.3, 0));
                        attacker.velocityDirty = true;

                        world.playSound(
                                null,
                                player.getBlockPos(),
                                SoundEvents.ITEM_SHIELD_BLOCK,
                                SoundCategory.PLAYERS,
                                1.0F, 1.0F
                        );
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}
