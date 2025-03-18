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
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

public class AlligatorEntity extends AnimalEntity implements GeoEntity {

    // Fields
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    private boolean activelyTraveling;
    private boolean landBound;
    private BlockPos homePos;
    private int dailyHuntCount = 0;
    private long lastHuntDay = -1;
    private static final int MAX_DAILY_HUNTS = 5;

    // Constructor
    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    // Initialization Methods
    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(1, new TemptGoal(this, 1.25, Ingredient.ofItems(Items.CHICKEN, Items.COOKED_CHICKEN, Items.SALMON, Items.COOKED_SALMON, Items.COD, Items.COOKED_COD), false));
        this.goalSelector.add(3, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.00));
        this.goalSelector.add(5, new WanderInWaterGoal(this, 1.00));
        this.goalSelector.add(6, new WanderOnLandGoal(this, 1.00, 50));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new ActiveTargetGoal<>(this, PassiveEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(11, new AlligatorBiteGoal(this, 2.0)); // Add the bite goal with a range of 2.0 blocks
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 0.8)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 1.2)
                .add(EntityAttributes.GENERIC_OXYGEN_BONUS, 20)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    // Interaction Methods
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isIn(ItemTags.MEAT)) {
            if (!this.getWorld().isClient) {
                this.eat(player, hand, itemStack);
                this.heal(5.0F); // Heal the alligator by 5 health points
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                this.triggerAnim("feedController", "eat"); // Trigger the feed animation

                // Increment the daily hunt count
                this.dailyHuntCount++;

                // Check if the alligator is full
                if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
                    // Spawn heart particles
                    ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                }

                return ActionResult.SUCCESS;
            } else {
                return ActionResult.CONSUME;
            }
        }

        return super.interactMob(player, hand);
    }

    private boolean canHuntAndExclude(LivingEntity entity) {
        // Exclude specific mobs
        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity ||
                entity instanceof SkeletonHorseEntity || entity instanceof SnowGolemEntity ||
                entity instanceof AllayEntity || entity instanceof BatEntity || entity instanceof ParrotEntity ||
                entity instanceof StriderEntity || entity instanceof PufferfishEntity ||
                entity instanceof AlligatorEntity) {
            return false;
        }
        return canHunt(entity);
    }
    // Movement and Navigation
    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AlligatorSwimNavigation(this, world);
    }

    protected MoveControl createMoveControl() {
        return new AlligatorMoveControl(this);
    }

    // Animation Handling
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 0, this::movementIdlePredicate));
        controllers.add(new AnimationController<>(this, "chaseController", 8, this::chasePredicate));
        controllers.add(new AnimationController<>(this, "biteController", 8, this::bitePredicate)
                .triggerableAnim("bite", RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE)));

        controllers.add(new AnimationController<>(this, "feedController", 0, this::feedPredicate)
                .triggerableAnim("eat", RawAnimation.begin().then("eat", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoEntity> PlayState feedPredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();
        if (controller.getCurrentAnimation() == null) {
            controller.setAnimation(RawAnimation.begin().then("eat", Animation.LoopType.PLAY_ONCE));
        }
        return PlayState.CONTINUE;
    }

    private <T extends GeoEntity> PlayState bitePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();
        if (controller.getCurrentAnimation() == null) {
            controller.setAnimation(RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE));
        }
        return PlayState.CONTINUE;
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
        } else {
            if (this.isTouchingWater()) {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("swim.idle", Animation.LoopType.LOOP)
                );
            } else {
                if (this.idleAnimationState.isRunning()) {
                    controller.setAnimation(
                            RawAnimation.begin()
                                    .then("alligator.idle", Animation.LoopType.LOOP)
                    );
                }
            }
        }
        return PlayState.CONTINUE;
    }

    private <T extends GeoEntity> PlayState chasePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();
        if (this.isAttacking()) {
            controller.setAnimation(
                    RawAnimation.begin()
                            .then("chase", Animation.LoopType.LOOP)
            );
            controller.setAnimationSpeed(1.5);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    // Utility Methods
    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    private void resetDailyHuntCount() {
        long currentDay = this.getWorld().getTimeOfDay() / 24000L;
        if (currentDay != this.lastHuntDay) {
            this.dailyHuntCount = 0;
            this.lastHuntDay = currentDay;
        }
    }

    public boolean canHunt(@Nullable LivingEntity target) {
        if (target == null || this.dailyHuntCount >= MAX_DAILY_HUNTS) {
            return false;
        }
        this.dailyHuntCount++;
        return true;
    }

    private void handleWaterMovement() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && target.isTouchingWater()) {
            swimTowardsTarget(target);
        } else {
            moveToWaterSurface();
        }
    }

    private void swimTowardsTarget(LivingEntity target) {
        double deltaX = target.getX() - this.getX();
        double deltaY = target.getY() - this.getY();
        double deltaZ = target.getZ() - this.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        double targetSpeed = Math.min(0.3, 0.05 + (distance * 0.02));
        double speed = MathHelper.lerp(0.1, this.getVelocity().length(), targetSpeed);

        if (distance > 0.1) { // Ensure distance is significant to prevent spinning
            this.setVelocity(
                    MathHelper.lerp(0.2, this.getVelocity().x, (deltaX / distance) * speed),
                    MathHelper.lerp(0.5, this.getVelocity().y, (deltaY / distance) * speed),
                    MathHelper.lerp(0.2, this.getVelocity().z, (deltaZ / distance) * speed)
            );
            this.setYaw((float) (MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F);
        }
    }

    private void moveToWaterSurface() {
        BlockPos waterSurfacePos = this.getBlockPos();
        for (int i = 0; i < 10; i++) {
            BlockPos checkPos = waterSurfacePos.up(i);
            if (!this.getWorld().getFluidState(checkPos).isStill()) {
                break;
            }
            waterSurfacePos = checkPos;
        }
        double waterSurfaceY = waterSurfacePos.getY() + this.getHeight() * 0.25 + 0.25;
        double deltaY = waterSurfaceY - this.getY();
        double newVelY = MathHelper.lerp(0.2, this.getVelocity().y, deltaY * 0.1);
        this.setVelocity(this.getVelocity().x, newVelY, this.getVelocity().z);
    }

    // Overrides
    @Override
    public void tick() {
        super.tick();

        // Ensure daily hunt count is reset before any checks happen
        resetDailyHuntCount();

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }

        if (this.isTouchingWater()) {
            handleWaterMovement();
        } else {
            this.setNoGravity(false);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("DailyHuntCount", this.dailyHuntCount);
        nbt.putLong("LastHuntDay", this.lastHuntDay);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dailyHuntCount = nbt.getInt("DailyHuntCount");
        this.lastHuntDay = nbt.getLong("LastHuntDay");
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    // Getters and Setters
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
}