package com.maxleiter.hudsmith.client;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Client calls whose shape differs between the Minecraft versions Hudsmith builds for. */
public final class McClient {
    private McClient() {
    }

    public static @Nullable Screen screen() {
        //? if >=26 {
        return Minecraft.getInstance().gui.screen();
        //?} else
        /*return Minecraft.getInstance().screen;*/
    }

    public static void setScreen(@Nullable Screen screen) {
        //? if >=26 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else
        /*Minecraft.getInstance().setScreen(screen);*/
    }

    public static void copy(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    public static void openFolder(Path dir) {
        //? if >=26.3 {
        com.mojang.blaze3d.Blaze3D.openPath(dir);
        //?} else if >=26 {
        /*net.minecraft.util.Util.getPlatform().openPath(dir);
        *///?} else
        /*net.minecraft.Util.getPlatform().openPath(dir);*/
    }

    public static String id(ResourceKey<?> key) {
        return location(key).toString();
    }

    public static Identifier location(ResourceKey<?> key) {
        //? if >=26 {
        return key.identifier();
        //?} else
        /*return key.location();*/
    }

    /** Ticks since the world's first sunrise: the overworld clock on 26.x, the day time before. */
    public static long dayTime(Level level) {
        //? if >=26 {
        return level.getDefaultClockTime();
        //?} else
        /*return level.getDayTime();*/
    }

    public static int selectedSlot(net.minecraft.world.entity.player.Inventory inventory) {
        //? if >=26 {
        return inventory.getSelectedSlot();
        //?} else
        /*return inventory.selected;*/
    }

    /** Every attribute in the game, modded ones included. */
    public static java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributes() {
        //? if >=26 {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements();
        //?} else
        /*return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders();*/
    }

    public static void clearToasts(Minecraft mc) {
        //? if >=26 {
        mc.gui.toastManager().clear();
        //?} else
        /*mc.getToasts().clear();*/
    }

    public static boolean debugScreenShown(Minecraft mc) {
        //? if >=26 {
        return mc.debugEntries.isOverlayVisible();
        //?} else
        /*return mc.getDebugOverlay().showDebugScreen();*/
    }

    /** Whether an overlay (resources reloading) covers the screen. */
    public static boolean overlayShown(Minecraft mc) {
        //? if >=26 {
        return mc.gui.overlay() != null;
        //?} else
        /*return mc.getOverlay() != null;*/
    }

    /** pack.mcmeta for a resource pack made for this version. */
    public static String packMeta(String description) {
        //? if >=26 {
        var format = net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        String range = "[" + format.major() + ", " + format.minor() + "]";
        return "{\"pack\": {\"description\": \"" + description + "\", \"min_format\": " + range + ", \"max_format\": " + range + "}}";
        //?} else
        /*return "{\"pack\": {\"description\": \"" + description + "\", \"pack_format\": " + net.minecraft.SharedConstants.getCurrentVersion().getPackVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES) + "}}";*/
    }
}
