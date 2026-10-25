package com.maxleiter.hudsmith.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.maxleiter.hudsmith.Hudsmith;

/** config/hudsmith/settings.json. The layout itself is the page, not a setting. */
public final class Settings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings current = new Settings();

    /** Whether the HUD shows; the toggle key flips it. */
    public boolean shown = true;

    public static Settings get() {
        return current;
    }

    private static Path file() {
        return HudFolder.dir().resolve("settings.json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) {
            current = new Settings();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Settings loaded = GSON.fromJson(reader, Settings.class);
            current = loaded == null ? new Settings() : loaded;
        } catch (IOException | JsonParseException e) {
            Hudsmith.LOG.error("Couldn't read {}, using the defaults", file, e);
            current = new Settings();
        }
    }

    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(current, writer);
            }
        } catch (IOException e) {
            Hudsmith.LOG.error("Couldn't write {}", file, e);
        }
    }
}
