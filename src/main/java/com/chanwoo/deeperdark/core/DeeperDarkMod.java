package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.entity.SculkAlerts;
import com.chanwoo.deeperdark.common.worldgen.DeeperDarkFeatures;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeeperDarkMod implements ModInitializer {
    public static final String MOD_ID = "deeper_dark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DeeperDarkSounds.init();
        DeeperDarkParticles.init();
        DeeperDarkBlocks.init();
        DeeperDarkBlockEntities.init();
        DeeperDarkGameEvents.init();
        DeeperDarkFeatures.init();
        DeeperDarkWorld.init();
        SculkAlerts.init();
    }
}
