package com.chanwoo.deeperdark.client.particle;

import com.chanwoo.deeperdark.client.DeeperDarkConfig;
import com.chanwoo.deeperdark.core.DeeperDarkParticles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Each of Deeper Dark's particle types is drawn either with LodestoneLib or with vanilla particles, decided
 * as it spawns, so toggling the setting takes effect immediately.
 * <p>
 * The spawn's velocity carries extra data for some types: the vase aura's swirl angle, the capacitor glow's
 * fill and angle, and the offset to the mob a capacitor is absorbing.
 */
@Environment(EnvType.CLIENT)
public final class DeeperDarkParticleFactories {
    @FunctionalInterface
    private interface Effect {
        void spawn(ClientWorld world, double x, double y, double z, double vx, double vy, double vz);
    }

    public static void register() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(DeeperDarkParticles.VASE_BREAK_PARTICLE, sprites -> (type, world, x, y, z, vx, vy, vz) -> {
            if (DeeperDarkConfig.useLodestoneParticles()) {
                LodestoneEffects.vaseBreak(world, x, y, z);
                return null;
            }
            if (DeeperDarkConfig.useGlowingVanillaParticles()) {
                return new VaseBreakParticle(world, x, y, z, sprites, LodestoneEffects.additiveSheet());
            }
            return new VaseBreakParticle(world, x, y, z, sprites);
        });
        registry.register(DeeperDarkParticles.VASE_AURA, effect(
                (world, x, y, z, vx, vy, vz) -> LodestoneEffects.vaseAura(world, x, y, z, vx, vy),
                DeeperDarkParticleFactories::vanillaVaseAura));
        registry.register(DeeperDarkParticles.CAPACITOR_GLOW, effect(
                (world, x, y, z, vx, vy, vz) -> LodestoneEffects.capacitorGlow(world, x, y, z, vx, vy),
                DeeperDarkParticleFactories::vanillaCapacitorGlow));
        registry.register(DeeperDarkParticles.CAPACITOR_ABSORB, effect(
                LodestoneEffects::capacitorAbsorb,
                DeeperDarkParticleFactories::vanillaCapacitorAbsorb));
        registry.register(DeeperDarkParticles.CAPACITOR_PULSE, effect(
                (world, x, y, z, vx, vy, vz) -> LodestoneEffects.capacitorPulse(world, x, y, z),
                DeeperDarkParticleFactories::vanillaCapacitorPulse));
    }

    /** A factory that spawns other particles in place of itself. */
    private static ParticleFactory<DefaultParticleType> effect(Effect lodestone, Effect vanilla) {
        return (type, world, x, y, z, vx, vy, vz) -> {
            (DeeperDarkConfig.useLodestoneParticles() ? lodestone : vanilla).spawn(world, x, y, z, vx, vy, vz);
            return null;
        };
    }

    /** The smoking vase is a Lodestone-only touch; in vanilla mode vases stay still. */
    private static void vanillaVaseAura(ClientWorld world, double x, double y, double z, double angle, double vy, double vz) {
    }

    /** Sculk charge drifting inside the glass, more often the fuller the capacitor is. */
    private static void vanillaCapacitorGlow(ClientWorld world, double x, double y, double z, double fill, double angle, double vz) {
        Random random = world.random;
        if (random.nextFloat() < 0.06f + 0.14f * fill) {
            world.addParticle(new SculkChargeParticleEffect(0.0f),
                    x + (random.nextDouble() - 0.5) * 0.6, y + 0.6, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
        }
    }

    private static void vanillaCapacitorAbsorb(ClientWorld world, double x, double y, double z, double dx, double dy, double dz) {
        Random random = world.random;
        for (int i = 0; i < 6; i++) {
            world.addParticle(ParticleTypes.SCULK_CHARGE_POP, x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6,
                    0.0, 0.03 + random.nextDouble() * 0.03, 0.0);
        }
    }

    private static void vanillaCapacitorPulse(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        int count = 8;
        for (int i = 0; i < count; i++) {
            float angle = i * MathHelper.TAU / count;
            world.addParticle(ParticleTypes.SCULK_CHARGE_POP, x, y + 0.02, z, MathHelper.cos(angle) * 0.06, 0.0, MathHelper.sin(angle) * 0.06);
        }
    }

    private DeeperDarkParticleFactories() {
    }
}
