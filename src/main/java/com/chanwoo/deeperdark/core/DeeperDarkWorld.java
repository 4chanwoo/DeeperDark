package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.worldgen.DeeperDarkFeatures;
import com.chanwoo.deeperdark.common.worldgen.feature.AncientVaseFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
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
    private static final int VASE_ATTEMPTS_PER_CHUNK = 8;
    private static final int ORIFICE_ATTEMPTS_PER_CHUNK = 6;
    private static final int FLOOR_SCAN_STEPS = 12;

    /** Air, or the sculk veins that tend to cover ancient city floors. */
    private static final BlockPredicate OPEN_SPACE = BlockPredicate.anyOf(BlockPredicate.IS_AIR, BlockPredicate.matchingBlocks(List.of(Blocks.SCULK_VEIN)));

    // Ancient cities sit between roughly y=-52 and y=-15, and their floors are the only deepslate bricks
    // and tiles in the deep dark, so scanning down onto those keeps vases inside the cities.
    public static final RegistryEntry<PlacedFeature> ANCIENT_VASE = PlacedFeatures.register(MOD_ID + ":ancient_vase",
            ConfiguredFeatures.register(MOD_ID + ":ancient_vase", DeeperDarkFeatures.ANCIENT_VASE),
            List.of(
                    CountPlacementModifier.of(VASE_ATTEMPTS_PER_CHUNK),
                    SquarePlacementModifier.of(),
                    HeightRangePlacementModifier.uniform(YOffset.fixed(-56), YOffset.fixed(-12)),
                    EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.matchingBlocks(AncientVaseFeature.FLOOR_BLOCKS), OPEN_SPACE, FLOOR_SCAN_STEPS),
                    RandomOffsetPlacementModifier.vertically(ConstantIntProvider.create(1)),
                    BiomePlacementModifier.of()));

    public static final RegistryEntry<PlacedFeature> SCULK_ORIFICE = PlacedFeatures.register(MOD_ID + ":sculk_orifice",
            ConfiguredFeatures.register(MOD_ID + ":sculk_orifice", DeeperDarkFeatures.SCULK_ORIFICE),
            List.of(
                    CountPlacementModifier.of(ORIFICE_ATTEMPTS_PER_CHUNK),
                    SquarePlacementModifier.of(),
                    HeightRangePlacementModifier.uniform(YOffset.aboveBottom(4), YOffset.fixed(0)),
                    EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.matchingBlocks(List.of(Blocks.SCULK)), OPEN_SPACE, FLOOR_SCAN_STEPS),
                    BiomePlacementModifier.of()));

    public static void init() {
        // Vegetal decoration runs after the ancient city (underground structures) and the deep dark's sculk patches.
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.DEEP_DARK),
                GenerationStep.Feature.VEGETAL_DECORATION, ANCIENT_VASE.getKey().orElseThrow());
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.DEEP_DARK),
                GenerationStep.Feature.VEGETAL_DECORATION, SCULK_ORIFICE.getKey().orElseThrow());
    }

    private DeeperDarkWorld() {
    }
}
