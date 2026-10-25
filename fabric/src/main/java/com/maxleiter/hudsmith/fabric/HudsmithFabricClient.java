package com.maxleiter.hudsmith.fabric;

import com.maxleiter.hudsmith.Hudsmith;
import com.maxleiter.hudsmith.client.HudsmithClient;
import com.maxleiter.hudsmith.client.Keys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class HudsmithFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudsmithClient.init();
        // Vanilla only sorts registered categories; Fabric has no event for it, so register it here.
        //? if >=26
        KeyMapping.Category.register(Keys.CATEGORY.id());
        for (KeyMapping key : Keys.ALL) KeyMappingHelper.registerKeyMapping(key);

        ClientTickEvents.END_CLIENT_TICK.register(HudsmithClient::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
                ClientCommands.literal(Hudsmith.MOD_ID).executes(context -> {
                    HudsmithClient.openScreenNextTick();
                    return 1;
                })));
    }
}
