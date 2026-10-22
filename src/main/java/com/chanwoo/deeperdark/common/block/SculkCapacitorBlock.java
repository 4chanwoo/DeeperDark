package com.chanwoo.deeperdark.common.block;

import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

/**
 * Stores experience from nearby deaths as sculk charges. When powered while sitting on a sculk catalyst,
 * it spends those charges to spread sculk from the catalyst.
 */
public class SculkCapacitorBlock extends BlockWithEntity {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public SculkCapacitorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean powered = world.isReceivingRedstonePower(pos);
            if (powered != state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
            }
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (world.getBlockEntity(pos) instanceof SculkCapacitorBlockEntity capacitor && capacitor.getCharges() > 0) {
            int particles = 1 + capacitor.getCharges() / 50;
            for (int i = 0; i < particles; i++) {
                world.addParticle(new SculkChargeParticleEffect(0.0f),
                        pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.6, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                        0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof SculkCapacitorBlockEntity capacitor ? capacitor.getComparatorOutput() : 0;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SculkCapacitorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, DeeperDarkBlockEntities.SCULK_CAPACITOR, SculkCapacitorBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof SculkCapacitorBlockEntity capacitor ? capacitor : null;
    }
}
