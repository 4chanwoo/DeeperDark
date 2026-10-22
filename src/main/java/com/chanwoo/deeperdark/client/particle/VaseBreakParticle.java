package com.chanwoo.deeperdark.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.util.function.Consumer;

/**
 * A flat ring that bursts out of a vase's mouth, drawn facing both up and down like the shrieker's ring.
 */
@Environment(EnvType.CLIENT)
public class VaseBreakParticle extends SpriteBillboardParticle {
    private static final Vec3f ROTATION_AXIS = Util.make(new Vec3f(0.5f, 0.5f, 0.5f), Vec3f::normalize);
    private static final float TILT = 1.56f;

    private final SpriteProvider sprites;

    protected VaseBreakParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.scale = 14.0f;
        this.maxAge = 25;
        this.gravityStrength = 0.0f;
        this.velocityMultiplier = 0.0f;
        this.setVelocity(0.0, 0.0, 0.0);
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteForAge(this.sprites);
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * MathHelper.clamp((this.age + tickDelta) / this.maxAge, 0.0f, 1.0f);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        this.alpha = 1.0f - MathHelper.clamp((this.age + tickDelta) / this.maxAge, 0.0f, 1.0f);
        this.buildQuad(vertexConsumer, camera, tickDelta, quaternion -> quaternion.hamiltonProduct(Vec3f.POSITIVE_X.getRadialQuaternion(-TILT)));
        this.buildQuad(vertexConsumer, camera, tickDelta, quaternion -> {
            quaternion.hamiltonProduct(Vec3f.POSITIVE_Y.getRadialQuaternion(-MathHelper.PI));
            quaternion.hamiltonProduct(Vec3f.POSITIVE_X.getRadialQuaternion(TILT));
        });
    }

    private void buildQuad(VertexConsumer vertexConsumer, Camera camera, float tickDelta, Consumer<Quaternion> rotator) {
        Vec3d cameraPos = camera.getPos();
        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        Quaternion rotation = new Quaternion(ROTATION_AXIS, 0.0f, true);
        rotator.accept(rotation);

        Vec3f[] corners = {new Vec3f(-1, -1, 0), new Vec3f(-1, 1, 0), new Vec3f(1, 1, 0), new Vec3f(1, -1, 0)};
        float size = this.getSize(tickDelta);
        for (Vec3f corner : corners) {
            corner.rotate(rotation);
            corner.scale(size);
            corner.add(x, y, z);
        }

        int light = this.getBrightness(tickDelta);
        this.vertex(vertexConsumer, corners[0], this.getMaxU(), this.getMaxV(), light);
        this.vertex(vertexConsumer, corners[1], this.getMaxU(), this.getMinV(), light);
        this.vertex(vertexConsumer, corners[2], this.getMinU(), this.getMinV(), light);
        this.vertex(vertexConsumer, corners[3], this.getMinU(), this.getMaxV(), light);
    }

    private void vertex(VertexConsumer vertexConsumer, Vec3f pos, float u, float v, int light) {
        vertexConsumer.vertex(pos.getX(), pos.getY(), pos.getZ()).texture(u, v).color(this.red, this.green, this.blue, this.alpha).light(light).next();
    }

    @Override
    public int getBrightness(float tint) {
        return 240;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new VaseBreakParticle(world, x, y, z, this.sprites);
        }
    }
}
