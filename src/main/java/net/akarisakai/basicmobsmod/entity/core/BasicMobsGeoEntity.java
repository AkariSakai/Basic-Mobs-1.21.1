package net.akarisakai.basicmobsmod.entity.core;

import software.bernie.geckolib.animatable.GeoEntity;

public interface BasicMobsGeoEntity extends GeoEntity {

    @Override
    default double getBoneResetTime() {
        return 5;
    }
}