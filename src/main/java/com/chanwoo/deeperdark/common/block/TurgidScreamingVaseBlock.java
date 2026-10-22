package com.chanwoo.deeperdark.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class TurgidScreamingVaseBlock extends AncientVaseBlock {
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    private static final VoxelShape BOTTOM_SHAPE = VoxelShapes.fullCube();
    private static final VoxelShape TOP_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(3, 8, 3, 13, 10, 13));

    public TurgidScreamingVaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(HALF, BlockHalf.BOTTOM));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HALF);
    }

    @Override
    public int getHeight() {
        return 2;
    }

    @Override
    public int getPart(BlockState state) {
        return state.get(HALF) == BlockHalf.BOTTOM ? 0 : 1;
    }

    @Override
    public BlockState withPart(BlockState state, int part) {
        return state.with(HALF, part == 0 ? BlockHalf.BOTTOM : BlockHalf.TOP);
    }

    @Override
    public double getMouthHeight() {
        return 1.625;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == BlockHalf.BOTTOM ? BOTTOM_SHAPE : TOP_SHAPE;
    }
}
