package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.alligator.*;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
    private boolean activelyTraveling;
    private boolean landBound;
    private BlockPos homePos;

    private int dailyHuntCount = 0;
    private long lastHuntDay = -1;
    private static final int MAX_DAILY_HUNTS = 5;

    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(3, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.00));
        this.goalSelector.add(5, new WanderInWaterGoal(this, 1.00));
        this.goalSelector.add(6, new WanderOnLandGoal(this, 1.00, 10)); // Add this line
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new ActiveTargetGoal<>(this, ChickenEntity.class, true, this::canHunt));
        this.goalSelector.add(10, new ActiveTargetGoal<>(this, SchoolingFishEntity.class, true, this::canHunt));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 0.8)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0.8)
                .add(EntityAttributes.GENERIC_OXYGEN_BONUS, 20)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    public boolean isLandBound() {
        return landBound;
    }

    public void setLandBound(boolean landBound) {
        this.landBound = landBound;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos;
    }

    public boolean isActivelyTraveling() {
        return activelyTraveling;
    }

    public void setActivelyTraveling(boolean activelyTraveling) {
        this.activelyTraveling = activelyTraveling;
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AlligatorSwimNavigation(this, world);
    }

    protected MoveControl createMoveControl() {
        return new AlligatorMoveControl(this);
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

        long currentDay = this.getWorld().getTimeOfDay() / 24000L;
        if (currentDay != this.lastHuntDay) {
            this.dailyHuntCount = 0;
            this.lastHuntDay = currentDay;
        }

        if (this.isTouchingWater()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && target.isTouchingWater()) {
                // Swim smoothly towards the target's position
                double targetX = target.getX();
                double targetY = target.getY();
                double targetZ = target.getZ();
                double deltaX = targetX - this.getX();
                double deltaY = targetY - this.getY();
                double deltaZ = targetZ - this.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                double speed = 0.1; // Adjust the speed as needed

                // Interpolate the velocity for smoother movement
                double newVelX = MathHelper.lerp(0.1, this.getVelocity().x, deltaX / distance * speed);
                double newVelY = MathHelper.lerp(0.1, this.getVelocity().y, deltaY / distance * speed);
                double newVelZ = MathHelper.lerp(0.1, this.getVelocity().z, deltaZ / distance * speed);
                this.setVelocity(newVelX, newVelY, newVelZ);
            } else {
                // Slowly swim back up to 25% higher than the top of the water block
                BlockPos waterSurfacePos = this.getBlockPos().up();
                while (this.getWorld().getFluidState(waterSurfacePos).isStill()) {
                    waterSurfacePos = waterSurfacePos.up();
                }
                double waterSurfaceY = waterSurfacePos.getY() - 1.0 + (this.getHeight() * 0.25) + 0.6;

                // Check if the alligator is near the edge of the water
                boolean nearEdge = !this.getWorld().getFluidState(this.getBlockPos().north()).isStill() ||
                        !this.getWorld().getFluidState(this.getBlockPos().south()).isStill() ||
                        !this.getWorld().getFluidState(this.getBlockPos().east()).isStill() ||
                        !this.getWorld().getFluidState(this.getBlockPos().west()).isStill();

                if (!nearEdge) {
                    double currentY = this.getY();
                    double deltaY = waterSurfaceY - currentY;
                    double speed = 0.1;

                    double newVelY = MathHelper.lerp(0.1, this.getVelocity().y, deltaY * speed);
                    this.setVelocity(this.getVelocity().x, newVelY, this.getVelocity().z);
                }
            }
        } else {
            // Handle land movement
            this.setNoGravity(false); // Enable gravity for land movement
            this.setVelocity(this.getVelocity().x, this.getVelocity().y, this.getVelocity().z);
        }
    }

    private boolean canHunt(@Nullable LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
            return false;
        }
        this.dailyHuntCount++;
        return true;
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
        controllers.add(new AnimationController<>(this, "movementController", 0, this::movementIdlePredicate));
        controllers.add(new AnimationController<>(this, "chaseController", 8, this::chasePredicate));
    }

    private <T extends GeoEntity> PlayState movementIdlePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();

        if (event.isMoving()) {
            if (this.isTouchingWater()) {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("swim.transition", Animation.LoopType.PLAY_ONCE)
                                .thenLoop("swim")
                );
                controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * 10));
            } else {
                if (this.isAttacking()) {
                    controller.setAnimation(
                            RawAnimation.begin()
                                    .then("walk.transition", Animation.LoopType.PLAY_ONCE)
                                    .thenLoop("walk")
                    );
                    controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * 100)); // Speed up if targeting
                } else {
                    controller.setAnimation(
                            RawAnimation.begin()
                                    .then("walk.transition", Animation.LoopType.PLAY_ONCE)
                                    .thenLoop("walk")
                    );
                    controller.setAnimationSpeed(1.5 + (this.getVelocity().lengthSquared() * 10));
                }
            }
            return PlayState.CONTINUE;
        } else {
            // Entity is idle
            if (this.isTouchingWater()) {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("swim.idle", Animation.LoopType.LOOP)
                );
            } else {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("alligator.idle", Animation.LoopType.LOOP) // Idle walking animation
                );
            }
            return PlayState.CONTINUE;
        }
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
        AnimationController<?> controller = event.getController();
        controller.setAnimation(
                RawAnimation.begin()
                        .then("bite", Animation.LoopType.PLAY_ONCE)
        );
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}