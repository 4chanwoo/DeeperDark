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
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * A vase that is one or more blocks tall. Part 0 is the base. It is the only part that drops loot and, on natural
 * vases, owns a block entity that listens for other vases screaming.
 * <p>
 * Breaking a natural vase, even with silk touch, makes it scream. Natural vases nearby receive a vibration and
 * scream in turn, spreading the alarm up to 16 blocks from the vase that broke. Sculk sensors and shriekers hear the
 * screams too. Placed vases are decoration only: no smoke, no screaming, and they ignore other vases.
 */
public abstract class AncientVaseBlock extends BlockWithEntity implements Waterloggable {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty GENERATED = BooleanProperty.of("generated");
    public static final BooleanProperty RINGING = DeeperDarkProperties.RINGING;

    public static final int RING_DURATION = 60;
    private static final int REMOVE_PART_FLAGS = Block.NOTIFY_ALL | Block.FORCE_STATE | Block.SKIP_DROPS;

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

    /**
     * Whether a block may be placed directly above this part. Vases whose rim stops short of the top of their block
     * would leave anything placed on them floating, so the space above them stays empty.
     */
    public boolean canHoldBlockAbove(BlockState state) {
        return this.getPart(state) < this.getHeight() - 1 || this.getMouthHeight() % 1.0 == 0.0;
    }

    public BlockPos getBasePos(BlockState state, BlockPos pos) {
        return pos.down(this.getPart(state));
    }

    public Vec3d getMouthPos(BlockPos basePos) {
        return Vec3d.ofBottomCenter(basePos).add(0.0, this.getMouthHeight(), 0.0);
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

        // Catches parts lost some other way, like explosions or commands.
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
                ItemStack tool = player.getMainHandStack();
                // Natural vases always scream, even when lifted off with silk touch. Placed ones are just decoration.
                if (baseState.get(GENERATED)) {
                    this.scream(serverWorld, basePos, baseState, player, AncientVaseBlockEntity.Alarm.start(serverWorld, this.getMouthPos(basePos)));
                }

                // Only the base drops anything, so break it with the player's tool to keep silk touch working.
                if (!basePos.equals(pos) && !player.isCreative()) {
                    Block.dropStacks(baseState, world, basePos, world.getBlockEntity(basePos), player, tool);
                }
                this.removeParts(serverWorld, basePos, pos);
            }
        }
        if (state.get(GENERATED)) {
            // The scream already carries the vibration to every sensor and shrieker around, so vanilla's block
            // destroy vibration would only send them a second one.
            this.spawnBreakParticles(world, player, pos, state);
        } else {
            super.onBreak(world, pos, state, player);
        }
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (!(world instanceof ServerWorld serverWorld) || projectile instanceof FishingBobberEntity) {
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
        if (!baseState.isOf(this)) {
            return;
        }

        // Vases are fragile: anything thrown at one shatters it.
        if (baseState.get(GENERATED)) {
            this.scream(serverWorld, basePos, baseState, owner, AncientVaseBlockEntity.Alarm.start(serverWorld, this.getMouthPos(basePos)));
        }
        Block.dropStacks(baseState, world, basePos, world.getBlockEntity(basePos), owner, ItemStack.EMPTY);
        world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(state));
        this.removeParts(serverWorld, basePos, pos);
        world.setBlockState(pos, emptyState(state), REMOVE_PART_FLAGS);
        if (!baseState.get(GENERATED)) {
            world.emitGameEvent(owner, GameEvent.BLOCK_DESTROY, pos);
        }
    }

    /** Natural vases lost some other way, like explosions or commands, still scream on the way out. */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld && this.getPart(state) == 0
                && state.get(GENERATED) && world.getBlockEntity(pos) instanceof AncientVaseBlockEntity vase
                && !vase.screamedThisTick(world)) {
            Vec3d mouth = this.getMouthPos(pos);
            AncientVaseBlockEntity.Alarm alarm = AncientVaseBlockEntity.Alarm.start(serverWorld, mouth);
            this.cry(serverWorld, mouth, alarm);
            this.raiseAlarm(serverWorld, mouth, null, vase, alarm);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    /**
     * Removes every part of the vase except {@code keep} at once, so the shatter sound plays once rather than
     * once per part. The part being broken by the player is left for vanilla to remove.
     */
    private void removeParts(ServerWorld world, BlockPos basePos, BlockPos keep) {
        for (int part = 0; part < this.getHeight(); part++) {
            BlockPos partPos = basePos.up(part);
            BlockState partState = world.getBlockState(partPos);
            if (partPos.equals(keep) || !this.isPart(partState, part)) {
                continue;
            }
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, partState),
                    partPos.getX() + 0.5, partPos.getY() + 0.5, partPos.getZ() + 0.5, 20, 0.25, 0.25, 0.25, 0.05);
            world.setBlockState(partPos, emptyState(partState), REMOVE_PART_FLAGS);
        }
    }

    /**
     * Makes the vase at {@code basePos} shriek and alerts every vase, sculk sensor and shrieker in range.
     *
     * @param source the entity responsible, passed along the chain so shriekers know who to warn
     * @param alarm  the alarm this scream belongs to; each vase answers an alarm only once
     */
    public void scream(ServerWorld world, BlockPos basePos, BlockState baseState, @Nullable Entity source, AncientVaseBlockEntity.Alarm alarm) {
        Vec3d mouth = this.getMouthPos(basePos);
        this.cry(world, mouth, alarm);

        world.setBlockState(basePos, baseState.with(RINGING, true), Block.NOTIFY_LISTENERS);
        world.createAndScheduleBlockTick(basePos, this, RING_DURATION);

        AncientVaseBlockEntity self = world.getBlockEntity(basePos) instanceof AncientVaseBlockEntity vase ? vase : null;
        this.raiseAlarm(world, mouth, source, self, alarm);
    }

    /** The shriek and ring. Vases further down the chain shriek a little quieter, so the alarm fades as it spreads. */
    private void cry(ServerWorld world, Vec3d mouth, AncientVaseBlockEntity.Alarm alarm) {
        float volume = 1.0f - 0.15f * alarm.hop();
        world.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.BLOCKS, volume, 0.9f + world.random.nextFloat() * 0.2f);
        spawnForEveryone(world, DeeperDarkParticles.VASE_BREAK_PARTICLE, mouth);
    }

    /**
     * Sends a particle to every player in the world. Normal server particles only reach players within 32 blocks of
     * where they start, which misses part of a scream ring that spans 28 blocks.
     */
    public static void spawnForEveryone(ServerWorld world, ParticleEffect particle, Vec3d pos) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            world.spawnParticles(player, particle, true, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Emits the scream from {@code origin}. {@code self} joins the wave first so it doesn't hear its own scream. */
    private void raiseAlarm(ServerWorld world, Vec3d origin, @Nullable Entity source, @Nullable AncientVaseBlockEntity self, AncientVaseBlockEntity.Alarm alarm) {
        if (self != null) {
            self.joinAlarm(world, alarm);
        }
        AncientVaseBlockEntity.emitScream(world, origin, GameEvent.Emitter.of(source, this.getDefaultState()), alarm);
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
        // Only natural vases listen, scream and smoke; placed ones are plain decoration.
        return this.getPart(state) == 0 && state.get(GENERATED) ? new AncientVaseBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, DeeperDarkBlockEntities.ANCIENT_VASE,
                world.isClient ? AncientVaseBlockEntity::clientTick : AncientVaseBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof AncientVaseBlockEntity vase ? vase : null;
    }
}
