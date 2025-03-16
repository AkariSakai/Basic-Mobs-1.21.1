package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class LeaveWaterGoal extends Goal {
    private final AlligatorEntity alligator;
    private final World world;
    private BlockPos landPos;
    private boolean isLeavingWater = false;

    public LeaveWaterGoal(AlligatorEntity alligator) {
        this.alligator = alligator;
        this.world = alligator.getWorld();
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        boolean shouldStart = alligator.isTouchingWater() && !alligator.targetingUnderwater;

        if (alligator.getTarget() != null) {
            return false; // Désactiver si une cible est présente
        }
        return shouldStart;
    }

    @Override
    public void start() {
        landPos = findNearbyLand();
        if (landPos != null) {
            System.out.println("[Alligator] Trouvé une plage en " + landPos);
            alligator.startLeavingWater(); // Activation de la navigation hybride
            boolean canMove = alligator.getNavigation().startMovingTo(landPos.getX(), landPos.getY(), landPos.getZ(), 1.2);
            System.out.println("[Alligator] Déplacement vers la terre réussi ? " + canMove);
        } else {
            System.out.println("[Alligator] Pas de plage trouvée, continue de nager.");
        }
    }


    @Override
    public void tick() {
        if (isLeavingWater) {
            if (!alligator.isTouchingWater()) {
                System.out.println("[Alligator] A quitté l'eau et est maintenant sur la terre !");
                alligator.startWaterCooldown();
                isLeavingWater = false;
            } else {
                System.out.println("[Alligator] Toujours en train d'essayer de sortir de l'eau...");
                jumpOutOfWater();
                moveToLand(); // Redonne la commande de déplacement en boucle
            }
        }
    }

    private void jumpOutOfWater() {
        BlockPos pos = alligator.getBlockPos();

        // Vérifier si l’alligator est collé à un bloc solide (rive)
        boolean isNearLand =
                !world.getBlockState(pos.north()).isOf(Blocks.WATER) && world.getBlockState(pos.north()).isSolid() ||
                        !world.getBlockState(pos.south()).isOf(Blocks.WATER) && world.getBlockState(pos.south()).isSolid() ||
                        !world.getBlockState(pos.east()).isOf(Blocks.WATER) && world.getBlockState(pos.east()).isSolid() ||
                        !world.getBlockState(pos.west()).isOf(Blocks.WATER) && world.getBlockState(pos.west()).isSolid();

        if (isNearLand) {
            System.out.println("[Alligator] Collé à la rive, tentative de saut !");
            alligator.setVelocity(alligator.getVelocity().x, 0.6, alligator.getVelocity().z); // Ajoute une impulsion vers le haut
        }
    }


    @Override
    public boolean shouldContinue() {
        boolean continueGoal = alligator.isTouchingWater();

        if (!continueGoal) { // Si l'alligator est bien sorti de l'eau
            System.out.println("[Alligator] A quitté l'eau et est maintenant sur la terre !");
            alligator.setNavigation(alligator.landNavigation); // Retour à la navigation terrestre
            alligator.startWaterCooldown(); // Active le cooldown de 30 sec
            isLeavingWater = false; // Fin du processus de sortie
        }

        if (alligator.getTarget() != null)
            return false;

        return continueGoal;
    }

    @Override
    public void stop() {
        System.out.println("[LeaveWaterGoal] ❌ Arrêt du goal, annulation de la navigation.");

        // 🛑 Annule le déplacement actuel
        alligator.getNavigation().stop();

        // ✅ Remet à zéro les variables du goal
        this.landPos = null;
        this.isLeavingWater = false;

        // 🔥 Recalcule immédiatement un chemin si une cible est présente
        if (alligator.getTarget() != null) {
            LivingEntity target = alligator.getTarget();
            System.out.println("[Alligator] 🎯 Cible détectée après sortie de l'eau, recalcul du chemin vers " + target.getBlockPos());

            alligator.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.5);
        }
    }


    private void moveToLand() {
        if (landPos != null) {
            boolean canMove = alligator.getNavigation().startMovingTo(landPos.getX(), landPos.getY(), landPos.getZ(), 1.2);
            System.out.println("[Alligator] Tentative de sortie de l'eau, succès ? " + canMove);
        }
    }

    private BlockPos findNearbyLand() {
        BlockPos alligatorPos = alligator.getBlockPos();
        BlockPos bestLandPos = null;
        int bestLandScore = -1; // Plus le score est élevé, plus la zone est idéale

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = alligatorPos.add(x, y, z);

                    if (!world.getBlockState(pos).isOf(Blocks.WATER) && world.getBlockState(pos.down()).isSolid()) {
                        // Vérifier qu'il y a une pente douce
                        int landScore = 0;
                        if (world.getBlockState(pos.up()).isAir()) landScore++; // Place libre au-dessus
                        if (world.getBlockState(pos.north()).isAir()) landScore++;
                        if (world.getBlockState(pos.south()).isAir()) landScore++;
                        if (world.getBlockState(pos.east()).isAir()) landScore++;
                        if (world.getBlockState(pos.west()).isAir()) landScore++;

                        if (landScore > bestLandScore) {
                            bestLandPos = pos;
                            bestLandScore = landScore;
                        }
                    }
                }
            }
        }

        if (bestLandPos != null) {
            System.out.println("[Alligator] Zone terrestre optimale trouvée en " + bestLandPos);
        } else {
            System.out.println("[Alligator] Impossible de trouver une sortie viable.");
        }

        return bestLandPos;
    }
}
