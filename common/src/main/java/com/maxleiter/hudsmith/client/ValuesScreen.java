package com.maxleiter.hudsmith.client;

import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.vellum.mod.client.DocumentDriver;
import dev.vellum.mod.client.VellumScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The Hudsmith screen: every value with what it is right now, which copies its template text when clicked, buttons
 * that swap in a template and open the HUD folder. The list is the page's data; the values now arrive as "now"
 * messages, so the list isn't sent again with every change.
 */
public final class ValuesScreen {
    private static final String URL = "hudsmith:vellum/values.html";

    private ValuesScreen() {
    }

    public static void open(@Nullable Screen parent) {
        JsonObject screen = new JsonObject();
        JsonArray values = new JsonArray();
        for (Values.Value value : Values.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", value.name());
            entry.addProperty("category", value.category());
            entry.addProperty("description", value.description());
            values.add(entry);
        }
        screen.add("values", values);
        JsonArray templates = new JsonArray();
        HudFolder.TEMPLATES.forEach(templates::add);
        screen.add("templates", templates);
        screen.addProperty("folder", HudFolder.dir().toString());
        screen.add("now", HudsmithClient.values());
        screen.addProperty("shown", Settings.get().shown);
        JsonObject data = new JsonObject();
        data.add("screen", screen);

        DocumentDriver driver = VellumScreens.open(URL, data).driver();
        Consumer<@Nullable JsonObject> live = now -> driver.receive("now", now == null ? "null" : now.toString());
        HudsmithClient.addListener(live);
        driver.onMessage("copy", ValuesScreen::copy)
                .onMessage("folder", value -> McClient.openFolder(HudFolder.dir()))
                .onMessage("template", ValuesScreen::useTemplate)
                .onMessage("toggle", value -> {
                    HudsmithClient.toggle();
                    driver.receive("shown", new JsonPrimitive(Settings.get().shown).toString());
                })
                .onClose(() -> {
                    HudsmithClient.removeListener(live);
                    if (parent != null) Minecraft.getInstance().execute(() -> {
                        if (McClient.screen() == null) McClient.setScreen(parent);
                    });
                });
    }

    /** The page asks to replace the HUD with a template. A resource pack can replace the page: only known names. */
    private static void useTemplate(JsonElement value) {
        String name = string(value);
        if (name != null && HudFolder.TEMPLATES.contains(name)) HudFolder.useTemplate(name);
    }

    /** The page asks to copy a value's template text. Only known names. */
    private static void copy(JsonElement value) {
        String name = string(value);
        if (name != null && Values.has(name)) McClient.copy("{{ " + name + " }}");
    }

    private static @Nullable String string(JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }
}
