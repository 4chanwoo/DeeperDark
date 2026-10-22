package com.chanwoo.deeperdark.mixin;

import com.chanwoo.deeperdark.common.block.SculkCapacitorBlock;
import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.SculkPatchFeature;
import net.minecraft.world.gen.feature.SculkPatchFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sometimes leaves a partly charged sculk capacitor on top of the catalyst a sculk patch grows from. */
@Mixin(SculkPatchFeature.class)
public abstract class SculkPatchFeatureMixin {
    @Unique
    private static final float CAPACITOR_CHANCE = 0.2f;
    @Unique
    private static final int MIN_CHARGES = 16;
    @Unique
    private static final int MAX_CHARGES = 320;
    /** Open sides needed around the spot, so capacitors only sit on exposed catalysts. */
    @Unique
    private static final int MIN_OPEN_SIDES = 3;

    @Inject(method = "generate", at = @At("RETURN"))
    private void deeperDark$placeCapacitor(FeatureContext<SculkPatchFeatureConfig> context, CallbackInfoReturnable<Boolean> cir) {
        StructureWorldAccess world = context.getWorld();
        BlockPos catalystPos = context.getOrigin();
        Random random = context.getRandom();
        if (!cir.getReturnValueZ() || !world.getBlockState(catalystPos).isOf(Blocks.SCULK_CATALYST) || random.nextFloat() >= CAPACITOR_CHANCE) {
            return;
        }

        BlockPos pos = catalystPos.up();
        if (!world.getBlockState(pos).isAir()) {
            return;
        }
        int openSides = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(pos.offset(direction)).isAir()) {
                openSides++;
            }
        }
        if (openSides < MIN_OPEN_SIDES) {
            return;
        }

        int charges = random.nextBetween(MIN_CHARGES, MAX_CHARGES);
        world.setBlockState(pos, DeeperDarkBlocks.SCULK_CAPACITOR.getDefaultState()
                .with(SculkCapacitorBlock.CHARGE_LEVEL, SculkCapacitorBlock.levelFor(charges)), Block.NOTIFY_LISTENERS);
        if (world.getBlockEntity(pos) instanceof SculkCapacitorBlockEntity capacitor) {
            capacitor.setGeneratedCharges(charges);
        }
    }
}
