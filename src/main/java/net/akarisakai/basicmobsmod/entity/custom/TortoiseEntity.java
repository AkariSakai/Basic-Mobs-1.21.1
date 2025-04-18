package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.block.ModBlocks;
import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.tortoise.EggLayingBreedGoal;
import net.akarisakai.basicmobsmod.entity.ai.tortoise.HideGoal;
import net.akarisakai.basicmobsmod.entity.ai.tortoise.LayEggGoal;
import net.akarisakai.basicmobsmod.entity.core.BasicMobsGeoEntity;
import net.akarisakai.basicmobsmod.entity.core.EggLayingAnimal;
import net.akarisakai.basicmobsmod.entity.core.HidingAnimal;
import net.akarisakai.basicmobsmod.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class TortoiseEntity extends TameableEntity implements BasicMobsGeoEntity, HidingAnimal, EggLayingAnimal {
    private static final TrackedData<Boolean> HIDING = DataTracker.registerData(TortoiseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TRANSITIONING_FROM_HIDE = DataTracker.registerData(TortoiseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_EGG = DataTracker.registerData(TortoiseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LAYING_EGG = DataTracker.registerData(TortoiseEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Ingredient TEMPT_ITEMS = Ingredient.ofItems(Items.MELON_SLICE, Items.BEETROOT, Items.CARROT);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("tortoise.idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("tortoise.walk");
    protected static final RawAnimation SIT = RawAnimation.begin().thenLoop("tortoise.sit");
    protected static final RawAnimation HURT = RawAnimation.begin().thenPlay("tortoise.hurt");
    protected static final RawAnimation DIG = RawAnimation.begin().thenLoop("tortoise.dig");
    protected static final RawAnimation HIDE = RawAnimation.begin().thenPlay("tortoise.hide").thenLoop("tortoise.hide_idle");
    protected static final RawAnimation HIDE_TO_IDLE = RawAnimation.begin().thenPlay("tortoise.hide_toidle");
    protected static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("tortoise_baby.idle");
    protected static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("tortoise_baby.walk");
    protected static final RawAnimation BABY_HURT = RawAnimation.begin().thenPlay("tortoise_baby.hurt");
    protected static final RawAnimation BABY_HIDE = RawAnimation.begin().thenPlay("tortoise_baby.hide").thenLoop("tortoise_baby.hide_idle");
    protected static final RawAnimation BABY_HIDE_TO_IDLE = RawAnimation.begin().thenPlay("tortoise_baby.hide_toidle");
    private float hideProgress;
    private int layEggCounter;

    public TortoiseEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new HideGoal<>(this));
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));// Add these to initGoals()
        this.goalSelector.add(2, new EggLayingBreedGoal<>(this, 1.0));
        this.goalSelector.add(3, new LayEggGoal<>(this, 1.0));
        this.goalSelector.add(3, new FollowOwnerGoal(this, 1.0, 2.0f, 10.0f));
        this.goalSelector.add(4, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(5, new TemptGoal(this, 1.0, TEMPT_ITEMS, false));
        this.goalSelector.add(6, new WanderAroundGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.16)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    public void setTamed(boolean tamed, boolean updateAttributes) {
        super.setTamed(tamed, updateAttributes);
        var healthAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (tamed) {
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(25.0);
            }
            this.setHealth(25.0f);
        } else {
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(15.0);
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (this.getWorld().isClient) {
            if (this.isTamed() && this.isOwner(player)) {
                return ActionResult.SUCCESS;
            }
            if (this.isBreedingItem(itemStack) && (this.getHealth() < this.getMaxHealth() || !this.isTamed())) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        if (this.isHiding()) {
            return ActionResult.PASS;
        }

        if (this.isTamed()) {
            if (this.isOwner(player)) {
                if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    this.eat(player, hand, itemStack);
                    this.heal(3.0F);
                    return ActionResult.CONSUME;
                }
                ActionResult interactionResult = super.interactMob(player, hand);
                if (!interactionResult.isAccepted() || this.isBaby()) {
                    this.setSitting(!this.isSitting());
                }
                return interactionResult;
            }
        } else if (this.isBreedingItem(itemStack)) {
            this.eat(player, hand, itemStack);
            if (this.random.nextInt(3) == 0) {
                this.setOwner(player);
                this.setSitting(true);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            this.setPersistent();
            return ActionResult.CONSUME;
        }

        ActionResult result = super.interactMob(player, hand);
        if (result.isAccepted()) {
            this.setPersistent();
        }
        return result;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HIDING, false);
        builder.add(TRANSITIONING_FROM_HIDE, false);
        builder.add(HAS_EGG, false);
        builder.add(LAYING_EGG, false);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ITEM_SHIELD_BLOCK;
    }

    public boolean isTransitioningFromHide() {
        return this.dataTracker.get(TRANSITIONING_FROM_HIDE);
    }

    public void setTransitioningFromHide(boolean transitioning) {
        this.dataTracker.set(TRANSITIONING_FROM_HIDE, transitioning);
    }

    public boolean isHiding() {
        return this.dataTracker.get(HIDING);
    }

    public void setHiding(boolean hiding) {
        boolean wasHiding = this.isHiding();
        this.dataTracker.set(HIDING, hiding);

        if (wasHiding && !hiding) {
            this.setTransitioningFromHide(true);
        }
        else if (hiding) {
            this.setTransitioningFromHide(false);
        }
    }

    @Override
    public boolean canHide() {
        boolean shouldHide = false;
        if (!this.isTamed()) {
            List<PlayerEntity> players = this.getWorld().getEntitiesByClass(
                    PlayerEntity.class,
                    this.getBoundingBox().expand(5.0D, 3.0D, 5.0D),
                    player -> !player.isSpectator() && !player.isCreative() && !player.isInvisible() && !player.isSneaking()
                            && !isBreedingItem(player.getMainHandStack()) && !isBreedingItem(player.getOffHandStack())
            );
            shouldHide = !players.isEmpty();
        }
        if (!this.isInSittingPose()) {
            List<MobEntity> hostileMobs = this.getWorld().getEntitiesByClass(
                    MobEntity.class,
                    this.getBoundingBox().expand(5.0D, 3.0D, 5.0D),
                    mob -> mob.isAttacking() || isHostileMob(mob)
            );
            shouldHide = shouldHide || !hostileMobs.isEmpty();
        }

        setHiding(shouldHide);
        return shouldHide;
    }

    private boolean isHostileMob(MobEntity mob) {
        return mob.getType() == EntityType.ZOMBIE ||
                mob.getType() == EntityType.SKELETON ||
                mob.getType() == EntityType.CREEPER ||
                mob.getType() == EntityType.SPIDER ||
                mob.getType() == EntityType.ENDERMAN ||
                mob.getType() == EntityType.WITCH ||
                mob.getType() == EntityType.SLIME ||
                mob.getType() == EntityType.STRAY ||
                mob.getType() == EntityType.HUSK ||
                mob.getType() == EntityType.DROWNED ||
                mob.getType() == EntityType.PILLAGER ||
                mob.getType() == EntityType.VINDICATOR ||
                mob.getType() == EntityType.RAVAGER ||
                mob.getType() == ModEntities.ALLIGATOR ||
                mob.getType() == EntityType.VEX;
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movementController", 4, this::movementPredicate));
        controllers.add(new AnimationController<>(this, "hurtController", 4, this::hurtPredicate)
                .triggerableAnim("hurt", this.isBaby() ? BABY_HURT : HURT));
        controllers.add(new AnimationController<>(this, "hideController", 0, this::hidePredicate));
    }

    private <T extends GeoEntity> PlayState hidePredicate(final AnimationState<T> event) {
        if (this.isHiding()) {
            if (!event.getController().getAnimationState().equals(AnimationController.State.RUNNING)) {
                event.getController().setAnimation(this.isBaby() ? BABY_HIDE : HIDE);
            }
            return PlayState.CONTINUE;
        } else if (this.isTransitioningFromHide()) {
            if (event.getController().getAnimationState().equals(AnimationController.State.RUNNING) &&
                    event.getController().getCurrentAnimation() != null &&
                    ((this.isBaby() ? "tortoise_baby.hide_idle" : "tortoise.hide_idle").equals(event.getController().getCurrentAnimation().animation().name()))) {
                event.getController().setAnimation(this.isBaby() ? BABY_HIDE_TO_IDLE : HIDE_TO_IDLE);
                return PlayState.CONTINUE;
            }
            if (event.getController().hasAnimationFinished()) {
                this.setTransitioningFromHide(false);
                event.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState movementPredicate(AnimationState<T> state) {
        if (this.isHiding() || this.isTransitioningFromHide()) {
            return PlayState.STOP;
        }
        if (this.isLayingEgg()) {
            state.getController().setAnimation(DIG);
            return PlayState.CONTINUE;
        } else if (this.isInSittingPose()) {
            state.getController().setAnimation(SIT);
            return PlayState.CONTINUE;
        } else if (this.getVelocity().horizontalLengthSquared() > 1.0E-6) {
            state.getController().setAnimation(this.isBaby() ? BABY_WALK : WALK);
            return PlayState.CONTINUE;
        } else {
            state.getController().setAnimation(this.isBaby() ? BABY_IDLE : IDLE);
            return PlayState.CONTINUE;
        }
    }

    private <T extends GeoEntity> PlayState hurtPredicate(final AnimationState<T> event) {
        if(this.hurtTime > 0) {
            event.getController().setAnimation(this.isBaby() ? BABY_HURT : HURT);
            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        super.takeKnockback((this.isSitting() || this.isHiding()) ? strength / 4 : strength, x, z);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.isBaby() && (this.isHiding() || this.isInSittingPose()) && source.isOf(DamageTypes.ARROW)) {
            this.triggerAnim("hurtController", "hurt");
            this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
            Entity attacker = source.getSource();
            if (attacker instanceof PersistentProjectileEntity arrow) {
                arrow.setVelocity(0, 0, 0);
            }
            return false;
        }

        return super.damage(source, (this.isHiding() || this.isSitting()) ? amount * 0.8F : amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (updateHideProgress()) {
            this.setBoundingBox(this.calculateBoundingBox());
        }
        if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
            BlockPos pos = this.getBlockPos();
            if (this.getWorld().getBlockState(pos.down()).isIn(this.getEggLayableBlockTag())) {
                this.getWorld().syncWorldEvent(2001, pos, Block.getRawIdFromState(this.getWorld().getBlockState(pos.down())));
            }
        }

        if (!this.isBaby()) {
            List<Entity> entitiesAbove = this.getWorld().getOtherEntities(
                    this,
                    this.getBoundingBox().offset(0, 0.1, 0).expand(0.2, 0, 0.2),
                    EntityPredicates.canBePushedBy(this)
            );

            for (Entity entity : entitiesAbove) {
                if (entity.getBoundingBox().minY > this.getBoundingBox().maxY - 0.1) {
                } else {
                    this.pushAwayFrom(entity);
                }
            }
        }
    }

    private boolean updateHideProgress() {
        float target = this.isHiding() || this.isInSittingPose() ? 1.0F : 0.0F;
        if (hideProgress == target) {
            return false;
        }
        if (hideProgress < target) {
            hideProgress = Math.min(hideProgress + 0.12F, target);
        } else {
            hideProgress = Math.max(hideProgress - 0.12F, target);
        }

        return true;
    }

    @Override
    protected Box calculateBoundingBox() {
        float hidingHeightPercent = 0.794F;
        float currentHeightPercent = 1.0F - ((1.0F - hidingHeightPercent) * hideProgress);
        float height = this.getHeight() * currentHeightPercent;
        float width = this.getWidth() / 2.0F;

        return new Box(
                this.getX() - width, this.getY(), this.getZ() - width,
                this.getX() + width, this.getY() + height, this.getZ() + width
        );
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (HIDING.equals(data)) {
            this.setBoundingBox(this.calculateBoundingBox());
        }
        super.onTrackedDataSet(data);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return !this.isBaby();
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (!this.isBaby() && entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.pushAwayFrom(entity);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Hiding", this.isHiding());
        nbt.putBoolean("TransitioningFromHide", this.isTransitioningFromHide());
        nbt.putBoolean("HasEgg", this.hasEgg());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Hiding")) {
            this.setHiding(nbt.getBoolean("Hiding"));
        }
        if (nbt.contains("TransitioningFromHide")) {
            this.setTransitioningFromHide(nbt.getBoolean("TransitioningFromHide"));
        }
        if (nbt.contains("HasEgg")) {
            this.setHasEgg(nbt.getBoolean("HasEgg"));
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return TEMPT_ITEMS.test(stack);
    }

    @Override
    public boolean canBreedWith(AnimalEntity other) {
        if (!this.isTamed()) {
            return false;
        }
        if (!(other instanceof TortoiseEntity tortoise)) {
            return false;
        }
        return tortoise.isTamed() && super.canBreedWith(other) && !this.hasEgg() && !tortoise.hasEgg();
    }

    @Override
    public boolean isInLove() {
        return super.isInLove() && !this.hasEgg();
    }

    @Override
    public boolean hasEgg() {
        return this.dataTracker.get(HAS_EGG);
    }

    @Override
    public void setHasEgg(boolean hasEgg) {
        this.dataTracker.set(HAS_EGG, hasEgg);
    }

    @Override
    public boolean isLayingEgg() {
        return this.dataTracker.get(LAYING_EGG);
    }

    @Override
    public void setLayingEgg(boolean isLayingEgg) {
        this.dataTracker.set(LAYING_EGG, isLayingEgg);
    }

    @Override
    public int getLayEggCounter() {
        return this.layEggCounter;
    }

    @Override
    public void setLayEggCounter(int layEggCounter) {
        this.layEggCounter = layEggCounter;
    }

    @Override
    public Block getEggBlock() {
        return ModBlocks.TORTOISE_EGG;
    }

    @Override
    public TagKey<Block> getEggLayableBlockTag() {
        return ModTags.Blocks.TORTOISE_EGG_LAYABLE_ON;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        TortoiseEntity baby = ModEntities.TORTOISE.create(world);
        if (baby != null) {
            baby.setBaby(true);

            UUID ownerUUID = this.getOwnerUuid();
            if (ownerUUID != null) {
                baby.setOwnerUuid(ownerUUID);
                baby.setTamed(true, true);
            }
        }
        return baby;
    }
}