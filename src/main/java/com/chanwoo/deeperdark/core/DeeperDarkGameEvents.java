package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.entity.AncientVaseBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.event.GameEvent;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkGameEvents {
    /** Emitted by an ancient vase when it shatters or rings. Heard by other vases, sculk sensors and shriekers. */
    public static final GameEvent VASE_SCREAM = register("vase_scream", AncientVaseBlockEntity.RANGE);

    private static GameEvent register(String name, int range) {
        Identifier id = new Identifier(MOD_ID, name);
        return Registry.register(Registry.GAME_EVENT, id, new GameEvent(id.toString(), range));
    }

    public static void init() {
    }

    private DeeperDarkGameEvents() {
    }
}
