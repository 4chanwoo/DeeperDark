package com.chanwoo.deeperdark.common.block;

import com.chanwoo.deeperdark.common.properties.TripleBlockPart;
import com.chanwoo.deeperdark.core.DeeperDarkProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class GauntScreamingVaseBlock extends AncientVaseBlock {
    public static final EnumProperty<TripleBlockPart> THIRD = DeeperDarkProperties.TRIPLE_BLOCK_HALF;

    private static final VoxelShape BODY_SHAPE = Block.createCuboidShape(3, 0, 3, 13, 16, 13);
    private static final VoxelShape UPPER_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 8, 4, 12, 11, 12),
            Block.createCuboidShape(3, 0, 3, 13, 8, 13));

    public GauntScreamingVaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(THIRD, TripleBlockPart.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(THIRD);
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public int getPart(BlockState state) {
        return switch (state.get(THIRD)) {
            case LOWER -> 0;
            case MIDDLE -> 1;
            case UPPER -> 2;
        };
    }

    @Override
    public BlockState withPart(BlockState state, int part) {
        return state.with(THIRD, switch (part) {
            case 0 -> TripleBlockPart.LOWER;
            case 1 -> TripleBlockPart.MIDDLE;
            default -> TripleBlockPart.UPPER;
        });
    }

    @Override
    public double getMouthHeight() {
        return 2.6875;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(THIRD) == TripleBlockPart.UPPER ? UPPER_SHAPE : BODY_SHAPE;
    }
}
