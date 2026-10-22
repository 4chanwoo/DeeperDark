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
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/**
 * Places a small cluster of mixed vases around the origin, each one standing on a deepslate floor
 * with its own random type and facing.
 */
public class AncientVaseFeature extends Feature<VaseClusterFeatureConfig> {
    /** Ancient city floors. */
    public static final List<Block> CITY_FLOOR_BLOCKS = List.of(
            Blocks.DEEPSLATE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_TILES,
            Blocks.CRACKED_DEEPSLATE_TILES,
            Blocks.POLISHED_DEEPSLATE);
    /** Natural deep dark cave floors. */
    public static final List<Block> CAVE_FLOOR_BLOCKS = List.of(
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.TUFF);

    /** How far up or down a cluster member may step to find its own floor. */
    private static final int FLOOR_SEARCH = 2;
    /** Minimum open sides around a vase, so they end up on floors rather than tucked into walls. */
    private static final int MIN_OPEN_SIDES = 2;
    /** Vases keep at least this many blocks between each other, so groups don't clump into clutter. */
    private static final int MIN_SPACING = 3;

    public AncientVaseFeature(Codec<VaseClusterFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<VaseClusterFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        VaseClusterFeatureConfig config = context.getConfig();

        boolean placedAny = tryPlace(world, random, origin);
        int tries = config.tries().get(random) - 1;
        int spread = config.spread();
        for (int i = 0; i < tries; i++) {
            BlockPos target = origin.add(random.nextBetween(-spread, spread), 0, random.nextBetween(-spread, spread));
            BlockPos floor = findFloor(world, target);
            if (floor != null) {
                placedAny |= tryPlace(world, random, floor);
            }
        }
        return placedAny;
    }

    /** Finds the open space just above a floor near {@code pos}, or null if there isn't one. */
    private static BlockPos findFloor(StructureWorldAccess world, BlockPos pos) {
        for (int dy = FLOOR_SEARCH; dy >= -FLOOR_SEARCH; dy--) {
            BlockPos candidate = pos.up(dy);
            if (isFree(world.getBlockState(candidate)) && isFloor(world, candidate.down())) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isFloor(StructureWorldAccess world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return (CITY_FLOOR_BLOCKS.contains(state.getBlock()) || CAVE_FLOOR_BLOCKS.contains(state.getBlock()))
                && state.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    private static boolean tryPlace(StructureWorldAccess world, Random random, BlockPos pos) {
        if (!isFloor(world, pos.down())) {
            return false;
        }

        int openSides = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (isFree(world.getBlockState(pos.offset(direction)))) {
                openSides++;
            }
        }
        if (openSides < MIN_OPEN_SIDES || hasVaseNearby(world, pos)) {
            return false;
        }

        AncientVaseBlock vase = pickVase(random);
        for (int part = 0; part < vase.getHeight(); part++) {
            if (!isFree(world.getBlockState(pos.up(part)))) {
                return false;
            }
        }

        BlockState base = vase.getDefaultState()
                .with(AncientVaseBlock.GENERATED, true)
                .with(AncientVaseBlock.FACING, Direction.Type.HORIZONTAL.random(random));
        for (int part = 0; part < vase.getHeight(); part++) {
            world.setBlockState(pos.up(part), vase.withPart(base, part), Block.NOTIFY_LISTENERS);
        }
        return true;
    }

    private static boolean hasVaseNearby(StructureWorldAccess world, BlockPos pos) {
        for (BlockPos nearby : BlockPos.iterate(pos.add(-MIN_SPACING, -MIN_SPACING, -MIN_SPACING), pos.add(MIN_SPACING, MIN_SPACING, MIN_SPACING))) {
            if (world.getBlockState(nearby).getBlock() instanceof AncientVaseBlock) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.isOf(Blocks.SCULK_VEIN);
    }

    /** Rotund vases are the most common, the tall gaunt ones the rarest. */
    private static AncientVaseBlock pickVase(Random random) {
        int roll = random.nextInt(10);
        if (roll < 5) {
            return DeeperDarkBlocks.ROTUND_SCREAMING_VASE;
        }
        return roll < 8 ? DeeperDarkBlocks.TURGID_SCREAMING_VASE : DeeperDarkBlocks.GAUNT_SCREAMING_VASE;
    }
}
