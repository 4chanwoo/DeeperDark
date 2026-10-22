package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.SculkCapacitorBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkCatalystBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkCatalystBlockEntity;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
    private static final int SYNC_STEP = 32;
    /** How many spread ticks to simulate at once when a capacitor bursts with no catalyst to take its charge. */
    private static final int BURST_TICKS = 40;
    private static final int GLOW_INTERVAL = 3;
    private static final double GLOW_VIEW_DISTANCE = 24.0;

    private final SculkSpreadManager spreadManager = SculkSpreadManager.create();
    private final BlockPositionSource positionSource;
    private int charges;
    /** Set when the block is picked up intact, so removing it doesn't burst its charges out. */
    private boolean keepCharges;

    public SculkCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(DeeperDarkBlockEntities.SCULK_CAPACITOR, pos, state);
        this.positionSource = new BlockPositionSource(pos);
    }

    public int getCharges() {
        return this.charges;
    }

    /** Whether this capacitor can still take charge. Catalysts underneath hold back their own spreading for it. */
    public boolean canAbsorb() {
        return !this.isRemoved() && this.charges < MAX_CHARGES;
    }

    public int getComparatorOutput() {
        return this.charges == 0 ? 0 : MathHelper.ceil(15.0f * this.charges / MAX_CHARGES);
    }

    /** Used by world generation, where there is no world to sync to yet. */
    public void setGeneratedCharges(int charges) {
        this.charges = MathHelper.clamp(charges, 0, MAX_CHARGES);
    }

    public void keepCharges() {
        this.keepCharges = true;
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
            BlockState state = this.getCachedState();
            int level = SculkCapacitorBlock.levelFor(clamped);
            if (state.contains(SculkCapacitorBlock.CHARGE_LEVEL) && state.get(SculkCapacitorBlock.CHARGE_LEVEL) != level) {
                this.world.setBlockState(this.pos, state.with(SculkCapacitorBlock.CHARGE_LEVEL, level), Block.NOTIFY_LISTENERS);
            }
            this.world.updateComparators(this.pos, this.getCachedState().getBlock());
            if (needsSync) {
                this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    /** Keeps the charge visibly swirling inside the glass. Random display ticks are far too rare for this. */
    public static void clientTick(World world, BlockPos pos, BlockState state, SculkCapacitorBlockEntity capacitor) {
        if (capacitor.charges <= 0 || (world.getTime() + (pos.asLong() & 3)) % GLOW_INTERVAL != 0
                || world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, GLOW_VIEW_DISTANCE, false) == null) {
            return;
        }
        // The particle factory reads the fill from the x velocity and the orbit angle from the y velocity.
        double fill = (double) capacitor.charges / MAX_CHARGES;
        double angle = world.getTime() * 0.3 + (pos.asLong() % 628) / 100.0;
        world.addParticle(DeeperDarkParticles.CAPACITOR_GLOW, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, fill, angle, 0.0);
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
        SculkContainment.contain(world, this.pos);
        this.spreadManager.spread(catalystPos.add(random.nextBetween(-1, 1), 0, random.nextBetween(-1, 1)), amount);
        this.setCharges(this.charges - amount);
        SculkCatalystBlock.bloom(world, catalystPos, catalyst, random);
        world.spawnParticles(DeeperDarkParticles.CAPACITOR_PULSE, catalystPos.getX() + 0.5, catalystPos.getY() + 1.0, catalystPos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);

        // A capacitor is used up once it has poured out its last charge.
        if (this.charges == 0) {
            world.spawnParticles(ParticleTypes.SCULK_SOUL, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.03);
            world.breakBlock(this.pos, false);
        }
    }

    /**
     * Bursts the stored charge out as sculk. A catalyst underneath takes it over and spreads it naturally;
     * otherwise it all spreads at once.
     */
    public void releaseCharges(ServerWorld world) {
        if (this.keepCharges || this.charges <= 0) {
            return;
        }

        BlockPos catalystPos = this.pos.down();
        Random random = world.getRandom();
        SculkContainment.contain(world, this.pos);
        if (world.getBlockEntity(catalystPos) instanceof SculkCatalystBlockEntity catalyst) {
            catalyst.getSpreadManager().spread(this.pos, this.charges);
            SculkCatalystBlock.bloom(world, catalystPos, world.getBlockState(catalystPos), random);
        } else {
            SculkSpreadManager burst = SculkSpreadManager.create();
            burst.spread(this.pos, this.charges);
            for (int i = 0; i < BURST_TICKS && !burst.getCursors().isEmpty(); i++) {
                burst.tick(world, this.pos, random, true);
            }
        }

        world.playSound(null, this.pos, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.BLOCKS, 2.0f, 0.6f + random.nextFloat() * 0.2f);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5,
                Math.min(24, 4 + this.charges / 16), 0.4, 0.4, 0.4, 0.05);
        this.charges = 0;
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
        if (event.getEvent() != GameEvent.ENTITY_DIE || !this.canAbsorb()) {
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

        Vec3d from = event.getEmitterPos();
        world.spawnParticles(ParticleTypes.SCULK_SOUL, from.x, from.y + 0.5, from.z, 3, 0.2, 0.3, 0.2, 0.02);
        // With a count of 0 the offsets arrive as the particle's velocity: here, the way back to the corpse.
        Vec3d center = Vec3d.ofCenter(this.pos);
        world.spawnParticles(DeeperDarkParticles.CAPACITOR_ABSORB, center.x, center.y, center.z, 0,
                from.x - center.x, from.y + 0.5 - center.y, from.z - center.z, 1.0);
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
        // Left out when empty so empty capacitors stack with crafted ones.
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
