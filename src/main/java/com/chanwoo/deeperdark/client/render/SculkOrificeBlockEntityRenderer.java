package com.chanwoo.deeperdark.client.render;

import com.chanwoo.deeperdark.common.entity.SculkOrificeBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

/**
 * Draws the orifice's four jaw flaps. Each is hinged on the rim, breathes slowly while idle,
 * and snaps upwards when the orifice bites.
 */
@Environment(EnvType.CLIENT)
public class SculkOrificeBlockEntityRenderer implements BlockEntityRenderer<SculkOrificeBlockEntity> {
    private static final Identifier JAW_TEXTURE = new Identifier(MOD_ID, "block/sculk_orifice_bite");

    private static final float REST_ANGLE = 22.5f;
    private static final float BREATH_ANGLE = 7.0f;
    private static final float BITE_ANGLE = 80.0f;
    private static final float BITE_TICKS = 12.0f;
    /** Fraction of the bite spent snapping shut; the rest is spent easing back open. */
    private static final float SNAP_PORTION = 0.2f;

    private static final float HINGE_Y = 15 / 16f;
    private static final float HINGE_Z = 1 / 16f;
    private static final float JAW_MIN_X = 5 / 16f;
    private static final float JAW_MAX_X = 11 / 16f;
    private static final float JAW_LENGTH = 4 / 16f;

    public SculkOrificeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(SculkOrificeBlockEntity orifice, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = orifice.getWorld();
        if (world == null) {
            return;
        }

        float time = world.getTime() + tickDelta;
        float sinceBite = (world.getTime() - orifice.getLastBiteTime()) + tickDelta;
        // Different orifices breathe out of step with each other.
        float phase = (float) Math.floorMod(orifice.getPos().asLong() * 2654435761L, 628L) / 100.0f;

        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(JAW_TEXTURE);
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));

        for (int side = 0; side < 4; side++) {
            matrices.push();
            matrices.translate(0.5, 0.0, 0.5);
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0f * side));
            matrices.translate(-0.5, HINGE_Y, -0.5 + HINGE_Z);
            // Negative X rotation lifts the jaw's inner edge.
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-getJawAngle(time, sinceBite, phase + side * 1.3f)));
            drawJaw(matrices.peek(), consumer, sprite, light, overlay);
            matrices.pop();
        }
    }

    private static float getJawAngle(float time, float sinceBite, float phase) {
        float idle = REST_ANGLE
                + BREATH_ANGLE * MathHelper.sin(time * 0.07f + phase)
                + 1.5f * MathHelper.sin(time * 0.31f + phase * 2.0f);
        if (sinceBite < 0.0f || sinceBite >= BITE_TICKS) {
            return idle;
        }

        float progress = sinceBite / BITE_TICKS;
        float closed;
        if (progress < SNAP_PORTION) {
            float snap = 1.0f - progress / SNAP_PORTION;
            closed = 1.0f - snap * snap * snap;
        } else {
            float release = (progress - SNAP_PORTION) / (1.0f - SNAP_PORTION);
            closed = 1.0f - release * release * (3.0f - 2.0f * release);
        }
        return MathHelper.lerp(closed, idle, BITE_ANGLE);
    }

    /** One flat jaw lying along +Z from its hinge, fang tips (the top of the texture region) innermost. */
    private static void drawJaw(MatrixStack.Entry entry, VertexConsumer consumer, Sprite sprite, int light, int overlay) {
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();
        float u0 = sprite.getFrameU(5), u1 = sprite.getFrameU(11);
        float vTip = sprite.getFrameV(12), vRoot = sprite.getFrameV(16);

        vertex(position, normal, consumer, JAW_MIN_X, 0.0f, u0, vRoot, light, overlay);
        vertex(position, normal, consumer, JAW_MIN_X, JAW_LENGTH, u0, vTip, light, overlay);
        vertex(position, normal, consumer, JAW_MAX_X, JAW_LENGTH, u1, vTip, light, overlay);
        vertex(position, normal, consumer, JAW_MAX_X, 0.0f, u1, vRoot, light, overlay);
    }

    private static void vertex(Matrix4f position, Matrix3f normal, VertexConsumer consumer, float x, float z, float u, float v, int light, int overlay) {
        consumer.vertex(position, x, 0.0f, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(normal, 0.0f, 1.0f, 0.0f)
                .next();
    }
}
