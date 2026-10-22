package com.chanwoo.deeperdark.mixin;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps blocks from being placed floating above the short rims of turgid and gaunt vases. */
@Mixin(ItemPlacementContext.class)
public abstract class ItemPlacementContextMixin {
    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void deeperDark$keepVaseTopsClear(CallbackInfoReturnable<Boolean> cir) {
        ItemPlacementContext self = (ItemPlacementContext) (Object) this;
        BlockState below = self.getWorld().getBlockState(self.getBlockPos().down());
        if (below.getBlock() instanceof AncientVaseBlock vase && !vase.canHoldBlockAbove(below)) {
            cir.setReturnValue(false);
        }
    }
}
