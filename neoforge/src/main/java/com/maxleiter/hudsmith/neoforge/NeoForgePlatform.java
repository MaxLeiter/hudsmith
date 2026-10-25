package com.maxleiter.hudsmith.neoforge;

import java.nio.file.Path;

import com.maxleiter.hudsmith.platform.Platform;

import net.neoforged.fml.loading.FMLPaths;

public final class NeoForgePlatform implements Platform {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
