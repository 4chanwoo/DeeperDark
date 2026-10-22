package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.block.*;
import com.chanwoo.deeperdark.common.item.SculkCapacitorBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.BiFunction;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkBlocks {
    public static final AncientVaseBlock ROTUND_SCREAMING_VASE = register("rotund_screaming_vase",
            new RotundScreamingVaseBlock(vaseSettings(0.5f)));
    public static final AncientVaseBlock TURGID_SCREAMING_VASE = register("turgid_screaming_vase",
            new TurgidScreamingVaseBlock(vaseSettings(0.3f)));
    public static final AncientVaseBlock GAUNT_SCREAMING_VASE = register("gaunt_screaming_vase",
            new GauntScreamingVaseBlock(vaseSettings(0.3f)));

    public static final Block ECHO_BONE = register("echo_bone",
            new PillarBlock(FabricBlockSettings.copy(Blocks.BONE_BLOCK)));

    public static final Block SCULK_ORIFICE = register("sculk_orifice",
            new SculkOrificeBlock(FabricBlockSettings.copy(Blocks.SCULK).strength(0.6f).nonOpaque()));

    public static final Block SCULK_CAPACITOR = register("sculk_capacitor",
            new SculkCapacitorBlock(AbstractBlock.Settings.of(Material.SCULK).strength(3.0f).sounds(BlockSoundGroup.SCULK_CATALYST).nonOpaque()),
            SculkCapacitorBlockItem::new);

    private static AbstractBlock.Settings vaseSettings(float strength) {
        return AbstractBlock.Settings.of(Material.STONE).strength(strength).nonOpaque().sounds(DeeperDarkSounds.ANCIENT_VASE);
    }

    private static <T extends Block> T register(String name, T block) {
        return register(name, block, BlockItem::new);
    }

    private static <T extends Block> T register(String name, T block, BiFunction<Block, Item.Settings, ? extends Item> itemFactory) {
        Identifier id = new Identifier(MOD_ID, name);
        Registry.register(Registry.BLOCK, id, block);
        Registry.register(Registry.ITEM, id, itemFactory.apply(block, new Item.Settings().group(DeeperDarkItemGroup.DEEPER_DARK)));
        return block;
    }

    public static void init() {
    }

    private DeeperDarkBlocks() {
    }
}
