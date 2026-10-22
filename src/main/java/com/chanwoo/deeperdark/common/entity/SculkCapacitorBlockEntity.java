package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.SculkCapacitorBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkCatalystBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class SculkCapacitorBlockEntity extends BlockEntity implements GameEventListener {
    public static final String CHARGES_KEY = "Charges";
    public static final int MAX_CHARGES = 512;
    private static final int RANGE = 8;
    private static final int PULSE_INTERVAL = 4;
    private static final int CHARGES_PER_PULSE = 4;
    /** Clients only need a rough charge count for particles, so only sync when it crosses one of these steps. */
    private static final int SYNC_STEP = 50;

    private final SculkSpreadManager spreadManager = SculkSpreadManager.create();
    private final BlockPositionSource positionSource;
    private int charges;

    public SculkCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(DeeperDarkBlockEntities.SCULK_CAPACITOR, pos, state);
        this.positionSource = new BlockPositionSource(pos);
    }

    public int getCharges() {
        return this.charges;
    }

    public int getComparatorOutput() {
        return this.charges == 0 ? 0 : MathHelper.ceil(15.0f * this.charges / MAX_CHARGES);
    }

    private void setCharges(int charges) {
        int clamped = MathHelper.clamp(charges, 0, MAX_CHARGES);
        if (clamped == this.charges) {
            return;
        }
        boolean needsSync = clamped / SYNC_STEP != this.charges / SYNC_STEP || clamped == 0;
        this.charges = clamped;
        this.markDirty();
        if (this.world != null) {
            this.world.updateComparators(this.pos, this.getCachedState().getBlock());
            if (needsSync) {
                this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, SculkCapacitorBlockEntity capacitor) {
        capacitor.spreadManager.tick(world, pos, world.getRandom(), true);

        if (state.get(SculkCapacitorBlock.POWERED) && capacitor.charges > 0 && world.getTime() % PULSE_INTERVAL == 0) {
            BlockPos catalystPos = pos.down();
            BlockState catalyst = world.getBlockState(catalystPos);
            if (catalyst.isOf(Blocks.SCULK_CATALYST)) {
                capacitor.discharge((ServerWorld) world, catalystPos, catalyst);
            }
        }
    }

    private void discharge(ServerWorld world, BlockPos catalystPos, BlockState catalyst) {
        Random random = world.getRandom();
        int amount = Math.min(this.charges, CHARGES_PER_PULSE);
        this.spreadManager.spread(catalystPos.add(random.nextBetween(-1, 1), 0, random.nextBetween(-1, 1)), amount);
        this.setCharges(this.charges - amount);
        SculkCatalystBlock.bloom(world, catalystPos, catalyst, random);
    }

    @Override
    public PositionSource getPositionSource() {
        return this.positionSource;
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public boolean shouldListenImmediately() {
        return true;
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent.Message event) {
        if (this.isRemoved() || event.getEvent() != GameEvent.ENTITY_DIE || this.charges >= MAX_CHARGES) {
            return false;
        }
        if (!(event.getEmitter().sourceEntity() instanceof LivingEntity entity)
                || entity.isExperienceDroppingDisabled() || !entity.shouldDropXp()) {
            return false;
        }

        int experience = entity.getXpToDrop();
        if (experience <= 0) {
            return false;
        }
        entity.disableExperienceDropping();
        this.setCharges(this.charges + experience);
        world.playSound(null, this.pos, SoundEvents.BLOCK_SCULK_CHARGE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.charges = MathHelper.clamp(nbt.getInt(CHARGES_KEY), 0, MAX_CHARGES);
        this.spreadManager.readNbt(nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        // Left out when empty so freshly broken capacitors still stack with crafted ones.
        if (this.charges > 0) {
            nbt.putInt(CHARGES_KEY, this.charges);
        }
        this.spreadManager.writeNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(CHARGES_KEY, this.charges);
        return nbt;
    }
}
