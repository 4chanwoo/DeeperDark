package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.entity.AncientVaseBlockEntity;
import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkBlockEntities {
    public static final BlockEntityType<SculkCapacitorBlockEntity> SCULK_CAPACITOR = register("sculk_capacitor",
            FabricBlockEntityTypeBuilder.create(SculkCapacitorBlockEntity::new, DeeperDarkBlocks.SCULK_CAPACITOR).build());

    public static final BlockEntityType<AncientVaseBlockEntity> ANCIENT_VASE = register("ancient_vase",
            FabricBlockEntityTypeBuilder.create(AncientVaseBlockEntity::new,
                    DeeperDarkBlocks.ROTUND_SCREAMING_VASE,
                    DeeperDarkBlocks.TURGID_SCREAMING_VASE,
                    DeeperDarkBlocks.GAUNT_SCREAMING_VASE).build());

    private static <T extends BlockEntityType<?>> T register(String name, T blockEntityType) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, name), blockEntityType);
    }

    public static void init() {
    }

    private DeeperDarkBlockEntities() {
    }
}
