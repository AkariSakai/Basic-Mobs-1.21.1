package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.AlligatorSwimGoal;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SchoolingFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

public class AlligatorEntity extends AnimalEntity implements GeoEntity {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AlligatorSwimGoal(this, 0.09));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 2, true));
        this.goalSelector.add(3, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.00));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.goalSelector.add(5, new AttackGoal(this));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, ChickenEntity.class, true));
        this.goalSelector.add(3, new ActiveTargetGoal<>(this, SchoolingFishEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 25)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return ModEntities.ALLIGATOR.create(world);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "walkController", 0, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "swimController", 0, this::swimPredicate));
        controllers.add(new AnimationController<>(this, "chaseController", 0, this::chasePredicate));
    }

    private <T extends GeoEntity> PlayState walkPredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        if (event.isMoving() && !this.isTouchingWater()) {
            AnimationController<?> controller = event.getController();
            controller.setAnimation(
                    RawAnimation.begin()
                            .then("walk.transition", Animation.LoopType.PLAY_ONCE)
                            .then("walk", Animation.LoopType.LOOP)
            );
            controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * 100));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState swimPredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        if (event.isMoving() && this.isTouchingWater()) {
            AnimationController<?> controller = event.getController();
            controller.setAnimation(
                    RawAnimation.begin()
                            .then("swim.transition", Animation.LoopType.PLAY_ONCE)
                            .then("swim", Animation.LoopType.LOOP)
            );
            controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * 10));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState chasePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        if (this.isAttacking()) {
            AnimationController<?> controller = event.getController();
            controller.setAnimation(
                    RawAnimation.begin()
                            .then("chase", Animation.LoopType.LOOP)
            );
            controller.setAnimationSpeed(1.5);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
