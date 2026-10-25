package com.maxleiter.hudsmith.fabric;

import java.nio.file.Path;

import com.maxleiter.hudsmith.platform.Platform;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
