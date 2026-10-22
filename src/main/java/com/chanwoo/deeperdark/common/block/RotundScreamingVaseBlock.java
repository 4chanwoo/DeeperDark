package com.chanwoo.deeperdark.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class RotundScreamingVaseBlock extends AncientVaseBlock {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2, 0, 2, 14, 12, 14),
            Block.createCuboidShape(4, 12, 4, 12, 16, 12));

    public RotundScreamingVaseBlock(Settings settings) {
        super(settings);
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public int getPart(BlockState state) {
        return 0;
    }

    @Override
    public BlockState withPart(BlockState state, int part) {
        return state;
    }

    @Override
    public double getMouthHeight() {
        return 1.0;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
