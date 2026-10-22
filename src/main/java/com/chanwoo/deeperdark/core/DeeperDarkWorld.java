package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.worldgen.DeeperDarkFeatures;
import com.chanwoo.deeperdark.common.worldgen.feature.AncientVaseFeature;
import com.chanwoo.deeperdark.common.worldgen.feature.VaseClusterFeatureConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placementmodifier.*;

import java.util.List;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkWorld {
    private static final int CITY_VASE_ATTEMPTS_PER_CHUNK = 10;
    private static final int CAVE_VASE_ATTEMPTS_PER_CHUNK = 4;
    private static final int ORIFICE_ATTEMPTS_PER_CHUNK = 6;
    private static final int FLOOR_SCAN_STEPS = 12;

    /** Air, or the sculk veins that tend to cover deep dark floors. */
    private static final BlockPredicate OPEN_SPACE = BlockPredicate.anyOf(BlockPredicate.IS_AIR, BlockPredicate.matchingBlocks(List.of(Blocks.SCULK_VEIN)));

    // Ancient cities sit between roughly y=-52 and y=-15. Their floors are the only deepslate bricks and tiles
    // in the deep dark, so these clusters stay inside the cities.
    public static final RegistryEntry<PlacedFeature> CITY_VASES = registerVases("city_vases",
            new VaseClusterFeatureConfig(UniformIntProvider.create(1, 3), 4),
            CITY_VASE_ATTEMPTS_PER_CHUNK, YOffset.fixed(-56), YOffset.fixed(-12), AncientVaseFeature.CITY_FLOOR_BLOCKS);

    // Smaller, rarer groups left on the bare deepslate of the caves around them.
    public static final RegistryEntry<PlacedFeature> CAVE_VASES = registerVases("cave_vases",
            new VaseClusterFeatureConfig(UniformIntProvider.create(1, 3), 5),
            CAVE_VASE_ATTEMPTS_PER_CHUNK, YOffset.aboveBottom(8), YOffset.fixed(0), AncientVaseFeature.CAVE_FLOOR_BLOCKS);

    public static final RegistryEntry<PlacedFeature> SCULK_ORIFICE = PlacedFeatures.register(MOD_ID + ":sculk_orifice",
            ConfiguredFeatures.register(MOD_ID + ":sculk_orifice", DeeperDarkFeatures.SCULK_ORIFICE),
            List.of(
                    CountPlacementModifier.of(ORIFICE_ATTEMPTS_PER_CHUNK),
                    SquarePlacementModifier.of(),
                    HeightRangePlacementModifier.uniform(YOffset.aboveBottom(4), YOffset.fixed(0)),
                    EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.matchingBlocks(List.of(Blocks.SCULK)), OPEN_SPACE, FLOOR_SCAN_STEPS),
                    BiomePlacementModifier.of()));

    private static RegistryEntry<PlacedFeature> registerVases(String name, VaseClusterFeatureConfig config, int attempts,
                                                              YOffset minY, YOffset maxY, List<Block> floors) {
        String id = MOD_ID + ":" + name;
        return PlacedFeatures.register(id,
                ConfiguredFeatures.register(id, DeeperDarkFeatures.ANCIENT_VASE, config),
                List.of(
                        CountPlacementModifier.of(attempts),
                        SquarePlacementModifier.of(),
                        HeightRangePlacementModifier.uniform(minY, maxY),
                        EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.matchingBlocks(floors), OPEN_SPACE, FLOOR_SCAN_STEPS),
                        RandomOffsetPlacementModifier.vertically(ConstantIntProvider.create(1)),
                        BiomePlacementModifier.of()));
    }

    public static void init() {
        // Vegetal decoration runs after the ancient city (underground structures) and the deep dark's sculk patches.
        for (RegistryEntry<PlacedFeature> feature : List.of(CITY_VASES, CAVE_VASES, SCULK_ORIFICE)) {
            BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.DEEP_DARK),
                    GenerationStep.Feature.VEGETAL_DECORATION, feature.getKey().orElseThrow());
        }
    }

    private DeeperDarkWorld() {
    }
}
