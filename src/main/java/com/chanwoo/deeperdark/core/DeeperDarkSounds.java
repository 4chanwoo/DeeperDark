package com.chanwoo.deeperdark.core;

import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public final class DeeperDarkSounds {
    /** Glass layer of a vase shattering. */
    public static final SoundEvent ANCIENT_VASE_BREAK = register("block.ancient_vase.break");
    /** Ceramic layer of a vase shattering, played on top of {@link #ANCIENT_VASE_BREAK}. */
    public static final SoundEvent ANCIENT_VASE_CRUMBLE = register("block.ancient_vase.crumble");

    public static final BlockSoundGroup ANCIENT_VASE = new BlockSoundGroup(1.0f, 1.0f,
            ANCIENT_VASE_BREAK,
            SoundEvents.BLOCK_DEEPSLATE_TILES_STEP,
            SoundEvents.BLOCK_DEEPSLATE_TILES_PLACE,
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
