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
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
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
import net.minecraft.util.Identifier;
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
    private static final double MOUNT_RANGE = 2.0;
    private static final int MAX_PASSENGERS = 3;
    private static final int DISMOUNT_COOLDOWN = 180;
    private static final double EJECT_VELOCITY_MULTIPLIER = 0.4;
    private static final double EJECT_UPWARD_VELOCITY = 0.3;
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Identifier BABY_HEALTH_MODIFIER_ID = Identifier.of("basicmobsmod:baby_health");
    private static final Identifier BABY_DAMAGE_MODIFIER_ID = Identifier.of("basicmobsmod:baby_damage");
    private static final EntityAttributeModifier BABY_HEALTH_MODIFIER =
            new EntityAttributeModifier(BABY_HEALTH_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final EntityAttributeModifier BABY_DAMAGE_MODIFIER =
            new EntityAttributeModifier(BABY_DAMAGE_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

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
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;


    // Constructor
    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }


    // Initialization Methods
    @Override
    protected void initGoals() {
        // --- Attack & Targeting ---
        this.goalSelector.add(4, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, PassiveEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, FishEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(3, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.MEAT), false));
        this.goalSelector.add(4, new AlligatorBiteGoal(this, 2.0));
        this.goalSelector.add(4, new RevengeGoal(this));

        // --- Movement & Navigation ---
        this.goalSelector.add(1, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.00));
        this.goalSelector.add(7, new WanderInWaterGoal(this, 1.00));
        this.goalSelector.add(8, new WanderOnLandGoal(this, 1.00, 50));

        // --- Interaction & Misc ---
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.goalSelector.add(0, new AnimalMateGoal(this, 1.0));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28)
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

        if(this.isBreedingItem(itemStack)) {
            return super.interactMob(player, hand);
        }

        if (itemStack.isIn(ItemTags.MEAT)) {
            long currentTime = this.getWorld().getTime(); // Get current world time (in ticks)

            if (currentTime - lastFedTime < 30) {
                return ActionResult.PASS; // Ignore feeding if still on cooldown
            }

            if (!this.getWorld().isClient) {
                this.eat(player, hand, itemStack);
                this.heal(5.0F);
                this.triggerAnim("feedController", "eat");

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
        // Quick checks first
        if (!this.isBaby() || this.remountCooldown > 0 || this.hasVehicle() ||
                !this.isTouchingWater() || this.isSubmergedInWater() || this.getTarget() != null) {
            return;
        }

        // Use a more efficient entity lookup with a smaller search radius
        List<AlligatorEntity> nearbyAdults = this.getWorld().getEntitiesByClass(
                AlligatorEntity.class,
                this.getBoundingBox().expand(MOUNT_RANGE, 0.5, MOUNT_RANGE),
                adult -> !adult.isBaby() &&
                        !adult.hasVehicle() &&
                        adult.getPassengerList().size() < MAX_PASSENGERS &&
                        !adult.isAttacking()
        );

        if (!nearbyAdults.isEmpty()) {
            AlligatorEntity closestAdult = nearbyAdults.getFirst();
            double closestDist = this.squaredDistanceTo(closestAdult);

            for (int i = 1; i < nearbyAdults.size(); i++) {
                AlligatorEntity adult = nearbyAdults.get(i);
                double dist = this.squaredDistanceTo(adult);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestAdult = adult;
                }
            }

            this.startRiding(closestAdult, true);
            this.remountCooldown = 200;
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
        long currentDay = this.getWorld().getTimeOfDay() / 48000L;
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
        if (this.hasVehicle()) {
            return; // Don't update yaw if riding something
        }

        double deltaX = target.getX() - this.getX();
        double deltaY = target.getY() - this.getY();
        double deltaZ = target.getZ() - this.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (distance < 0.1) {
            return;
        }

        double targetSpeed = Math.min(0.3, 0.05 + (distance * 0.02));
        double speed = MathHelper.lerp(0.1, this.getVelocity().length(), targetSpeed);

        this.setVelocity(
                MathHelper.lerp(0.2, this.getVelocity().x, (deltaX / distance) * speed),
                MathHelper.lerp(0.5, this.getVelocity().y, (deltaY / distance) * speed),
                MathHelper.lerp(0.2, this.getVelocity().z, (deltaZ / distance) * speed)
        );

        // Update yaw to face target, but only if not riding something
        if (!this.hasVehicle()) {
            this.setYaw((float) (MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F);
        }
    }


    private void ejectBabiesOneByOne() {
        if (!this.getPassengerList().isEmpty() && ejectTimer-- <= 0) {
            Entity passenger = this.getPassengerList().getFirst();

            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                BlockPos targetPos = babyAlligator.getBlockPos().down();

                // Find a safe solid ground position
                while (!this.getWorld().getBlockState(targetPos).isSolidBlock(this.getWorld(), targetPos) && targetPos.getY() > this.getBlockY() - 5) {
                    targetPos = targetPos.down();
                }

                babyAlligator.stopRiding();

                // Set position on solid ground
                babyAlligator.setPosition(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5);

                babyAlligator.setVelocity(this.getVelocity().multiply(0.4).add(0, 0.3, 0));

                babyAlligator.remountCooldown = 180; // Prevent immediate remounting
                ejectTimer = 35; // Increased timer to prevent all babies ejecting instantly
            }
        }
    }

    private void ejectAllPassengers() {
        if (this.getPassengerList().isEmpty()) {
            return;
        }

        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof AlligatorEntity baby && baby.isBaby()) {
                baby.stopRiding();
                baby.setVelocity(this.getVelocity().multiply(EJECT_VELOCITY_MULTIPLIER).add(0, EJECT_UPWARD_VELOCITY, 0));
                baby.remountCooldown = DISMOUNT_COOLDOWN;
            }
        }
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
        this.calculateDimensions();
        EntityAttributeInstance health = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        EntityAttributeInstance damage = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        if (health != null) {
            health.removeModifier(BABY_HEALTH_MODIFIER_ID);
        }
        if (damage != null) {
            damage.removeModifier(BABY_DAMAGE_MODIFIER_ID);
        }
        if (baby) {
            if (health != null) {
                health.addPersistentModifier(BABY_HEALTH_MODIFIER);
                this.setHealth(this.getMaxHealth()); // Update current health
            }
            if (damage != null) {
                damage.addPersistentModifier(BABY_DAMAGE_MODIFIER);
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

        if (ejectTimer > 0) {
            ejectTimer--;
        }

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

        if (feedingSoundDelay > 0) {
            feedingSoundDelay--;
            if (feedingSoundDelay == 0) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.2F, 1.0F);
            }
        }

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
            if (this.isBaby() && this.hasVehicle()) {
                this.stopRiding();
                this.remountCooldown = DISMOUNT_COOLDOWN;
            } else if (this.hasPassengers()) {
                ejectAllPassengers();
            }
        }

        return result;
    }

    @Override
    public void stopRiding() {
        if (!this.isTouchingWater()) {
            super.stopRiding();
            if (this instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                babyAlligator.remountCooldown = DISMOUNT_COOLDOWN;
            }
        }
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            double offsetY = 0.905;
            double offsetX = 0.0;
            double offsetZ = 0.0;

            int passengerIndex = this.getPassengerList().indexOf(passenger);

            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                switch (passengerIndex) {
                    case 0 -> {
                        offsetX = 0.2;  // Start closer to center for Baby 1
                        offsetZ = 0.2;
                    }
                    case 1 -> {
                        offsetX = -0.2; // Start closer to center for Baby 2
                        offsetZ = -0.2;
                    }

                }

                // Transition to the final positions once all babies are present
                if (this.getPassengerList().size() == 3) {
                    switch (passengerIndex) {
                        case 0 -> offsetZ = 0.5;  // Final position for Baby 1
                        case 1 -> offsetZ = 0.05;  // Final position for Baby 2
                        case 2 -> {
                            offsetX = 0.2;
                            offsetZ = -0.35;
                        }  // Final position for Baby 3
                    }
                }

                // Rotate offsets based on parent yaw
                double yawRad = Math.toRadians(this.getYaw()); // Convert yaw to radians
                double rotatedX = offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
                double rotatedZ = offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

                // Set the passenger's position
                positionUpdater.accept(passenger, this.getX() + rotatedX, this.getY() + offsetY, this.getZ() + rotatedZ);
            } else {
                // For non-baby passengers, use the default method
                super.updatePassengerPosition(passenger, positionUpdater);
            }
        }
    }


    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean result = super.onKilledOther(world, other);

        if (result) {
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
        nbt.putBoolean("IsBaby", this.isBaby());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dailyHuntCount = nbt.getInt("DailyHuntCount");
        this.lastHuntDay = nbt.getLong("LastHuntDay");
        this.setBaby(nbt.getBoolean("IsBaby"));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.IRON_INGOT) || stack.isOf(Items.IRON_ORE);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        AlligatorEntity babyAlligator = ModEntities.ALLIGATOR.create(world);
        if (babyAlligator != null) {
            babyAlligator.setBaby(true);
            babyAlligator.setBreedingAge(-24000);
        }
        return babyAlligator;
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