package com.chanwoo.deeperdark.common.item;

import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SculkCapacitorBlockItem extends BlockItem {
    private static final int BAR_COLOR = 0x29DFEB;

    public SculkCapacitorBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    public static int getCharges(ItemStack stack) {
        NbtCompound nbt = BlockItem.getBlockEntityNbt(stack);
        return nbt == null ? 0 : nbt.getInt(SculkCapacitorBlockEntity.CHARGES_KEY);
    }

    public static ItemStack withCharges(ItemStack stack, int charges) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(SculkCapacitorBlockEntity.CHARGES_KEY, charges);
        BlockItem.setBlockEntityNbt(stack, DeeperDarkBlockEntities.SCULK_CAPACITOR, nbt);
        return stack;
    }

    /** The creative tab hands out capacitors that are already full. */
    @Override
    public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks) {
        if (this.isIn(group)) {
            stacks.add(withCharges(new ItemStack(this), SculkCapacitorBlockEntity.MAX_CHARGES));
        }
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getCharges(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0f * getCharges(stack) / SculkCapacitorBlockEntity.MAX_CHARGES);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.deeper_dark.sculk_capacitor.charges", getCharges(stack), SculkCapacitorBlockEntity.MAX_CHARGES)
                .formatted(Formatting.DARK_AQUA));
    }
}
