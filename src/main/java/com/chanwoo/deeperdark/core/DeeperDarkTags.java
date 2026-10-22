package com.chanwoo.deeperdark.core;

import net.minecraft.entity.EntityType;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.event.GameEvent;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkTags {
    /** Game events that ancient vases react to. */
    public static final TagKey<GameEvent> VASE_CAN_LISTEN = TagKey.of(Registry.GAME_EVENT_KEY, new Identifier(MOD_ID, "vase_can_listen"));
    /** Projectiles that shatter ancient vases on impact. */
    public static final TagKey<EntityType<?>> SHATTERS_VASES = TagKey.of(Registry.ENTITY_TYPE_KEY, new Identifier(MOD_ID, "shatters_vases"));

    private DeeperDarkTags() {
    }
}
