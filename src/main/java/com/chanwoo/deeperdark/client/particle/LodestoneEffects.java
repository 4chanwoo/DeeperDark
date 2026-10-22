package com.chanwoo.deeperdark.client.particle;

import com.chanwoo.deeperdark.client.ClientScheduler;
import com.sammy.lodestone.setup.LodestoneParticles;
import com.sammy.lodestone.systems.rendering.particle.Easing;
import com.sammy.lodestone.systems.rendering.particle.ParticleBuilders;
import com.sammy.lodestone.systems.rendering.particle.ParticleTextureSheets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Glowing LodestoneLib versions of Deeper Dark's particles. This is the only class that touches LodestoneLib,
 * and it's only loaded once {@link com.chanwoo.deeperdark.client.DeeperDarkConfig#useLodestoneParticles()} is true.
 * <p>
 * Builders are made fresh for each particle because a builder's {@code spawn} adds onto its stored motion.
 */
@Environment(EnvType.CLIENT)
public final class LodestoneEffects {
    private static final float[] SOUL = {0.72f, 1.0f, 1.0f};
    private static final float[] CYAN = {0.16f, 0.87f, 0.92f};
    private static final float[] TEAL = {0.04f, 0.36f, 0.40f};
    private static final float[] ABYSS = {0.01f, 0.08f, 0.10f};

    private static final int ERUPTION_TICKS = 8;
    /** The vanilla sonic boom ring reaches about this radius, so the Lodestone scream matches it. */
    private static final float SCREAM_RADIUS = 13.0f;
    private static final int WAVE_LIFETIME = 25;
    /** Waves slow to this fraction of their starting speed, so they burst out fast and settle. */
    private static final float WAVE_SLOWDOWN = 0.3f;

    private static final int STREAM_TICKS = 10;
    private static final int STREAM_LIFETIME = 16;

    private static ParticleBuilders.WorldParticleBuilder glow(ParticleType<?> type, float[] from, float[] to) {
        return ParticleBuilders.create(type)
                .setColor(from[0], from[1], from[2], to[0], to[1], to[2])
                .setColorEasing(Easing.QUAD_OUT)
                .enableNoClip();
    }

    private static float spread(Random random, float amount) {
        return (random.nextFloat() - 0.5f) * 2.0f * amount;
    }

    /**
     * A puff that starts moving around the helix at {@code angle} and curls a further {@code twist} radians
     * around it as it rises. Spawning these at an angle that turns over time braids them into a twisting column.
     */
    private static Vec3f[] curl(double angle, double twist, float speed, float rise, float endRise) {
        Vec3f start = new Vec3f((float) -Math.sin(angle) * speed, rise, (float) Math.cos(angle) * speed);
        double end = angle + twist;
        Vec3f finish = new Vec3f((float) -Math.sin(end) * speed * 0.6f, endRise, (float) Math.cos(end) * speed * 0.6f);
        return new Vec3f[]{start, finish};
    }

    /** Starting speed that, slowing linearly to {@link #WAVE_SLOWDOWN} of itself, covers {@code radius} in {@code lifetime} ticks. */
    private static float speedFor(float radius, int lifetime) {
        return 2.0f * radius / ((1.0f + WAVE_SLOWDOWN) * lifetime);
    }

    /**
     * The scream: one crisp ring of light, as wide as the vanilla sonic boom ring, racing out from the mouth like a
     * sound wave, with a brief flash and a few souls slipping free.
     */
    public static void vaseBreak(ClientWorld world, double x, double y, double z) {
        glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                .setScale(1.6f, 0.0f).setAlpha(0.9f, 0.0f).setLifetime(8)
                .setScaleEasing(Easing.QUAD_OUT)
                .spawn(world, x, y, z);

        // Dense and thin, so it reads as one clean line rather than a ring of dots.
        ring(world, x, y, z, 220, SCREAM_RADIUS, 0.32f, WAVE_LIFETIME);

        double spin = world.random.nextDouble() * MathHelper.TAU;
        for (int tick = 0; tick < ERUPTION_TICKS; tick++) {
            int step = tick;
            ClientScheduler.schedule(world, tick, w -> eruption(w, x, y, z, spin + step * 0.9, step));
        }
    }

    /** One tick of the soul eruption: a couple of souls on a turning helix, curling upwards as they rise. */
    private static void eruption(ClientWorld world, double x, double y, double z, double angle, int step) {
        Random random = world.random;
        float fade = 1.0f - (float) step / ERUPTION_TICKS * 0.5f;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * MathHelper.PI;
            Vec3f[] motion = curl(a, MathHelper.PI * 1.2, 0.08f, 0.18f, 0.06f);
            glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                    .setScale(0.22f * fade, 0.0f).setAlpha(fade, 0.0f).setLifetime(35 + random.nextInt(10))
                    .setScaleEasing(Easing.SINE_IN)
                    .setForcedMotion(motion[0], motion[1])
                    .spawn(world, x + Math.cos(a) * 0.15, y, z + Math.sin(a) * 0.15);
        }
    }

    /** A flat ring of light expanding to {@code radius} blocks over {@code lifetime} ticks. */
    private static void ring(World world, double x, double y, double z, int count, float radius, float scale, int lifetime) {
        Random random = world.random;
        float speed = speedFor(radius, lifetime);
        double offset = random.nextDouble() * MathHelper.TAU;
        for (int i = 0; i < count; i++) {
            double angle = offset + i * MathHelper.TAU / count;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            glow(LodestoneParticles.WISP_PARTICLE, CYAN, TEAL)
                    .setScale(scale, scale * 0.6f).setAlpha(0.9f, 0.0f).setLifetime(lifetime)
                    .setForcedMotion(new Vec3f(cos * speed, 0.0f, sin * speed),
                            new Vec3f(cos * speed * WAVE_SLOWDOWN, 0.0f, sin * speed * WAVE_SLOWDOWN))
                    .spawn(world, x, y, z);
        }
    }

    private static final int PLUME_LIFETIME = 80;
    /** Blocks per tick. Slow, so the smoke lingers and the braid stays readable. */
    private static final float PLUME_RISE = 0.022f;
    /** How far either side of the middle the strands swing, along the vase's left-right axis. */
    private static final float PLUME_WIDTH = 0.2f;
    /** How far the strands swing towards and away from the viewer, so they pass in front of and behind each other. */
    private static final float PLUME_DEPTH = 0.09f;
    /** Ticks for one full swing. With the rise speed this sets the wave's length, about one block. */
    private static final double PLUME_PERIOD = 48.0;

    /** How high above the mouth the plume reaches, and so where its sparkles can appear. */
    private static final float PLUME_HEIGHT = PLUME_RISE * PLUME_LIFETIME;

    /**
     * Two slim strands of glowing sculk smoke braided around each other as they rise out of the vase. Each puff rises
     * straight up while the point it leaves the mouth from slowly circles, so every strand traces a gentle wave. The
     * two strands circle half a turn apart: seen from the front they cross in and out of each other, passing in front
     * and behind in turn. The swing slowly breathes wider and narrower. Tiny sparkles drift up through the smoke.
     *
     * @param axis the vase's facing, in radians; the strands swing across it
     * @param time a steadily increasing tick count, which drives the swing
     */
    public static void vaseAura(World world, double x, double y, double z, double axis, double time) {
        Random random = world.random;
        double phase = (time % PLUME_PERIOD) / PLUME_PERIOD * MathHelper.TAU;
        float breath = 0.85f + 0.15f * (float) Math.sin((time % 160.0) / 160.0 * MathHelper.TAU);
        // Left-right relative to the vase's face is perpendicular to the direction it faces.
        float acrossX = (float) Math.cos(axis);
        float acrossZ = (float) Math.sin(axis);

        for (int strand = 0; strand < 2; strand++) {
            double angle = phase + strand * MathHelper.PI;
            float across = (float) Math.cos(angle) * PLUME_WIDTH * breath;
            float depth = (float) Math.sin(angle) * PLUME_DEPTH * breath;
            // Rises a little slower as it goes, so the waves bunch up gently towards the top.
            glow(LodestoneParticles.SMOKE_PARTICLE, CYAN, TEAL)
                    .setScale(0.18f, 0.1f, 0.22f).setAlpha(0.0f, 0.26f, 0.0f).setLifetime(PLUME_LIFETIME)
                    .setSpin(spread(random, 0.02f))
                    .setForcedMotion(new Vec3f(0.0f, PLUME_RISE * 1.15f, 0.0f), new Vec3f(0.0f, PLUME_RISE * 0.85f, 0.0f))
                    .spawn(world, x + acrossX * across - acrossZ * depth, y, z + acrossZ * across + acrossX * depth);
        }

        if (random.nextInt(4) == 0) {
            float across = spread(random, PLUME_WIDTH * 2.0f);
            glow(LodestoneParticles.SPARKLE_PARTICLE, SOUL, CYAN)
                    .setScale(0.07f, 0.0f).setAlpha(0.9f, 0.0f).setLifetime(50 + random.nextInt(30))
                    .setForcedMotion(new Vec3f(spread(random, 0.003f), 0.008f, spread(random, 0.003f)),
                            new Vec3f(0.0f, 0.003f, 0.0f))
                    .spawn(world, x + acrossX * across, y + random.nextFloat() * PLUME_HEIGHT, z + acrossZ * across);
        }
    }

    /** Lodestone's additive blending, which makes the vanilla scream ring glow. */
    public static ParticleTextureSheet additiveSheet() {
        return ParticleTextureSheets.ADDITIVE;
    }

    /**
     * Charge swirling inside the capacitor's glass: motes orbiting at the height of the sculk fill and a core
     * that glows brighter the fuller it is.
     *
     * @param x     the centre of the capacitor's bottom face
     * @param fill  how full the capacitor is, 0 to 1
     * @param angle where the orbiting motes are this tick; it turns steadily over time
     */
    public static void capacitorGlow(World world, double x, double y, double z, double fill, double angle) {
        Random random = world.random;
        float level = (float) MathHelper.clamp(fill, 0.0, 1.0);
        // The sculk fill rises from 7 px up to 15 px inside the glass.
        double surface = y + (7.0 + 8.0 * level) / 16.0;

        for (int i = 0; i < 2; i++) {
            double a = angle + i * MathHelper.PI;
            Vec3f[] motion = curl(a, 1.2, 0.025f, 0.004f, 0.012f);
            glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                    .setScale(0.1f + 0.06f * level, 0.0f).setAlpha(0.9f, 0.0f).setLifetime(18)
                    .setForcedMotion(motion[0], motion[1])
                    .spawn(world, x + Math.cos(a) * 0.32, surface - random.nextDouble() * 0.25, z + Math.sin(a) * 0.32);
        }

        float pulse = 0.65f + 0.35f * MathHelper.sin((float) angle * 0.6f);
        glow(LodestoneParticles.WISP_PARTICLE, CYAN, TEAL)
                .setScale(0.35f + 0.45f * level, 0.0f).setAlpha(0.25f * pulse * (0.4f + level), 0.0f).setLifetime(8)
                .spawn(world, x, y + 0.45 + 0.25 * level, z);

        if (random.nextInt(Math.max(2, 10 - (int) (level * 8))) == 0) {
            glow(LodestoneParticles.SPARKLE_PARTICLE, SOUL, CYAN)
                    .setScale(0.12f, 0.0f).setAlpha(1.0f, 0.0f).setLifetime(25 + random.nextInt(10))
                    .setSpin(spread(random, 0.2f))
                    .setMotion(spread(random, 0.01f), 0.02 + random.nextDouble() * 0.02, spread(random, 0.01f))
                    .spawn(world, x + spread(random, 0.3f), y + 1.0, z + spread(random, 0.3f));
        }
    }

    /**
     * The souls of a slain mob streaming into the capacitor along bowed arcs, then flaring as they arrive.
     *
     * @param dx offset from the capacitor to where the mob died (likewise {@code dy}, {@code dz})
     */
    public static void capacitorAbsorb(ClientWorld world, double x, double y, double z, double dx, double dy, double dz) {
        for (int tick = 0; tick < STREAM_TICKS; tick++) {
            ClientScheduler.schedule(world, tick, w -> {
                Random random = w.random;
                for (int i = 0; i < 3; i++) {
                    double sx = dx + spread(random, 0.3f);
                    double sy = dy + spread(random, 0.3f);
                    double sz = dz + spread(random, 0.3f);
                    // Straight at the capacitor on average, bowed out sideways at the start and back in at the end.
                    float vx = (float) (-sx / STREAM_LIFETIME);
                    float vy = (float) (-sy / STREAM_LIFETIME);
                    float vz = (float) (-sz / STREAM_LIFETIME);
                    float bow = spread(random, 1.2f);
                    glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                            .setScale(0.22f, 0.05f).setAlpha(0.95f, 0.2f).setLifetime(STREAM_LIFETIME)
                            .setForcedMotion(new Vec3f(vx - vz * bow, vy + Math.abs(bow) * 0.05f, vz + vx * bow),
                                    new Vec3f(vx + vz * bow, vy - Math.abs(bow) * 0.05f, vz - vx * bow))
                            .spawn(w, x + sx, y + sy, z + sz);
                }
            });
        }

        ClientScheduler.schedule(world, STREAM_LIFETIME, w -> {
            Random random = w.random;
            glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                    .setScale(1.2f, 0.0f).setAlpha(1.0f, 0.0f).setLifetime(12)
                    .spawn(w, x, y, z);
            for (int i = 0; i < 18; i++) {
                double angle = i * MathHelper.TAU / 18;
                Vec3f[] motion = curl(angle, MathHelper.PI, 0.07f, 0.09f, 0.0f);
                glow(LodestoneParticles.SPARKLE_PARTICLE, SOUL, CYAN)
                        .setScale(0.18f, 0.0f).setAlpha(1.0f, 0.0f).setLifetime(22 + random.nextInt(12))
                        .setSpin(0.2f)
                        .setForcedMotion(motion[0], motion[1])
                        .spawn(w, x + Math.cos(angle) * 0.4, y, z + Math.sin(angle) * 0.4);
            }
        });
    }

    /** Charge draining down out of the capacitor and rippling across the catalyst it feeds. */
    public static void capacitorPulse(World world, double x, double y, double z) {
        Random random = world.random;
        ring(world, x, y + 0.02, z, 24, 1.4f, 0.25f, 14);
        for (int i = 0; i < 5; i++) {
            glow(LodestoneParticles.WISP_PARTICLE, SOUL, CYAN)
                    .setScale(0.2f, 0.05f).setAlpha(0.9f, 0.0f).setLifetime(10)
                    .setForcedMotion(new Vec3f(0.0f, -0.02f, 0.0f), new Vec3f(0.0f, -0.08f, 0.0f))
                    .spawn(world, x + spread(random, 0.25f), y + 0.9, z + spread(random, 0.25f));
        }
        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * MathHelper.TAU;
            glow(LodestoneParticles.SPARKLE_PARTICLE, CYAN, TEAL)
                    .setScale(0.12f, 0.0f).setAlpha(1.0f, 0.0f).setLifetime(14)
                    .setMotion(Math.cos(angle) * 0.12, 0.02, Math.sin(angle) * 0.12)
                    .spawn(world, x, y + 0.05, z);
        }
    }

    private LodestoneEffects() {
    }
}
