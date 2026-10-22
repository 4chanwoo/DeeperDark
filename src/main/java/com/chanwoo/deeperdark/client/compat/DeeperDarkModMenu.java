package com.chanwoo.deeperdark.client.compat;

import com.chanwoo.deeperdark.client.DeeperDarkConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Only loaded by Mod Menu, so Mod Menu stays an optional dependency. */
public class DeeperDarkModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DeeperDarkConfigScreen::new;
    }
}
