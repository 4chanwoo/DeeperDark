package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkTags;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.VibrationListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Lives in the base of every ancient vase. Listens for other vases screaming and, when one is heard,
 * makes this vase ring and pass the scream on with one less strength.
 */
public class AncientVaseBlockEntity extends BlockEntity implements VibrationListener.Callback {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int LISTENER_RANGE = 16;

    private VibrationListener listener;
    /** Strength of the last scream this vase made. Read by the vases that hear it. */
    private int signalStrength;
    /** Strength this vase will scream with once the vibration it accepted arrives. */
    private int pendingStrength;

    public AncientVaseBlockEntity(BlockPos pos, BlockState state) {
        super(DeeperDarkBlockEntities.ANCIENT_VASE, pos, state);
        this.listener = new VibrationListener(new BlockPositionSource(pos), LISTENER_RANGE, this, null, 0.0f, 0);
    }

    public VibrationListener getEventListener() {
        return this.listener;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
        this.markDirty();
    }

    @Override
    public TagKey<GameEvent> getTag() {
        return DeeperDarkTags.VASE_CAN_LISTEN;
    }

    @Override
    public boolean accepts(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable GameEvent.Emitter emitter) {
        BlockState state = this.getCachedState();
        if (this.isRemoved() || !(state.getBlock() instanceof AncientVaseBlock vase) || state.get(AncientVaseBlock.RINGING)) {
            return false;
        }

        // Ignore our own scream, which comes from the top of this vase.
        if (pos.getX() == this.pos.getX() && pos.getZ() == this.pos.getZ()
                && pos.getY() >= this.pos.getY() && pos.getY() < this.pos.getY() + vase.getHeight()) {
            return false;
        }

        int incoming = getScreamStrength(world, pos);
        if (incoming <= 0) {
            return false;
        }
        this.pendingStrength = incoming - 1;
        return true;
    }

    /** Looks up the strength of the scream emitted from the top of the vase at {@code pos}. */
    private static int getScreamStrength(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AncientVaseBlock vase
                && world.getBlockEntity(vase.getBasePos(state, pos)) instanceof AncientVaseBlockEntity sender) {
            return sender.signalStrength;
        }
        return 0;
    }

    @Override
    public void accept(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity, @Nullable Entity sourceEntity, float distance) {
        BlockState state = this.getCachedState();
        if (state.getBlock() instanceof AncientVaseBlock vase && !state.get(AncientVaseBlock.RINGING)) {
            vase.scream(world, this.pos, state, this.pendingStrength, sourceEntity != null ? sourceEntity : entity);
        }
    }

    @Override
    public void onListen() {
        this.markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.signalStrength = nbt.getInt("SignalStrength");
        this.pendingStrength = nbt.getInt("PendingStrength");
        if (nbt.contains("listener", NbtElement.COMPOUND_TYPE)) {
            VibrationListener.createCodec(this)
                    .parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("listener")))
                    .resultOrPartial(LOGGER::error)
                    .ifPresent(listener -> this.listener = listener);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("SignalStrength", this.signalStrength);
        nbt.putInt("PendingStrength", this.pendingStrength);
        VibrationListener.createCodec(this)
                .encodeStart(NbtOps.INSTANCE, this.listener)
                .resultOrPartial(LOGGER::error)
                .ifPresent(listenerNbt -> nbt.put("listener", listenerNbt));
    }
}
