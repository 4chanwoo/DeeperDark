package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;

import java.util.List;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public class DeeperDarkItemGroup {
    public static final ItemGroup DEEPER_DARK = FabricItemGroupBuilder.create(new Identifier(MOD_ID, "deeper_dark"))
            .icon(() -> new ItemStack(DeeperDarkBlocks.SCULK_CAPACITOR.asItem()))
            .appendItems(DeeperDarkItemGroup::appendItems)
            .build();

    /** Every item in the group, with a naturally generated copy of each vase after it for testing screams. */
    private static void appendItems(List<ItemStack> stacks, ItemGroup group) {
        for (Item item : Registry.ITEM) {
            if (!Registry.ITEM.getId(item).getNamespace().equals(MOD_ID)) {
                continue;
            }
            DefaultedList<ItemStack> itemStacks = DefaultedList.of();
            item.appendStacks(group, itemStacks);
            stacks.addAll(itemStacks);
            if (!itemStacks.isEmpty() && item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AncientVaseBlock vase) {
                stacks.add(naturalVase(vase));
            }
        }
    }

    /** A vase that places as if worldgen had put it there: it smokes, screams when broken and drops loot. */
    private static ItemStack naturalVase(Block vase) {
        ItemStack stack = new ItemStack(vase);
        NbtCompound blockState = new NbtCompound();
        blockState.putString(AncientVaseBlock.GENERATED.getName(), "true");
        stack.setSubNbt(BlockItem.BLOCK_STATE_TAG_KEY, blockState);
        stack.setCustomName(Text.translatable("item.deeper_dark.natural_vase", vase.getName())
                .styled(style -> style.withItalic(false)));
        return stack;
    }
}
