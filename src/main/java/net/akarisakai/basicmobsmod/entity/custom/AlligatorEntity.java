package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.alligator.*;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

import java.util.List;
import java.util.Set;

public class AlligatorEntity extends AnimalEntity implements GeoEntity {

    // --- Constants ---
    private static final int MAX_DAILY_HUNTS = 5;

    // --- AI & Navigation ---
    private boolean activelyTraveling;
    private boolean landBound;
    private BlockPos homePos;
    private int ejectTimer = 0;
    private int remountCooldown = 0;

    // --- Hunt Tracking ---
    private int dailyHuntCount = 0;
    private long lastHuntDay = -1;

    // --- Feeding System ---
    private long lastFedTime = 0;
    private int feedingDelay = 0;
    private int feedingSoundDelay = 0;

    // --- Animation State & Cache ---
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;


    // Constructor
    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }


    // Initialization Methods
    @Override
    protected void initGoals() {
        // --- Attack & Targeting ---
        this.goalSelector.add(3, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, PassiveEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(1, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.MEAT), false));
        this.goalSelector.add(4, new AlligatorBiteGoal(this, 2.0));

        // --- Movement & Navigation ---
        this.goalSelector.add(5, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.00));
        this.goalSelector.add(7, new WanderInWaterGoal(this, 1.00));
        this.goalSelector.add(8, new WanderOnLandGoal(this, 1.00, 50));

        // --- Interaction & Misc ---
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35)
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
            long currentTime = this.getWorld().getTime(); // Get current world time (in ticks)

            if (currentTime - lastFedTime < 30) {
                return ActionResult.PASS; // Ignore feeding if still on cooldown
            }

            if (!this.getWorld().isClient) {
                this.eat(player, hand, itemStack);
                this.heal(5.0F); // Heal the alligator by 5 HP
                this.triggerAnim("feedController", "eat"); // Trigger feed animation

                // Set cooldown
                lastFedTime = currentTime;
                feedingSoundDelay = 16; // Sound plays at * ticks
                feedingDelay = 25; // Hearts spawn at * ticks

                // Update hunt count
                if (this.isBaby()) {
                    this.dailyHuntCount = MAX_DAILY_HUNTS;
                } else {
                    this.dailyHuntCount++;
                }

                return ActionResult.SUCCESS;
            } else {
                return ActionResult.CONSUME;
            }
        }

        return super.interactMob(player, hand);
    }

    private static final Set<Class<?>> EXCLUDED_HUNT_ENTITIES = Set.of(
            VillagerEntity.class, WanderingTraderEntity.class, SkeletonHorseEntity.class,
            SnowGolemEntity.class, AllayEntity.class, BatEntity.class, ParrotEntity.class,
            StriderEntity.class, PufferfishEntity.class, AlligatorEntity.class
    );

    private boolean canHuntAndExclude(LivingEntity entity) {
        return entity != null && !EXCLUDED_HUNT_ENTITIES.contains(entity.getClass()) && canHunt(entity);
    }

    // Movement and Navigation
    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AlligatorSwimNavigation(this, world);
    }

    protected MoveControl createMoveControl() {
        return new AlligatorMoveControl(this);
    }

    private void tryMountParent() {
        if (!this.isBaby() || this.remountCooldown > 0 || this.hasVehicle()) {
            return; // Ensure it's a baby and not in cooldown
        }

        List<AlligatorEntity> nearbyAdults = this.getWorld().getEntitiesByClass(
                AlligatorEntity.class, this.getBoundingBox().expand(2.0), // Slightly increased range
                adult -> !adult.isBaby() && adult.getPassengerList().size() < 3
        );

        if (!nearbyAdults.isEmpty()) {
            AlligatorEntity closestAdult = nearbyAdults.stream()
                    .min((a, b) -> Double.compare(this.squaredDistanceTo(a), this.squaredDistanceTo(b)))
                    .orElse(null);

            if (closestAdult != null) {
                this.startRiding(closestAdult, true);
                this.remountCooldown = 200; // Prevent immediate remounting after dismounting
            }
        }
    }

    // Animation Handling
    private void setAnimationWithSpeed(AnimationController<?> controller, String transition, String loop, double speedFactor) {
        controller.setAnimation(
                RawAnimation.begin()
                        .then(transition, Animation.LoopType.PLAY_ONCE)
                        .thenLoop(loop)
        );
        controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * speedFactor));
    }

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
        if (feedingDelay > 0) {
            if (controller.getCurrentAnimation() == null) {
                controller.setAnimation(RawAnimation.begin().then("eat", Animation.LoopType.PLAY_ONCE));
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState bitePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();
        if (this.isAttacking()) {
            if (controller.getCurrentAnimation() == null) {
                controller.setAnimation(RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE));
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState movementIdlePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();

        if (event.isMoving()) {
            if (this.isTouchingWater()) {
                setAnimationWithSpeed(controller, "swim.transition", "swim", 10);
            } else if (this.isAttacking()) {
                setAnimationWithSpeed(controller, "walk.transition", "walk", 100);
            } else {
                setAnimationWithSpeed(controller, "walk.transition", "walk", 10);
            }
        } else {
            controller.setAnimation(
                    RawAnimation.begin()
                            .then("swim.idle", Animation.LoopType.LOOP)
            );
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
        controller.setAnimation(
                RawAnimation.begin()
                        .then("bite", Animation.LoopType.PLAY_ONCE)
        );
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
        return target != null && this.dailyHuntCount < MAX_DAILY_HUNTS;
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

    private void ejectBabiesOneByOne() {
        if (!this.getPassengerList().isEmpty() && ejectTimer-- <= 0) {
            Entity passenger = this.getPassengerList().get(0);

            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                babyAlligator.stopRiding();

                // Instead of instantly setting velocity, let it "jump" off naturally
                babyAlligator.setVelocity(this.getVelocity().multiply(0.4).add(0, 0.3, 0));

                babyAlligator.remountCooldown = 180; // Prevent it from immediately remounting
                ejectTimer = 40; // Increased timer to prevent all babies ejecting instantly
            }
        }
    }

    private void ejectAllPassengers() {
        List<Entity> passengers = this.getPassengerList();
        for (Entity passenger : passengers) {
            if (passenger instanceof AlligatorEntity baby && baby.isBaby()) {
                baby.stopRiding();
                baby.setVelocity(this.getVelocity().multiply(0.4).add(0, 0.3, 0));
                baby.remountCooldown = 180; // Prevent remounting too soon
            }
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
        double waterSurfaceY = waterSurfacePos.getY() + this.getHeight() * 0.25 + (this.isBaby() ? 0.85 : 0.25);
        double deltaY = waterSurfaceY - this.getY();
        double newVelY = MathHelper.lerp(0.2, this.getVelocity().y, deltaY * 0.1);
        this.setVelocity(this.getVelocity().x, newVelY, this.getVelocity().z);
    }

    // Overrides
    @Override
    public void tick() {
        super.tick();

        resetDailyHuntCount();

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }

        if (remountCooldown > 0) {
            remountCooldown--;
        }

        // If the alligator is in water and has a target, eject all passengers
        if (this.hasPassengers() && this.getTarget() != null) {
            ejectAllPassengers();
        }

        if (this.isTouchingWater()) {
            if (remountCooldown == 0) {
                tryMountParent();
            }
            handleWaterMovement();
        } else {
            this.setNoGravity(false);
            ejectBabiesOneByOne();
        }

        // Handle feeding sound delay (16 ticks)
        if (feedingSoundDelay > 0) {
            feedingSoundDelay--;
            if (feedingSoundDelay == 0) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.2F, 1.0F);
            }
        }

        // Handle heart particles delay (20 ticks), but only if it can't hunt anymore
        if (feedingDelay > 0) {
            feedingDelay--;
            if (feedingDelay == 0 && !this.getWorld().isClient) {
                if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
                    ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);

        if (result) {
            ejectAllPassengers(); // Babies will dismount when the alligator takes damage
        }

        return result;
    }


    @Override
    public void stopRiding() {
        if (!this.isTouchingWater()) { // Allow dismounting only if not in water
            super.stopRiding();
        }
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            double offsetY = 0.915; // Raise babies higher
            double offsetX = 0.0;
            double offsetZ = 0.0;

            int passengerIndex = this.getPassengerList().indexOf(passenger);
            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                switch (passengerIndex) {
                    case 0 -> { offsetX = 0.2; offsetZ = 0.5; }  // Baby 1 - More north, slightly right
                    case 1 -> { offsetX = -0.2; offsetZ = 0.05; } // Baby 2 - More south, slightly left
                    case 2 -> { offsetX = 0.2; offsetZ = -0.35; }  // Baby 3 - Even more south, slightly right
                }

                // Rotate offsets based on parent yaw
                double yawRad = Math.toRadians(this.getYaw()); // Convert yaw to radians
                double rotatedX = offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
                double rotatedZ = offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

                // Set the passenger's position
                positionUpdater.accept(passenger, this.getX() + rotatedX, this.getY() + offsetY, this.getZ() + rotatedZ);
            } else {
                super.updatePassengerPosition(passenger, positionUpdater);
            }
        }
    }



    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean result = super.onKilledOther(world, other); // Call parent method

        if (result) { // If the kill is valid
            // Set the daily hunt count to the maximum if the alligator is a baby
            if (this.isBaby()) {
                this.dailyHuntCount = MAX_DAILY_HUNTS;
            } else {
                this.dailyHuntCount++;
            }

            // Show particles when the hunt limit is reached
            if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
                world.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0, this.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0);
            }
        }

        return result;
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