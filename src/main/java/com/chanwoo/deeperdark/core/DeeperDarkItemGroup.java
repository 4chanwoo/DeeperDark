package com.chanwoo.deeperdark.core;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public class DeeperDarkItemGroup {
    public static final ItemGroup DEEPER_DARK = FabricItemGroupBuilder.build(
            new Identifier(MOD_ID,"deeper_dark"),() -> new ItemStack(DeeperDarkBlocks.SCULK_CAPACITOR.asItem()));

}
