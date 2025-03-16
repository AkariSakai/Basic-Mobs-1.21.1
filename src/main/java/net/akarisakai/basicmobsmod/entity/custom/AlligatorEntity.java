package net.akarisakai.basicmobsmod.entity.custom;

import net.akarisakai.basicmobsmod.entity.ModEntities;
import net.akarisakai.basicmobsmod.entity.ai.alligator.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SwimNavigation;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

public class AlligatorEntity extends AnimalEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    public boolean targetingUnderwater;
    public SwimNavigation waterNavigation;
    public MobNavigation landNavigation;
    public HybridNavigation hybridNavigation;
    public int waterCooldown;

    public void startWaterCooldown() {
        this.waterCooldown = 600; // 30 sec (600 ticks)
        System.out.println("[Alligator] Cooldown activé : 30 secondes avant de retourner dans l'eau.");
    }

    public boolean isWaterCooldownOver() {
        return this.waterCooldown <= 0;
    }
    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        } else {
            if (waterCooldown > 0) {
                waterCooldown--;
                if (waterCooldown % 20 == 0) { // Afficher toutes les secondes
                    System.out.println("[Alligator] Cooldown avant retour à l'eau : " + waterCooldown / 20 + " sec");
                }
            }
        }
        if (this.getTarget() != null) {
            LivingEntity target = this.getTarget();

            // Vérifie si la cible est sur la terre
            if (!target.isTouchingWater() && this.isTouchingWater()) {
                this.setNavigation(this.landNavigation); // Force la navigation terrestre
                System.out.println("[Alligator] 🏃 Changement en LandNavigation pour attaquer !");
            }
        }
    }

    public void startLeavingWater() {
        this.setNavigation(this.hybridNavigation);
        this.hybridNavigation.setLeavingWater(true);
        System.out.println("[Alligator] Activation de la navigation hybride pour quitter l'eau !");
    }

    public AlligatorEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new AlligatorMoveControl(this);
        this.waterNavigation = new HybridSwimNavigation(this, world);
        this.landNavigation = new WideMobNavigation(this, world);
        this.hybridNavigation = new HybridNavigation(this, world);
        this.waterCooldown = 100 /*+ this.getRandom().nextInt(501)*/;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AlligatorAttackGoal(this, 2, true));
        this.goalSelector.add(2, new EnterWaterGoal(this));
        this.goalSelector.add(3, new LeaveWaterGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.goalSelector.add(3, new ActiveTargetGoal<>(this, ChickenEntity.class, true));
        this.goalSelector.add(3, new ActiveTargetGoal<>(this, SchoolingFishEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 0.8)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, 0.8)
                .add(EntityAttributes.GENERIC_OXYGEN_BONUS, 20)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }
    @Override
    protected Box getAttackBox() {
        return this.getBoundingBox().expand(1.3, 0.5, 1.3); // 🔥 Augmente la portée
    }

    public void setNavigation(EntityNavigation navigation) {
        this.navigation = navigation;
    }

    public boolean isTargetingUnderwater() {
        if (this.targetingUnderwater) {
            return true;
        } else {
            LivingEntity livingEntity = this.getTarget();
            return livingEntity != null && livingEntity.isTouchingWater();
        }
    }

    public double getWaterSurfaceY() {
        BlockPos pos = this.getBlockPos();
        while (this.getWorld().getBlockState(pos).isOf(Blocks.WATER) && pos.getY() < this.getWorld().getTopY()) {
            pos = pos.up();
        }
        return pos.getY() - 0.88f;
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isLogicalSideForUpdatingMovement() && this.isTouchingWater() && this.isTargetingUnderwater()) {
            this.updateVelocity(0.06F, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9));
        } else {
            super.travel(movementInput);
        }
    }

    @Override
    public boolean isInSwimmingPose() {
        return this.isSwimming();
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