package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.alligator.*;
import net.akarisakai.basicmobsmod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Bucketable;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.Set;

public class AlligatorEntity extends AnimalEntity implements GeoEntity, Bucketable {

    // --- Constants ---
    private static final int MAX_DAILY_HUNTS = 5;
    private static final int FEEDING_SOUND_DELAY = 17;
    private static final int FEEDING_PARTICLE_DELAY = 25;
    private static final int FEEDING_COOLDOWN_TICKS = 30;
    private static final float HEAL_AMOUNT_FROM_FEEDING = 5.0F;
    private static final int IDLE_ANIMATION_TIMEOUT = 40;
    private static final double ANIMATION_SPEED_FACTOR = 10.0;
    private static final double ATTACK_ANIMATION_SPEED_FACTOR = 100.0;
    private static final double CHASE_ANIMATION_SPEED = 1.5;
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BASKING = DataTracker.registerData(AlligatorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Identifier BABY_HEALTH_MODIFIER_ID = Identifier.of("basicmobsmod:baby_health");
    private static final Identifier BABY_DAMAGE_MODIFIER_ID = Identifier.of("basicmobsmod:baby_damage");
    private static final EntityAttributeModifier BABY_HEALTH_MODIFIER =
            new EntityAttributeModifier(BABY_HEALTH_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final EntityAttributeModifier BABY_DAMAGE_MODIFIER =
            new EntityAttributeModifier(BABY_DAMAGE_MODIFIER_ID, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final Set<Block> BREAKABLE_WATER_PLANTS = Set.of(
            Blocks.LILY_PAD
    );
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
    private boolean isResting = false;
    private double lastTickY;
    private float verticalDirection;
    private float bodyPitch;
    public int noVerticalMovementTicks;
    private boolean hasBredThisCycle = false;
    private int baskingCooldown = 0;
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
    private int nextBlinkTime = 40 + random.nextInt(60);


    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.lookControl = this.createLookControl();
        this.moveControl = this.createMoveControl();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(4, new AlligatorSurfaceRestGoal(this, 30, 60));
        this.goalSelector.add(1, new FleeAtLowHealthGoal(this, 2, 10.0f));
        this.goalSelector.add(2, new AlligatorAttackGoal(this, 2, true)); // Main attack
        this.goalSelector.add(3, new AlligatorRevengeGoal(this));
        this.goalSelector.add(4, new AlligatorBiteGoal(this, 2.0));
        this.goalSelector.add(5, new BaskInSunGoal(this));
        this.goalSelector.add(6, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(7, new FollowParentGoal(this, 1.10));
        this.goalSelector.add(11, new SwimAroundGoal(this, 1.0F, 10));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PassiveEntity.class, true, this::canHuntAndExclude));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, FishEntity.class, true, this::canHuntAndExclude));
        this.goalSelector.add(8, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.MEAT), false));
        this.goalSelector.add(8, new TemptGoal(this, 1.25, Ingredient.fromTag(ItemTags.FISHES), false));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 0.8)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 1.2)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (this.isBaby() && itemStack.getItem() == Items.WATER_BUCKET && this.isAlive()) {
            return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
        }

        if (this.isBasking()) {
            return ActionResult.PASS;
        }

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
        return new AlligatorMoveControl(this,
                85,  10,  1.0f, 1.0f, true);
    }

    private void updateBlinking() {
        if (blinkTimer > 0) {
            blinkTimer--;
        } else {
            // Trigger blink and reset timer
            triggerAnim("blinkController", "blink");
            blinkTimer = nextBlinkTime;
            nextBlinkTime = 40 + random.nextInt(60);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 12, this::movementIdlePredicate));
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

        boolean isOnMud = this.getBlockStateAtPos().getBlock() == Blocks.MUD;

        if (event.isMoving()) {
            if (this.isTouchingWater()&& !isOnMud) {
                controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() * ANIMATION_SPEED_FACTOR));
                return event.setAndContinue(RawAnimation.begin().thenLoop("swim"));
            } else if (isOnMud) {
                controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() *
                        (this.isAttacking() ? ATTACK_ANIMATION_SPEED_FACTOR : ANIMATION_SPEED_FACTOR)));
                return event.setAndContinue(RawAnimation.begin().thenLoop("mud.crawl"));
            } else {
                if (controller.getCurrentAnimation() != null &&
                        controller.getCurrentAnimation().animation().name().equals("basking")) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("basking").thenLoop("walk"));
                } else {
                    controller.setAnimationSpeed(1.0 + (this.getVelocity().lengthSquared() *
                            (this.isAttacking() ? ATTACK_ANIMATION_SPEED_FACTOR : ANIMATION_SPEED_FACTOR)));
                    return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                }
            }
        } else {
            if (this.isTouchingWater()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("swim.idle"));
            } else if (this.isBasking()) {
                return PlayState.STOP;
            } else if (isOnMud) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("mud.idle"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("alligator.idle"));
            }
        }
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

    private void breakWaterPlants() {
        if (this.horizontalCollision && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            boolean brokeAny = false;
            Box box = this.getBoundingBox().expand(0.2);

            for (BlockPos blockPos : BlockPos.iterate(
                    MathHelper.floor(box.minX),
                    MathHelper.floor(box.minY),
                    MathHelper.floor(box.minZ),
                    MathHelper.floor(box.maxX),
                    MathHelper.floor(box.maxY),
                    MathHelper.floor(box.maxZ))) {

                BlockState blockState = this.getWorld().getBlockState(blockPos);
                if (BREAKABLE_WATER_PLANTS.contains(blockState.getBlock())) {
                    brokeAny = this.getWorld().breakBlock(blockPos, true, this) || brokeAny;
                }
            }

            if (!brokeAny && this.isTouchingWater() && this.isOnGround()) {
                this.jump();
            }
        }
    }

    @Override
    public int getMaxAir() {
        return 1500;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(BABY, false);
        builder.add(HELD_ITEM, ItemStack.EMPTY);
        builder.add(BASKING, false);
        builder.add(FROM_BUCKET, false);
    }

    public boolean isResting() {
        return isResting;
    }

    public void setResting(boolean resting) {
        this.isResting = resting;
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
        this.setBreedingAge(baby ? -24000 : 0);
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
    @Override
    public void tickMovement() {
        super.tickMovement();

        if (this.isBaby() && this.getBreedingAge() < 0) {
            this.setBreedingAge(this.getBreedingAge() + 1);
            if (this.getBreedingAge() == 0) {
                this.setBaby(false);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isTouchingWater()) {
            updateBodyPitch();

            if (!this.isBaby() && this.getWorld().isClient && this.getVelocity().lengthSquared() > 0.01) {
                Vec3d vec3d = this.getRotationVec(0.0F);
                float f = MathHelper.cos(this.getYaw() * ((float)Math.PI / 180F)) * 0.3F;
                float g = MathHelper.sin(this.getYaw() * ((float)Math.PI / 180F)) * 0.3F;
                float h = 1.2F - this.random.nextFloat() * 0.7F;

                for(int i = 0; i < 2; ++i) {
                    this.getWorld().addParticle(ParticleTypes.DOLPHIN,
                            this.getX() - vec3d.x * (double)h + (double)f,
                            this.getY() - vec3d.y + 0.4,
                            this.getZ() - vec3d.z * (double)h + (double)g,
                            0.0, 0.0, 0.0);
                    this.getWorld().addParticle(ParticleTypes.DOLPHIN,
                            this.getX() - vec3d.x * (double)h - (double)f,
                            this.getY() - vec3d.y + 0.4,
                            this.getZ() - vec3d.z * (double)h - (double)g,
                            0.0, 0.0, 0.0);
                }
            }
        } else {
            bodyPitch = 0;
        }

        if (baskingCooldown > 0) baskingCooldown--;
        if (age % 20 == 0) resetDailyHuntCount();
        if (age % 5 == 0) updateBlinking();

        if (this.getWorld().isClient()) {
            setupAnimationStates();
        } else {
            updateServerSideLogic();
        }
    }

    private void updateBodyPitch() {
        double currentY = this.getY();
        double yChange = currentY - lastTickY;

        if (Math.abs(yChange) > 0.01) {
            verticalDirection = (float) Math.signum(yChange);
            noVerticalMovementTicks = 0;
        } else {
            noVerticalMovementTicks++;
        }

        float targetPitch = noVerticalMovementTicks < 5 ? verticalDirection * 0.2F : 0;
        float lerpSpeed = noVerticalMovementTicks < 5 ? 0.3F : 0.1F;
        bodyPitch = MathHelper.lerp(lerpSpeed, bodyPitch, targetPitch);

        lastTickY = currentY;
    }

    private void updateServerSideLogic() {

        if (this.isTouchingWater()) {
            breakWaterPlants();
        } else {
            this.setNoGravity(false);
        }

        updateFeedingLogic();
    }

    private void updateFeedingLogic() {
        if (feedingSoundDelay > 0 && --feedingSoundDelay == 0) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.2F, 1.0F);
        }

        if (feedingDelay > 0 && --feedingDelay == 0) {
            if (this.dailyHuntCount >= MAX_DAILY_HUNTS) {
                ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
            }
            if (!this.getHeldItem().isEmpty()) {
                this.setHeldItem(ItemStack.EMPTY);
            }
        }
    }

    public float getBodyPitch() {
        return bodyPitch;
    }


    @Override
    public boolean canBeLeashed() {
        return !this.isBaby();
    }


    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean result = super.onKilledOther(world, other);

        if (result) {
            if (this.isBaby()) {
                this.dailyHuntCount = MAX_DAILY_HUNTS;
                this.heal(HEAL_AMOUNT_FROM_FEEDING);
            } else {
                this.dailyHuntCount++;
                this.heal(HEAL_AMOUNT_FROM_FEEDING);
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
    public int getBaskingCooldown() {
        return baskingCooldown;
    }
    public void setBaskingCooldown(int cooldown) {
        this.baskingCooldown = cooldown;
    }

    @Override
    public boolean isFromBucket() {
        return this.getDataTracker().get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.getDataTracker().set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void copyDataToStack(ItemStack stack) {
        Bucketable.copyDataToStack(this, stack);
    }

    @Override
    public void copyDataFromNbt(NbtCompound nbt) {
        Bucketable.copyDataFromNbt(this, nbt);
    }

    @Override
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_AXOLOTL;
    }

    @Override
    public ItemStack getBucketItem() {
        return ModItems.BUCKET_OF_BABY_ALLIGATOR.getDefaultStack();
    }
}