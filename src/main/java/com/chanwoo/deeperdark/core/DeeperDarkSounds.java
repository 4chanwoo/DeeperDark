package com.chanwoo.deeperdark.core;

import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkSounds {
    /** Ceramic shatter. */
    public static final SoundEvent ANCIENT_VASE_SHATTER = register("block.ancient_vase.shatter");
    /** Ceramic set down on stone. */
    public static final SoundEvent ANCIENT_VASE_PLACE = register("block.ancient_vase.place");

    public static final BlockSoundGroup ANCIENT_VASE = new BlockSoundGroup(1.0f, 1.0f,
            ANCIENT_VASE_SHATTER,
            SoundEvents.BLOCK_DEEPSLATE_TILES_STEP,
            ANCIENT_VASE_PLACE,
            SoundEvents.BLOCK_DEEPSLATE_TILES_HIT,
            SoundEvents.BLOCK_DEEPSLATE_TILES_FALL);

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(MOD_ID, name);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    public static void init() {
    }

    private DeeperDarkSounds() {
    }
}
