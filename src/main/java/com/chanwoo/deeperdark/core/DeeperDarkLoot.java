package com.chanwoo.deeperdark.core;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

/** Loot from other mods, added to vases only when that mod is installed. */
public final class DeeperDarkLoot {
    /** The pool each shattered vase rolls 3 to 5 times. */
    private static final Identifier VASE_LOOT = new Identifier(MOD_ID, "chests/single_ancient_city");
    private static final Identifier CLOTHED_INGOT = new Identifier("ratsmischief", "clothed_ingot");
    /** Per roll, so roughly one vase in five holds one. */
    private static final float CLOTHED_INGOT_CHANCE = 0.05f;

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded(CLOTHED_INGOT.getNamespace())) {
            return;
        }
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!VASE_LOOT.equals(id)) {
                return;
            }
            Registry.ITEM.getOrEmpty(CLOTHED_INGOT).ifPresent(item -> tableBuilder.pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(CLOTHED_INGOT_CHANCE))
                    .with(ItemEntry.builder(item))));
        });
    }

    private DeeperDarkLoot() {
    }
}
