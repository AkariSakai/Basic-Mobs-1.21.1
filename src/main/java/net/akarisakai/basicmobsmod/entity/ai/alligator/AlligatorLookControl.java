package net.akarisakai.basicmobsmod.entity.ai.alligator;

import net.akarisakai.basicmobsmod.entity.custom.AlligatorEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.mob.MobEntity;

public class AlligatorLookControl extends LookControl {
    private final AlligatorEntity alligator;

    public AlligatorLookControl(MobEntity entity) {
        super(entity);
        this.alligator = (AlligatorEntity) entity;
    }

    @Override
    public void tick() {
        if (alligator.isBasking()) {
            this.entity.setPitch(0);
            this.entity.setHeadYaw(this.entity.getYaw());
        } else {
            super.tick();
        }
    }
}