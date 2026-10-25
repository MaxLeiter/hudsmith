package com.maxleiter.hudsmith.fabric;

import com.maxleiter.hudsmith.client.HudsmithClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's config button opens the Hudsmith screen. Mod Menu loads this class only when it is installed. */
public final class HudsmithModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HudsmithClient::configScreen;
    }
}
