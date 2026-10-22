package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.SculkOrificeBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Only exists so the client can animate the jaws. It keeps no saved data. */
public class SculkOrificeBlockEntity extends BlockEntity {
    /** World time of the last bite, or a value far in the past if it hasn't bitten yet. */
    private long lastBiteTime = Long.MIN_VALUE / 2;

    public SculkOrificeBlockEntity(BlockPos pos, BlockState state) {
        super(DeeperDarkBlockEntities.SCULK_ORIFICE, pos, state);
    }

    public long getLastBiteTime() {
        return this.lastBiteTime;
    }

    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        if (type == SculkOrificeBlock.BITE_EVENT && this.world != null) {
            this.lastBiteTime = this.world.getTime();
            return true;
        }
        return super.onSyncedBlockEvent(type, data);
    }
}
