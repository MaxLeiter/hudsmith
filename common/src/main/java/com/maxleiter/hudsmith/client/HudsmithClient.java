package com.maxleiter.hudsmith.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.maxleiter.hudsmith.Hudsmith;

import dev.vellum.mod.client.VellumHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The client half, called by each loader's client entrypoint and events. Hudsmith is client-only. */
public final class HudsmithClient {
    public static final Identifier HUD = Hudsmith.id("hud");

    /**
     * Who gets the values besides the HUD: the open Hudsmith screen, the dev autopilot's template overlays. They get
     * each change, and null when the world closes.
     */
    private static final List<Consumer<@Nullable JsonObject>> LISTENERS = new ArrayList<>();
    private static @Nullable JsonObject last;
    private static boolean wroteReference;
    private static boolean openScreen;

    private HudsmithClient() {
    }

    /** Client setup. Vellum wants overlays registered before the game finishes loading. */
    public static void init() {
        HudFolder.ensureDefaults();
        Settings.load();
        BuiltInValues.register();
        HudPage.register();
        VellumHud.register(HUD, HudPage.URL);
        if (Settings.get().shown) VellumHud.show(HUD);
        DevAutopilot.init();
    }

    public static void tick(Minecraft mc) {
        DevAutopilot.tick(mc);
        while (Keys.TOGGLE.consumeClick()) toggle();
        while (Keys.SCREEN.consumeClick()) openScreen = true;
        if (openScreen) {
            openScreen = false;
            ValuesScreen.open(McClient.screen());
        }
        if (mc.player == null || mc.level == null) {
            if (last != null) {
                last = null;
                LISTENERS.forEach(listener -> listener.accept(null));
            }
            return;
        }
        // Other mods register their values during their own setup, which may finish after Hudsmith's.
        if (!wroteReference) {
            wroteReference = true;
            HudFolder.writeValuesReference();
        }
        BuiltInValues.tick(mc);
        if (!Settings.get().shown && LISTENERS.isEmpty()) return;
        JsonObject data = Values.compute(mc);
        if (data.equals(last)) return;
        last = data;
        VellumHud.push(HUD, data);
        LISTENERS.forEach(listener -> listener.accept(data));
    }

    /** The values last computed; null outside a world. */
    static @Nullable JsonObject values() {
        return last;
    }

    static void addListener(Consumer<@Nullable JsonObject> listener) {
        LISTENERS.add(listener);
    }

    static void removeListener(Consumer<@Nullable JsonObject> listener) {
        LISTENERS.remove(listener);
    }

    public static void setShown(boolean shown) {
        Settings.get().shown = shown;
        Settings.save();
        if (shown) VellumHud.show(HUD);
        else VellumHud.hide(HUD);
    }

    public static void toggle() {
        setShown(!Settings.get().shown);
    }

    /** For /hudsmith: the chat screen is still closing when the command runs, so the screen opens next tick. */
    public static void openScreenNextTick() {
        openScreen = true;
    }

    /** A screen for the loaders' config buttons (NeoForge's mod list, Mod Menu) that opens the Hudsmith screen. */
    public static Screen configScreen(@Nullable Screen parent) {
        return new Screen(Component.translatable("hudsmith.gui.title")) {
            @Override
            public void tick() {
                ValuesScreen.open(parent);
            }
        };
    }
}
