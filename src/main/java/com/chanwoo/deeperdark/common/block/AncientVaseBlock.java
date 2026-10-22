package com.chanwoo.deeperdark.common.block;

import com.chanwoo.deeperdark.common.entity.AncientVaseBlockEntity;
import com.chanwoo.deeperdark.core.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

/**
 * A vase that is one or more blocks tall. Part 0 is the base and is the only part that owns a block entity,
 * drops loot, and listens for other vases screaming.
 * <p>
 * When a vase is broken it screams: a {@link DeeperDarkGameEvents#VASE_SCREAM} vibration travels to nearby vases
 * (which ring and pass the scream on with one less strength), sculk sensors and sculk shriekers.
 */
public abstract class AncientVaseBlock extends BlockWithEntity implements Waterloggable {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty GENERATED = BooleanProperty.of("generated");
    public static final BooleanProperty RINGING = DeeperDarkProperties.RINGING;

    public static final int RING_DURATION = 60;
    /** How many vases a scream from a shattered vase can hop through. */
    public static final int MAX_SIGNAL_STRENGTH = 3;

    protected AncientVaseBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(GENERATED, false)
                .with(RINGING, false)
                .with(WATERLOGGED, false));
    }

    /** Height of the vase in blocks. */
    public abstract int getHeight();

    /** Part index of this state, counted upwards from 0 (the base). */
    public abstract int getPart(BlockState state);

    public abstract BlockState withPart(BlockState state, int part);

    /** Height of the vase's mouth above the bottom of the base block. */
    public abstract double getMouthHeight();

    public BlockPos getBasePos(BlockState state, BlockPos pos) {
        return pos.down(this.getPart(state));
    }

    private boolean isPart(BlockState state, int part) {
        return state.isOf(this) && this.getPart(state) == part;
    }

    private static BlockState emptyState(BlockState state) {
        return state.get(WATERLOGGED) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, GENERATED, RINGING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = this.getDefaultState()
                .with(FACING, ctx.getPlayerFacing().getOpposite())
                .with(WATERLOGGED, world.getFluidState(pos).isOf(Fluids.WATER));

        for (int part = 1; part < this.getHeight(); part++) {
            BlockPos partPos = pos.up(part);
            if (partPos.getY() >= world.getTopY()
                    || !world.getBlockState(partPos).canReplace(ctx)
                    || !world.canPlace(this.withPart(state, part), partPos, ShapeContext.absent())) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        for (int part = 1; part < this.getHeight(); part++) {
            BlockPos partPos = pos.up(part);
            BlockState partState = this.withPart(state, part).with(WATERLOGGED, world.getFluidState(partPos).isOf(Fluids.WATER));
            world.setBlockState(partPos, partState, Block.NOTIFY_ALL);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.createAndScheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        int part = this.getPart(state);
        boolean missingAbove = direction == Direction.UP && part < this.getHeight() - 1 && !this.isPart(neighborState, part + 1);
        boolean missingBelow = direction == Direction.DOWN && part > 0 && !this.isPart(neighborState, part - 1);
        if (missingAbove || missingBelow) {
            return emptyState(state);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            BlockPos basePos = this.getBasePos(state, pos);
            BlockState baseState = world.getBlockState(basePos);
            if (baseState.isOf(this)) {
                this.scream(serverWorld, basePos, baseState, MAX_SIGNAL_STRENGTH, player);
                this.playShatterSound(world, pos);

                // Only the base drops anything, so break it with the player's tool to keep silk touch working.
                if (!basePos.equals(pos)) {
                    if (!player.isCreative()) {
                        Block.dropStacks(baseState, world, basePos, world.getBlockEntity(basePos), player, player.getMainHandStack());
                    }
                    world.setBlockState(basePos, emptyState(baseState), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                    world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, basePos, Block.getRawIdFromState(baseState));
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (!(world instanceof ServerWorld serverWorld) || !projectile.getType().isIn(DeeperDarkTags.SHATTERS_VASES)) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        Entity owner = projectile.getOwner();
        boolean ownerMayGrief = owner == null || owner instanceof PlayerEntity || world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
        if (!ownerMayGrief || !projectile.canModifyAt(world, pos)) {
            return;
        }

        BlockPos basePos = this.getBasePos(state, pos);
        BlockState baseState = world.getBlockState(basePos);
        if (baseState.isOf(this)) {
            this.scream(serverWorld, basePos, baseState, MAX_SIGNAL_STRENGTH, owner);
            this.playShatterSound(world, pos);
            world.breakBlock(basePos, true, owner);
        }
    }

    /** The ceramic layer of the shatter; the glass layer is the block sound group's break sound. */
    private void playShatterSound(World world, BlockPos pos) {
        world.playSound(null, pos, DeeperDarkSounds.ANCIENT_VASE_CRUMBLE, SoundCategory.BLOCKS, 1.0f, 0.9f + world.random.nextFloat() * 0.2f);
    }

    /**
     * Makes the vase at {@code basePos} shriek and sends a vibration to everything listening nearby.
     *
     * @param strength how many more vases the scream may be passed on to
     * @param source   the entity responsible, passed along so shriekers know who to warn
     */
    public void scream(ServerWorld world, BlockPos basePos, BlockState baseState, int strength, @Nullable Entity source) {
        Vec3d mouth = Vec3d.ofBottomCenter(basePos).add(0.0, this.getMouthHeight(), 0.0);
        world.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.BLOCKS, 1.0f, 0.9f + world.random.nextFloat() * 0.2f);
        world.spawnParticles(DeeperDarkParticles.VASE_BREAK_PARTICLE, mouth.x, mouth.y, mouth.z, 1, 0.0, 0.0, 0.0, 0.0);

        world.setBlockState(basePos, baseState.with(RINGING, true), Block.NOTIFY_LISTENERS);
        world.createAndScheduleBlockTick(basePos, this, RING_DURATION);
        if (world.getBlockEntity(basePos) instanceof AncientVaseBlockEntity vase) {
            vase.setSignalStrength(strength);
        }

        // Emitted from the top part so listeners can find this vase again from the vibration's origin.
        Vec3d origin = Vec3d.ofCenter(basePos.up(this.getHeight() - 1));
        world.emitGameEvent(DeeperDarkGameEvents.VASE_SCREAM, origin, GameEvent.Emitter.of(source, baseState));
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(RINGING)) {
            world.setBlockState(pos, state.with(RINGING, false), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return this.getPart(state) == 0 ? new AncientVaseBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, DeeperDarkBlockEntities.ANCIENT_VASE,
                (tickWorld, pos, tickState, vase) -> vase.getEventListener().tick(tickWorld));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof AncientVaseBlockEntity vase ? vase.getEventListener() : null;
    }
}
