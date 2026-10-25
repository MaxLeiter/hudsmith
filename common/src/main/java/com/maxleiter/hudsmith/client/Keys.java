package com.maxleiter.hudsmith.client;

import java.util.List;

import com.maxleiter.hudsmith.Hudsmith;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

/** Hudsmith's key mappings, unbound by default: F1 already hides the HUD, and /hudsmith opens the screen. */
public final class Keys {
    //? if >=26 {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Hudsmith.id("hudsmith"));
    //?} else
    /*public static final String CATEGORY = "key.category.hudsmith.hudsmith";*/

    public static final KeyMapping TOGGLE = new KeyMapping("key.hudsmith.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping SCREEN = new KeyMapping("key.hudsmith.screen", InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static final List<KeyMapping> ALL = List.of(TOGGLE, SCREEN);

    private Keys() {
    }
}
