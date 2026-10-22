package com.chanwoo.deeperdark.common.worldgen.feature;

import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Turns the sculk block at the origin into an orifice when it is a floor with solid ground around it.
 */
public class SculkOrificeFeature extends Feature<DefaultFeatureConfig> {
    public SculkOrificeFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();

        if (!world.getBlockState(origin).isOf(Blocks.SCULK)) {
            return false;
        }

        BlockPos abovePos = origin.up();
        BlockState above = world.getBlockState(abovePos);
        if (!above.isAir() && !above.isOf(Blocks.SCULK_VEIN)) {
            return false;
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = origin.offset(direction);
            if (!world.getBlockState(sidePos).isSideSolidFullSquare(world, sidePos, direction.getOpposite())) {
                return false;
            }
        }

        if (above.isOf(Blocks.SCULK_VEIN)) {
            world.setBlockState(abovePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        world.setBlockState(origin, DeeperDarkBlocks.SCULK_ORIFICE.getDefaultState(), Block.NOTIFY_LISTENERS);
        return true;
    }
}
