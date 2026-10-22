package com.chanwoo.deeperdark.common.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * @param tries  how many vases to try placing around the origin
 * @param spread how far from the origin (horizontally) those vases may land
 */
public record VaseClusterFeatureConfig(IntProvider tries, int spread) implements FeatureConfig {
    public static final Codec<VaseClusterFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.createValidatingCodec(1, 16).fieldOf("tries").forGetter(VaseClusterFeatureConfig::tries),
            Codec.intRange(0, 8).fieldOf("spread").forGetter(VaseClusterFeatureConfig::spread)
    ).apply(instance, VaseClusterFeatureConfig::new));
}
