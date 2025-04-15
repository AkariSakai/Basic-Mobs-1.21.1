package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TortoiseEntity extends AnimalEntity implements GeoEntity {
    private static final String NBT_IS_BABY = "IsBaby";
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(TortoiseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("tortoise.idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("tortoise.walk");
    protected static final RawAnimation HURT = RawAnimation.begin().thenLoop("tortoise.hurt");


    public TortoiseEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(5, new WanderAroundGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.16)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (this.isBreedingItem(itemStack)) {
            this.eat(player, hand, itemStack);
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(BABY, false);
    }

    public boolean isBaby() {
        return this.dataTracker.get(BABY);
    }

    public void setBaby(boolean baby) {
        this.dataTracker.set(BABY, baby);
        this.setBreedingAge(baby ? -24000 : 0);
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 4, this::movementPredicate));
        controllers.add(new AnimationController<>(this, "hurtController", 4, this::hurtPredicate));
    }

    private <T extends GeoEntity> PlayState movementPredicate(AnimationState<T> state) {
        if (this.hurtTime > 0) {
            state.getController().setAnimation(IDLE);
            return PlayState.CONTINUE;
        }
        if (this.getVelocity().horizontalLengthSquared() > 1.0E-6) {
            state.getController().setAnimation(WALK);
            return PlayState.CONTINUE;
        } else {
            state.getController().setAnimation(IDLE);
            return PlayState.CONTINUE;
        }
    }

    private <T extends GeoEntity> PlayState hurtPredicate(final AnimationState<T> event) {
        if(this.hurtTime > 0) {
            event.getController().setAnimation(HURT);
            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();

        return PlayState.STOP;
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean(NBT_IS_BABY, this.isBaby());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setBaby(nbt.getBoolean(NBT_IS_BABY));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        TortoiseEntity babyTortoise = ModEntities.TORTOISE.create(world);
        if (babyTortoise != null) {
            babyTortoise.setBaby(true);
            babyTortoise.setBreedingAge(-24000);
        }
        return babyTortoise;
    }
}