package com.chanwoo.deeperdark.common.block;

import com.chanwoo.deeperdark.core.DeeperDarkDamageSources;
import com.chanwoo.deeperdark.core.DeeperDarkProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * A sculk mouth. Living things that step into it get stuck and bitten, like evoker fangs snapping shut.
 * Sneaking past it is safe.
 */
public class SculkOrificeBlock extends Block {
    public static final BooleanProperty BITING = DeeperDarkProperties.BITING;

    private static final int BITE_COOLDOWN = 20;
    private static final float BITE_DAMAGE = 4.0f;
    private static final Vec3d STUCK_MOVEMENT = new Vec3d(0.25, 0.1, 0.25);

    private static final VoxelShape RAYCAST_SHAPE = Block.createCuboidShape(1.0, 4.0, 1.0, 15.0, 16.0, 15.0);
    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.combineAndSimplify(
            VoxelShapes.fullCube(),
            VoxelShapes.union(Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 3.0, 15.0), RAYCAST_SHAPE),
            BooleanBiFunction.ONLY_FIRST);

    public SculkOrificeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(BITING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(BITING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return RAYCAST_SHAPE;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || !entity.getBlockPos().equals(pos)) {
            return;
        }

        entity.slowMovement(state, STUCK_MOVEMENT);

        if (world.isClient) {
            Random random = world.getRandom();
            boolean moving = entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ();
            if (moving && random.nextInt(3) == 0) {
                world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                        entity.getX(), pos.getY() + 1, entity.getZ(),
                        MathHelper.nextBetween(random, -1.0f, 1.0f) * 0.083333336f, 0.05f,
                        MathHelper.nextBetween(random, -1.0f, 1.0f) * 0.083333336f);
            }
        } else if (!state.get(BITING) && canBite(living)) {
            this.bite(state, (ServerWorld) world, pos, living);
        }
    }

    private static boolean canBite(LivingEntity entity) {
        if (entity.isSpectator() || entity.bypassesSteppingEffects() || entity.getType() == EntityType.WARDEN) {
            return false;
        }
        return !(entity instanceof PlayerEntity player) || !player.getAbilities().creativeMode;
    }

    private void bite(BlockState state, ServerWorld world, BlockPos pos, LivingEntity entity) {
        world.setBlockState(pos, state.with(BITING, true), Block.NOTIFY_ALL);
        world.createAndScheduleBlockTick(pos, this, BITE_COOLDOWN);

        world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.BLOCKS, 1.0f, 0.85f + world.random.nextFloat() * 0.3f);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.02);
        entity.damage(DeeperDarkDamageSources.SCULK_ORIFICE, BITE_DAMAGE);
        world.emitGameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(BITING)) {
            world.setBlockState(pos, state.with(BITING, false), Block.NOTIFY_ALL);
        }
    }
}
