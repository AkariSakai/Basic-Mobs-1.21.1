package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HybridNavigation extends EntityNavigation {
    private final SwimNavigation swimNavigation;
    private final MobNavigation landNavigation;
    private boolean isLeavingWater = false;

    AlligatorEntity entity;

    public HybridNavigation(AlligatorEntity mobEntity, World world) {
        super(mobEntity, world);
        this.entity= mobEntity;
        this.swimNavigation = new SwimNavigation(mobEntity, world);
        this.landNavigation = new MobNavigation(mobEntity, world);
    }

    @Override
    protected PathNodeNavigator createPathNodeNavigator(int range) {
        return new PathNodeNavigator(new WaterPathNodeMaker(false), range);
    }

    @Override
    protected boolean isAtValidPosition() {
        return entity.isTouchingWater() || !entity.isSubmergedInWater();
    }

    @Override
    protected Vec3d getPos() {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    @Override
    public boolean startMovingTo(double x, double y, double z, double speed) {
        if (entity.isTouchingWater()) {
            if (isLeavingWater) {
                System.out.println("[HybridNavigation] Sortie de l'eau en cours, utilisation de la nav terrestre !");
                return landNavigation.startMovingTo(x, y, z, speed);
            } else {
                return swimNavigation.startMovingTo(x, y, z, speed);
            }
        } else {
            return landNavigation.startMovingTo(x, y, z, speed);
        }
    }

    @Override
    public void tick() {
        LivingEntity target = entity.getTarget();

        if (entity.isTouchingWater()) {
            if (isLeavingWater) {
                // Vérifier si la cible est sous l'eau avant de sortir
                if (target != null && target.isTouchingWater() && target.getY() < entity.getY()) {
                    System.out.println("[HybridNavigation] 🚨 Cible sous l'eau détectée ! Reprise de la navigation aquatique !");
                    isLeavingWater = false;
                    swimNavigation.tick();
                    return;
                }

                // Forcer l'alligator à remonter vers la surface avant d'utiliser la navigation terrestre
                Vec3d surfacePos = new Vec3d(entity.getX(), entity.getWaterSurfaceY(), entity.getZ());

                if (entity.getY() < entity.getWaterSurfaceY() - 0.5) {
                    System.out.println("[HybridNavigation] ⏫ Montée forcée vers " + surfacePos);
                    entity.setVelocity(entity.getVelocity().x, 0.3, entity.getVelocity().z);
                } else {
                    System.out.println("[HybridNavigation] 🌊 Sortie de l'eau terminée, passage à la navigation terrestre !");
                    landNavigation.tick(); // Une fois hors de l'eau, utiliser la navigation terrestre
                }
            } else {
                swimNavigation.tick();
            }
        } else {
            landNavigation.tick();
        }
    }



    public void setLeavingWater(boolean leaving) {
        this.isLeavingWater = leaving;
    }
}
