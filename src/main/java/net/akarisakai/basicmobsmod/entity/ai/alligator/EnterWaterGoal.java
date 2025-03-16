package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Random;

public class EnterWaterGoal extends Goal {
    private final AlligatorEntity alligator;
    private final World world;
    private int swimmingTime;
    private final Random random = new Random();
    private BlockPos waterPos;
    private int failTimer = 0;
    private BlockPos lastPosition;
    private int retryTimer = 0;

    public EnterWaterGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.world = alligator.getWorld();
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {

        if (alligator.getTarget() != null) {
            return false; // Désactiver si une cible est présente
        }

        if (!world.isDay()) {
            return false;
        }

        if (alligator.isTouchingWater()) {
            return false;
        }

        if (!alligator.isWaterCooldownOver()) {
            return false;
        }

        boolean shouldStart = random.nextInt(100) < 20; // Probabilité de 20%

        if (shouldStart) {
            System.out.println("[EnterWaterGoal] Succès : L'alligator décide d'aller dans l'eau !");
        } else {
            System.out.println("[EnterWaterGoal] Échec : L'alligator ne veut pas aller dans l'eau cette fois.");
        }

        return shouldStart;
    }

    @Override
    public void start() {
        waterPos = findNearbyWater();

        if (waterPos != null) {
            System.out.println("[Alligator] Trouvé de l'eau en " + waterPos);
            alligator.setNavigation(alligator.landNavigation); // Change la navigation avant de bouger
            boolean canMove = alligator.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2);
            System.out.println("[Alligator] Déplacement vers l'eau réussi ? " + canMove);
            failTimer = 0; // Réinitialise le compteur de blocage
        } else {
            System.out.println("[Alligator] Aucune eau trouvée, activation d’un cooldown de 20 sec...");
            this.startShortCooldown();
        }

        swimmingTime = 200; // Temps de nage
    }

    public void startShortCooldown() {
        this.alligator.waterCooldown = 400; // 20 secondes (400 ticks)
    }

    @Override
    public void stop() {
        System.out.println("[EnterWaterGoal] Goal stoppé, réinitialisation complète...");

        this.waterPos = null;
        this.failTimer = 0;
        this.lastPosition = null;
        this.retryTimer = 0;
        this.alligator.waterCooldown = 400;
        System.out.println("[Alligator] Cooldown de 20 secondes activé avant nouvelle recherche d'eau.");

        if (alligator.getTarget() != null) {
            LivingEntity target = alligator.getTarget();
            System.out.println("[Alligator] 🎯 Cible détectée après sortie de l'eau, recalcul du chemin vers " + target.getBlockPos());

            alligator.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.5);
        }
    }

    @Override
    public void tick() {
        if (alligator.isTouchingWater()) {
            System.out.println("[Alligator] Entré dans l'eau !");
            alligator.setNavigation(alligator.waterNavigation); // Passe en navigation aquatique
            swimmingTime--;

            if (swimmingTime % 20 == 0) {
                System.out.println("[Alligator] Nage... Temps restant : " + swimmingTime / 20 + " sec");
            }

            if (swimmingTime <= 0) {
                System.out.println("[Alligator] Fin de la baignade, cherche une plage.");
                alligator.targetingUnderwater = false;
            }

            failTimer = 0; // Reset du compteur si l'alligator atteint l'eau
        } else if (waterPos != null) {
            System.out.println("[Alligator] Toujours en route vers l'eau... Position actuelle : " + alligator.getBlockPos());

            // Vérification après 5 secondes d'immobilité
            if (lastPosition != null && alligator.getBlockPos().equals(lastPosition)) {
                failTimer++;
            } else {
                failTimer = 0;
            }

            // Relance la navigation après 5 secondes bloqué
            if (failTimer > 100) { // 5 secondes (20 ticks * 5)
                System.out.println("[Alligator] Bloqué ! Relancement du déplacement vers l'eau...");
                boolean canMove = alligator.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2);
                System.out.println("[Alligator] Nouvelle tentative de déplacement, succès ? " + canMove);
                failTimer = 0; // Réinitialise le compteur
            }

            lastPosition = alligator.getBlockPos();
        }
    }

    @Override
    public boolean shouldContinue() {
        return alligator.isAlive() && alligator.waterCooldown <= 0 && (!alligator.isTouchingWater() || swimmingTime > 0) &&
                this.alligator.getTarget() != null;
    }

    private BlockPos findNearbyWater() {
        BlockPos alligatorPos = alligator.getBlockPos();
        BlockPos bestWaterPos = null;
        int bestWaterScore = -1;

        int range = 15; // Était 10, maintenant 15 pour +50% de portée

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -4; y <= 4; y++) { // Était -3 à 3, maintenant -4 à 4 pour mieux détecter
                    BlockPos pos = alligatorPos.add(x, y, z);

                    if (world.getBlockState(pos).isOf(Blocks.WATER)) {
                        // Vérifier que l’eau est bien entourée
                        int waterCount = 0;
                        if (world.getBlockState(pos.north()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.south()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.east()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.west()).isOf(Blocks.WATER)) waterCount++;
                        if (world.getBlockState(pos.down()).isOf(Blocks.WATER)) waterCount++;

                        // Vérifier que ce bloc d'eau ne touche pas immédiatement un bloc terrestre
                        boolean isNearLand =
                                (!world.getBlockState(pos.north()).isOf(Blocks.WATER) && world.getBlockState(pos.north()).isSolid()) ||
                                        (!world.getBlockState(pos.south()).isOf(Blocks.WATER) && world.getBlockState(pos.south()).isSolid()) ||
                                        (!world.getBlockState(pos.east()).isOf(Blocks.WATER) && world.getBlockState(pos.east()).isSolid()) ||
                                        (!world.getBlockState(pos.west()).isOf(Blocks.WATER) && world.getBlockState(pos.west()).isSolid());

                        // Vérifier que l'eau est profonde (éviter les bords de plage)
                        boolean isDeepEnough =
                                world.getBlockState(pos.down()).isOf(Blocks.WATER) &&
                                        world.getBlockState(pos.down(2)).isOf(Blocks.WATER);

                        if (!isNearLand && isDeepEnough && waterCount > bestWaterScore) {
                            bestWaterPos = pos;
                            bestWaterScore = waterCount;
                        }
                    }
                }
            }
        }

        return bestWaterPos;
    }
}
