package com.chanwoo.deeperdark.common.block;

import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import com.chanwoo.deeperdark.common.item.SculkCapacitorBlockItem;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

/**
 * Stores experience from nearby deaths as sculk charges. Sitting on a sculk catalyst, it takes the catalyst's
 * experience instead of letting it spread, and releases it into the catalyst while powered (for example by a lever).
 * <p>
 * Only silk touch picks it up intact. Breaking it any other way bursts its stored charge out as sculk.
 */
public class SculkCapacitorBlock extends BlockWithEntity {
    public static final BooleanProperty POWERED = Properties.POWERED;
    /** The visible sculk fill inside the glass. Follows the stored charges. */
    public static final IntProperty CHARGE_LEVEL = DeeperDarkProperties.CHARGE_LEVEL;
    public static final int MAX_LEVEL = 4;

    public SculkCapacitorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false).with(CHARGE_LEVEL, 0));
    }

    /** Fill level shown for a charge count: empty only at 0, full only when completely charged. */
    public static int levelFor(int charges) {
        if (charges <= 0) {
            return 0;
        }
        return MathHelper.clamp(MathHelper.ceil((float) MAX_LEVEL * charges / SculkCapacitorBlockEntity.MAX_CHARGES), 1, MAX_LEVEL);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED, CHARGE_LEVEL);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()))
                .with(CHARGE_LEVEL, levelFor(SculkCapacitorBlockItem.getCharges(ctx.getStack())));
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
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        boolean silkTouch = EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, player.getMainHandStack()) > 0;
        if ((silkTouch || player.isCreative()) && world.getBlockEntity(pos) instanceof SculkCapacitorBlockEntity capacitor) {
            capacitor.keepCharges();
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof SculkCapacitorBlockEntity capacitor) {
            capacitor.releaseCharges(serverWorld);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
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
        return checkType(type, DeeperDarkBlockEntities.SCULK_CAPACITOR,
                world.isClient ? SculkCapacitorBlockEntity::clientTick : SculkCapacitorBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof SculkCapacitorBlockEntity capacitor ? capacitor : null;
    }
}
