package com.chanwoo.deeperdark.core;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkParticles {
    public static final DefaultParticleType VASE_BREAK_PARTICLE = Registry.register(Registry.PARTICLE_TYPE, new Identifier(MOD_ID, "vase_break_particle"), FabricParticleTypes.simple(true));

    public static void init() {
    }

    private DeeperDarkParticles() {
    }
}
