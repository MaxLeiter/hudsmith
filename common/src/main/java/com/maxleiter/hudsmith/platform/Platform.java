package com.maxleiter.hudsmith.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

/** What differs between NeoForge and Fabric. Each loader provides one through META-INF/services. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Hudsmith platform: the loader jar is missing its service file"));

    Path configDir();
}
