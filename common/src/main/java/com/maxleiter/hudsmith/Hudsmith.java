package com.maxleiter.hudsmith;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class Hudsmith {
    public static final String MOD_ID = "hudsmith";
    public static final Logger LOG = LogUtils.getLogger();

    private Hudsmith() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
