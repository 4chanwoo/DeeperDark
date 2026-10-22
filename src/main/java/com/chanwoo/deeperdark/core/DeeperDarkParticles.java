package com.chanwoo.deeperdark.core;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

/**
 * The server only ever spawns these types. Each client decides how to draw them, either with vanilla
 * particles or with LodestoneLib when enhanced particles are turned on.
 */
public final class DeeperDarkParticles {
    /** Ring bursting out of a vase's mouth when it screams. */
    public static final DefaultParticleType VASE_BREAK_PARTICLE = register("vase_break_particle");
    /** Sculk smoke and trapped souls drifting out of a vase. */
    public static final DefaultParticleType VASE_AURA = register("vase_aura");
    /** Idle glow of a charged sculk capacitor. */
    public static final DefaultParticleType CAPACITOR_GLOW = register("capacitor_glow");
    /** A capacitor soaking up experience from a death. */
    public static final DefaultParticleType CAPACITOR_ABSORB = register("capacitor_absorb");
    /** A capacitor pushing charge into the catalyst below it. */
    public static final DefaultParticleType CAPACITOR_PULSE = register("capacitor_pulse");

    private static DefaultParticleType register(String name) {
        return Registry.register(Registry.PARTICLE_TYPE, new Identifier(MOD_ID, name), FabricParticleTypes.simple(true));
    }

    public static void init() {
    }

    private DeeperDarkParticles() {
    }
}
