package net.akarisakai.basicmobsmod.entity.ai.tortoise;

import net.akarisakai.basicmobsmod.entity.core.HidingAnimal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;

public class HideGoal<T extends MobEntity & HidingAnimal> extends Goal {
    private final T mob;

    public HideGoal(T mob) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        return mob.canHide();
    }

    @Override
    public void start() {
        mob.setJumping(false);
        mob.getNavigation().stop();
        mob.getMoveControl().moveTo(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
    }
}