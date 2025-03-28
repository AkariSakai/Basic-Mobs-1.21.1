package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.alligator.*;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
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
import net.minecraft.util.math.Box;
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
    private static final int FEEDING_SOUND_DELAY = 17;
    private static final int FEEDING_PARTICLE_DELAY = 25;
    private static final int FEEDING_COOLDOWN_TICKS = 30;
    private static final float HEAL_AMOUNT_FROM_FEEDING = 5.0F;
    private static final double MOUNT_RANGE = 2.0;
    private static final int MAX_PASSENGERS = 3;
    private static final int DISMOUNT_COOLDOWN = 180;
    private static final int EJECT_TIMER_DELAY = 35;
    private static final double EJECT_VELOCITY_MULTIPLIER = 0.4;
    private static final double EJECT_UPWARD_VELOCITY = 0.3;
    private static final double PASSENGER_Y_OFFSET = 0.905;
    private static final int IDLE_ANIMATION_TIMEOUT = 40;
    private static final double ANIMATION_SPEED_FACTOR = 10.0;
    private static final double ATTACK_ANIMATION_SPEED_FACTOR = 100.0;
    private static final double CHASE_ANIMATION_SPEED = 1.5;
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BASKING = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Identifier BABY_HEALTH_MODIFIER_ID = Identifier.of("basicmobsmod:baby_health");
    private static final Identifier BABY_DAMAGE_MODIFIER_ID = Identifier.of("basicmobsmod:baby_damage");
    private static final EntityAttributeModifier BABY_HEALTH_MODIFIER =
            new EntityAttributeModifier(BABY_HEALTH_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final EntityAttributeModifier BABY_DAMAGE_MODIFIER =
            new EntityAttributeModifier(BABY_DAMAGE_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final double WATER_SURFACE_OFFSET = 0.25;
    private static final double BABY_WATER_SURFACE_OFFSET = 0.85;
    private static final double SWIM_TARGET_SPEED = 0.3;
    private static final double SWIM_BASE_SPEED = 0.05;
    private static final double SWIM_SPEED_FACTOR = 0.02;
    private static final String NBT_DAILY_HUNT_COUNT = "DailyHuntCount";
    private static final String NBT_LAST_HUNT_DAY = "LastHuntDay";
    private static final String NBT_IS_BABY = "IsBaby";
    private static final String NBT_HAS_BRED = "HasBredThisCycle";

    // Entity Tracking
    private static final TrackedData<ItemStack> HELD_ITEM = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final Set<Class<?>> EXCLUDED_HUNT_ENTITIES = Set.of(
            VillagerEntity.class, WanderingTraderEntity.class, SkeletonHorseEntity.class,
            SnowGolemEntity.class, AllayEntity.class, BatEntity.class, ParrotEntity.class,
            StriderEntity.class, PufferfishEntity.class, AlligatorEntity.class, BeeEntity.class
    );

    // Instance variables
    private Box cachedSearchBox;
    private boolean hasBredThisCycle = false;
    private boolean activelyTraveling;
    private boolean landBound;
    private BlockPos homePos;
    private int ejectTimer = 0;
    private int remountCooldown = 0;
    private int dailyHuntCount = 0;
    private long lastHuntDay = -1;
    private long lastFedTime = 0;
    private int feedingDelay = 0;
    private int feedingSoundDelay = 0;
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    private boolean wasAttacking;
    private int blinkTimer = 0;
    private int nextBlinkTime = 40 + random.nextInt(60); // 5-6 seconds initial delay



    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.lookControl = this.createLookControl();

    }

    @Override
    protected void initGoals() {
        // --- Attack & Targeting ---
        this.goalSelector.add(4, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, PassiveEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(2, new ActiveTargetGoal<>(this, FishEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(3, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.MEAT), false));
        this.goalSelector.add(3, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.FISHES), false));
        this.goalSelector.add(4, new AlligatorBiteGoal(this, 2.0));
        this.goalSelector.add(4, new RevengeGoal(this));

        // --- Movement & Navigation ---
        this.goalSelector.add(1, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(1, new BaskInSunGoal(this));
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

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isIn(ItemTags.MEAT) || this.isBreedingItem(itemStack)) {
            long currentTime = this.getWorld().getTime();

            if (currentTime - lastFedTime < FEEDING_COOLDOWN_TICKS) {
                return ActionResult.PASS;
            }

            if (!this.getWorld().isClient) {
                this.eat(player, hand, itemStack);
                this.heal(HEAL_AMOUNT_FROM_FEEDING);
                this.setHeldItem(itemStack.split(1));
                this.triggerAnim("feedController", "eat");

                lastFedTime = currentTime;
                feedingSoundDelay = FEEDING_SOUND_DELAY;
                feedingDelay = FEEDING_PARTICLE_DELAY;

                if (this.isBaby()) {
                    this.dailyHuntCount = MAX_DAILY_HUNTS;
                } else {
                    this.dailyHuntCount++;
                }

                if (this.dailyHuntCount >= MAX_DAILY_HUNTS && !this.hasBredThisCycle) {
                    this.setLoveTicks(600);
                }

                return ActionResult.SUCCESS;
            } else {
                return ActionResult.CONSUME;
            }
        }

        return super.interactMob(player, hand);
    }

    private boolean canHuntAndExclude(LivingEntity entity) {
        return entity != null &&
                !EXCLUDED_HUNT_ENTITIES.contains(entity.getClass()) &&
                this.dailyHuntCount < MAX_DAILY_HUNTS;
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AlligatorSwimNavigation(this, world);
    }

    protected MoveControl createMoveControl() {
        return new AlligatorMoveControl(this);
    }

    private void tryMountParent() {
        if (!this.isBaby() || this.remountCooldown > 0 || this.hasVehicle() ||
                !this.isTouchingWater() || this.isSubmergedInWater() || this.getTarget() != null) {
            return;
        }

        // Use cached bounding box if available
        Box searchBox = this.cachedSearchBox;
        if (searchBox == null || this.age % 20 == 0) {
            searchBox = this.getBoundingBox().expand(MOUNT_RANGE, 0.5, MOUNT_RANGE);
            this.cachedSearchBox = searchBox;
        }

        List<AlligatorEntity> nearbyAdults = this.getWorld().getEntitiesByClass(
                AlligatorEntity.class,
                searchBox,
                adult -> !adult.isBaby() &&
                        !adult.hasVehicle() &&
                        adult.getPassengerList().size() < MAX_PASSENGERS &&
                        !adult.isAttacking()
        );

        if (!nearbyAdults.isEmpty()) {
            AlligatorEntity closestAdult = nearbyAdults.get(0);
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
            this.remountCooldown = DISMOUNT_COOLDOWN;
        }
    }

    private void updateBlinking() {
        if (blinkTimer > 0) {
            blinkTimer--;
        } else {
            // Trigger blink and reset timer
            triggerAnim("blinkController", "blink");
            blinkTimer = nextBlinkTime;
            nextBlinkTime = 40 + random.nextInt(60); // 4-6 seconds for next blink
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 10, this::movementIdlePredicate));
        controllers.add(new AnimationController<>(this, "chaseController", 6, this::chasePredicate));
        controllers.add(new AnimationController<>(this, "blinkController", 0, state -> PlayState.STOP).triggerableAnim("blink", RawAnimation.begin().then("alligator.blinking", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "biteController", 8, this::bitePredicate)
                .triggerableAnim("bite", RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE)));

        controllers.add(new AnimationController<>(this, "feedController", 0, this::feedPredicate)
                .triggerableAnim("eat", RawAnimation.begin().then("eat", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "baskController", 10, state -> {
            if (this.isBasking()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("basking"));
            } else if (state.getController().getAnimationState() == AnimationController.State.RUNNING) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("basking").thenLoop("alligator.idle"));
            }
            return PlayState.STOP;
        }));
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
                controller.setAnimation(RawAnimation.begin().thenLoop("swim"));
                controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * ANIMATION_SPEED_FACTOR));
            } else {
                if (controller.getCurrentAnimation() != null &&
                        controller.getCurrentAnimation().animation().name().equals("basking")) {
                    controller.setAnimation(RawAnimation.begin().thenPlay("basking").thenLoop("walk"));
                } else {
                    controller.setAnimation(RawAnimation.begin().thenLoop("walk"));
                }
                controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() *
                        (this.isAttacking() ? ATTACK_ANIMATION_SPEED_FACTOR : ANIMATION_SPEED_FACTOR)));
            }
        } else {
            if (this.isTouchingWater()) {
                controller.setAnimation(RawAnimation.begin().thenLoop("swim.idle"));
            } else if (this.isBasking()) {
                return PlayState.STOP;
            } else {
                controller.setAnimation(RawAnimation.begin().thenLoop("alligator.idle"));
            }
        }
        return PlayState.CONTINUE;
    }

    private <T extends GeoEntity> PlayState chasePredicate(software.bernie.geckolib.animation.AnimationState<T> event) {
        AnimationController<?> controller = event.getController();

        if (this.isAttacking()) {
            if (!wasAttacking) {
                controller.setAnimation(RawAnimation.begin().thenLoop("chase"));
                controller.setAnimationSpeed(CHASE_ANIMATION_SPEED);
            }
            wasAttacking = true;
            return PlayState.CONTINUE;
        }
        else if (wasAttacking) {
            controller.setAnimation(RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE));
            wasAttacking = false;
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = IDLE_ANIMATION_TIMEOUT;
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
            this.hasBredThisCycle = false;
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
        if (this.hasVehicle()) return;

        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        double horizontalDistSqr = deltaX * deltaX + deltaZ * deltaZ;

        if (horizontalDistSqr < 0.01) return; // 0.1^2

        double deltaY = target.getY() - this.getY();
        double distance = Math.sqrt(horizontalDistSqr + deltaY * deltaY);

        double targetSpeed = Math.min(SWIM_TARGET_SPEED, SWIM_BASE_SPEED + (distance * SWIM_SPEED_FACTOR));
        double speed = MathHelper.lerp(0.1, this.getVelocity().length(), targetSpeed);

        this.setVelocity(
                MathHelper.lerp(0.2, this.getVelocity().x, (deltaX / distance) * speed),
                MathHelper.lerp(0.5, this.getVelocity().y, (deltaY / distance) * speed),
                MathHelper.lerp(0.2, this.getVelocity().z, (deltaZ / distance) * speed)
        );

        if (!this.hasVehicle()) {
            this.setYaw((float)(MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F);
        }
    }

    private void ejectBabiesOneByOne() {
        if (!this.getPassengerList().isEmpty() && ejectTimer-- <= 0) {
            Entity passenger = this.getPassengerList().getFirst();

            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                BlockPos targetPos = babyAlligator.getBlockPos().down();

                while (!this.getWorld().getBlockState(targetPos).isSolidBlock(this.getWorld(), targetPos) && targetPos.getY() > this.getBlockY() - 5) {
                    targetPos = targetPos.down();
                }

                babyAlligator.stopRiding();
                babyAlligator.setPosition(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5);
                babyAlligator.setVelocity(this.getVelocity().multiply(EJECT_VELOCITY_MULTIPLIER).add(0, EJECT_UPWARD_VELOCITY, 0));
                babyAlligator.remountCooldown = DISMOUNT_COOLDOWN;
                ejectTimer = EJECT_TIMER_DELAY;
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
        builder.add(HELD_ITEM, ItemStack.EMPTY);
        builder.add(BASKING, false);
    }

    protected LookControl createLookControl() {
        return new AlligatorLookControl(this);
    }


    public boolean isBasking() {
        return this.dataTracker.get(BASKING);
    }

    public void setBasking(boolean basking) {
        this.dataTracker.set(BASKING, basking);
        if (basking) {
            this.getLookControl().lookAt(this.getX(), this.getEyeY(), this.getZ());
        }
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
                this.setHealth(this.getMaxHealth());
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

        double waterSurfaceY = waterSurfacePos.getY() + this.getHeight() * WATER_SURFACE_OFFSET + (this.isBaby() ? BABY_WATER_SURFACE_OFFSET : WATER_SURFACE_OFFSET);
        double deltaY = waterSurfaceY - this.getY();
        double newVelY = MathHelper.lerp(0.2, this.getVelocity().y, deltaY * 0.1);

        this.setVelocity(this.getVelocity().x, newVelY, this.getVelocity().z);
    }

    @Override
    public void tick() {
        super.tick();

        // Only check daily hunt count every second (20 ticks)
        if (this.age % 20 == 0) {
            resetDailyHuntCount();
        }

        if (age % 5 == 0) { // Only check every 5 ticks for performance
            updateBlinking();
        }

        if (this.getWorld().isClient()) {
            setupAnimationStates();
        } else {
            // Server-side optimizations
            if (remountCooldown > 0) remountCooldown--;
            if (ejectTimer > 0) ejectTimer--;

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

            // Feeding effects
            if (feedingSoundDelay > 0 && --feedingSoundDelay == 0) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.2F, 1.0F);
            }

            if (feedingDelay > 0 && --feedingDelay == 0) {
                if (!this.getWorld().isClient) {
                    if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
                        ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
                    }
                    if (!this.getHeldItem().isEmpty()) {
                        this.setHeldItem(ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isBaby() && this.hasVehicle()) {
            this.stopRiding();
            this.remountCooldown = DISMOUNT_COOLDOWN;
        } else if (this.hasPassengers()) {
            ejectAllPassengers();
        }

        return super.damage(source, amount);
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
            double offsetY = PASSENGER_Y_OFFSET;
            double offsetX = 0.0;
            double offsetZ = 0.0;

            int passengerIndex = this.getPassengerList().indexOf(passenger);

            if (passenger instanceof AlligatorEntity babyAlligator && babyAlligator.isBaby()) {
                switch (passengerIndex) {
                    case 0 -> {
                        offsetX = 0.2;
                        offsetZ = 0.2;
                    }
                    case 1 -> {
                        offsetX = -0.2;
                        offsetZ = -0.2;
                    }
                }

                if (this.getPassengerList().size() == 3) {
                    switch (passengerIndex) {
                        case 0 -> offsetZ = 0.5;
                        case 1 -> offsetZ = 0.05;
                        case 2 -> {
                            offsetX = 0.2;
                            offsetZ = -0.35;
                        }
                    }
                }

                double yawRad = Math.toRadians(this.getYaw());
                double rotatedX = offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
                double rotatedZ = offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

                positionUpdater.accept(passenger, this.getX() + rotatedX, this.getY() + offsetY, this.getZ() + rotatedZ);
            } else {
                super.updatePassengerPosition(passenger, positionUpdater);
            }
        }
    }

    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean result = super.onKilledOther(world, other);

        if (result) {
            if (this.isBaby()) {
                this.dailyHuntCount = MAX_DAILY_HUNTS;
            } else {
                this.dailyHuntCount++;
            }
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
        nbt.putInt(NBT_DAILY_HUNT_COUNT, this.dailyHuntCount);
        nbt.putLong(NBT_LAST_HUNT_DAY, this.lastHuntDay);
        nbt.putBoolean(NBT_IS_BABY, this.isBaby());
        nbt.putBoolean(NBT_HAS_BRED, this.hasBredThisCycle);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dailyHuntCount = nbt.getInt(NBT_DAILY_HUNT_COUNT);
        this.lastHuntDay = nbt.getLong(NBT_LAST_HUNT_DAY);
        this.setBaby(nbt.getBoolean(NBT_IS_BABY));
        this.hasBredThisCycle = nbt.getBoolean(NBT_HAS_BRED);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(ItemTags.MEAT) || stack.isIn(ItemTags.FISHES);
    }

    @Override
    public boolean canBreedWith(AnimalEntity other) {
        if (other == this || !(other instanceof AlligatorEntity alligator)) {
            return false;
        }
        return this.isInLove() &&
                alligator.isInLove() &&
                this.dailyHuntCount >= MAX_DAILY_HUNTS &&
                alligator.dailyHuntCount >= MAX_DAILY_HUNTS &&
                !this.hasBredThisCycle &&
                !alligator.hasBredThisCycle &&
                !this.isBaby() &&
                !alligator.isBaby();
    }

    @Override
    public void breed(ServerWorld world, AnimalEntity other) {
        this.hasBredThisCycle = true;
        ((AlligatorEntity) other).hasBredThisCycle = true;
        this.setLoveTicks(0);
        other.setLoveTicks(0);
        super.breed(world, other);
        this.setBreedingAge(0);
        other.setBreedingAge(0);
    }

    protected int getXpToDrop() {
        if (this.isBaby()) {
            return 1 + this.random.nextInt(3);
        } else {
            return 5 + this.random.nextInt(7);
        }
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

    public ItemStack getHeldItem() {
        return this.dataTracker.get(HELD_ITEM);
    }

    public void setHeldItem(ItemStack stack) {
        this.dataTracker.set(HELD_ITEM, stack.copy());
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 10);
        }
    }

    public boolean isLandBound() {
        return !landBound;
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