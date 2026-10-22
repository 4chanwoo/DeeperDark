package com.chanwoo.deeperdark.mixin;

import com.chanwoo.deeperdark.common.entity.SculkContainment;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops sculk cursors from moving out of a capacitor's border. */
@Mixin(SculkSpreadManager.Cursor.class)
public abstract class SculkSpreadCursorMixin {
    @Inject(method = "canSpread(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void deeperDark$stayContained(WorldAccess world, BlockPos sourcePos, BlockPos targetPos, CallbackInfoReturnable<Boolean> cir) {
        if (!SculkContainment.mayMove(world, sourcePos, targetPos)) {
            cir.setReturnValue(false);
        }
    }
}
