package com.maxleiter.hudsmith.neoforge;

import com.maxleiter.hudsmith.Hudsmith;
import com.maxleiter.hudsmith.client.HudsmithClient;
import com.maxleiter.hudsmith.client.Keys;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Hudsmith.MOD_ID, dist = Dist.CLIENT)
public final class HudsmithNeoForgeClient {
    public HudsmithNeoForgeClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(FMLClientSetupEvent.class, event -> HudsmithClient.init());
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            //? if >=26
            event.registerCategory(Keys.CATEGORY);
            for (KeyMapping key : Keys.ALL) event.register(key);
        });
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> HudsmithClient.configScreen(parent));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> HudsmithClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> event.getDispatcher().register(
                Commands.literal(Hudsmith.MOD_ID).executes(context -> {
                    HudsmithClient.openScreenNextTick();
                    return 1;
                })));
    }
}
