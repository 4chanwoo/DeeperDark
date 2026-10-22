package com.chanwoo.deeperdark.common.worldgen.feature;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/**
 * Stands a random vase on the floor at the origin. The placed feature scans down onto ancient city
 * floor blocks, so the origin is the space just above the floor.
 */
public class AncientVaseFeature extends Feature<DefaultFeatureConfig> {
    public static final List<Block> FLOOR_BLOCKS = List.of(
            Blocks.DEEPSLATE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_TILES,
            Blocks.CRACKED_DEEPSLATE_TILES,
            Blocks.POLISHED_DEEPSLATE);

    /** Minimum open sides around the vase, so they end up on floors rather than tucked into walls. */
    private static final int MIN_OPEN_SIDES = 2;

    public AncientVaseFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        BlockPos floorPos = origin.down();
        BlockState floor = world.getBlockState(floorPos);
        if (!FLOOR_BLOCKS.contains(floor.getBlock()) || !floor.isSideSolidFullSquare(world, floorPos, Direction.UP)) {
            return false;
        }

        int openSides = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (isFree(world.getBlockState(origin.offset(direction)))) {
                openSides++;
            }
        }
        if (openSides < MIN_OPEN_SIDES) {
            return false;
        }

        AncientVaseBlock vase = pickVase(random);
        for (int part = 0; part < vase.getHeight(); part++) {
            if (!isFree(world.getBlockState(origin.up(part)))) {
                return false;
            }
        }

        BlockState base = vase.getDefaultState()
                .with(AncientVaseBlock.GENERATED, true)
                .with(AncientVaseBlock.FACING, Direction.Type.HORIZONTAL.random(random));
        for (int part = 0; part < vase.getHeight(); part++) {
            world.setBlockState(origin.up(part), vase.withPart(base, part), Block.NOTIFY_LISTENERS);
        }
        return true;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.isOf(Blocks.SCULK_VEIN);
    }

    private static AncientVaseBlock pickVase(Random random) {
        return switch (random.nextInt(4)) {
            case 0, 1 -> DeeperDarkBlocks.ROTUND_SCREAMING_VASE;
            case 2 -> DeeperDarkBlocks.TURGID_SCREAMING_VASE;
            default -> DeeperDarkBlocks.GAUNT_SCREAMING_VASE;
        };
    }
}
