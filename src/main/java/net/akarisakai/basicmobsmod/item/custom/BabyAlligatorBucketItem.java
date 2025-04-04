package net.akarisakai.basicmobsmod.item.custom;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;

import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class BabyAlligatorBucketItem extends EntityBucketItem {
    private final EntityType<?> type;

    public BabyAlligatorBucketItem(EntityType<?> type, Fluid fluid, SoundEvent emptyingSound, Settings settings) {
        super(type, fluid, emptyingSound, settings);
        this.type = type;
    }

    @Override
    public void onEmptied(@Nullable PlayerEntity player, World world, ItemStack stack, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            Entity entity = this.type.create(serverWorld);
            if (entity instanceof AlligatorEntity alligator) {
                alligator.setBaby(true);
                alligator.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);

                NbtComponent nbtComponent = stack.get(DataComponentTypes.BUCKET_ENTITY_DATA);
                if (nbtComponent != null) {
                    alligator.copyDataFromNbt(nbtComponent.getNbt());
                }
                alligator.setFromBucket(true);
                serverWorld.spawnEntity(entity);
                world.emitGameEvent(player, GameEvent.ENTITY_PLACE, pos);
            }
        }
    }
}