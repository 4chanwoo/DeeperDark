package com.chanwoo.deeperdark.common.worldgen;

import com.chanwoo.deeperdark.common.worldgen.feature.AncientVaseFeature;
import com.chanwoo.deeperdark.common.worldgen.feature.SculkOrificeFeature;
import com.chanwoo.deeperdark.common.worldgen.feature.VaseClusterFeatureConfig;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkFeatures {
    public static final Feature<VaseClusterFeatureConfig> ANCIENT_VASE = register("ancient_vase", new AncientVaseFeature(VaseClusterFeatureConfig.CODEC));
    public static final Feature<DefaultFeatureConfig> SCULK_ORIFICE = register("sculk_orifice", new SculkOrificeFeature(DefaultFeatureConfig.CODEC));

    private static <F extends Feature<?>> F register(String name, F feature) {
        return Registry.register(Registry.FEATURE, new Identifier(MOD_ID, name), feature);
    }

    public static void init() {
    }

    private DeeperDarkFeatures() {
    }
}
