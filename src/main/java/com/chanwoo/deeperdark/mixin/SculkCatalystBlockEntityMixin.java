package com.chanwoo.deeperdark.mixin;

import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkCatalystBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A catalyst with a capacitor sitting on it lets the capacitor take the experience instead of spreading sculk,
 * so kills charge the capacitor until it's full.
 */
@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin {
    @Inject(method = "listen", at = @At("HEAD"), cancellable = true)
    private void deeperDark$deferToCapacitor(ServerWorld world, GameEvent.Message event, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (event.getEvent() == GameEvent.ENTITY_DIE
                && world.getBlockEntity(self.getPos().up()) instanceof SculkCapacitorBlockEntity capacitor
                && capacitor.canAbsorb()) {
            cir.setReturnValue(false);
        }
    }
}
