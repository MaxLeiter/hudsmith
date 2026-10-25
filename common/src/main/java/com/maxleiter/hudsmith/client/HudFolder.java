package com.maxleiter.hudsmith.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.maxleiter.hudsmith.Hudsmith;
import com.maxleiter.hudsmith.platform.Platform;

/**
 * config/hudsmith/: the player's HUD page (hud.html, hud.css and whatever they link), settings.json, and VALUES.md,
 * the list of every value, rewritten each launch so it matches the mods installed.
 */
public final class HudFolder {
    /** A HUD's two files: the default and every template have them, in assets/hudsmith/vellum/templates/<name>/. */
    private static final List<String> FILES = List.of("hud.html", "hud.css");
    /** The templates in the jar, the default HUD first, then simplest first. */
    public static final List<String> TEMPLATES = List.of("default", "minimal", "compass", "survival", "builder", "performance");

    private HudFolder() {
    }

    public static Path dir() {
        return Platform.INSTANCE.configDir().resolve(Hudsmith.MOD_ID);
    }

    /** Puts the default HUD in place of any of its files that are missing. */
    public static void ensureDefaults() {
        copyTemplate("default", false);
    }

    /**
     * Replaces the HUD with a template's files, keeping the current ones as hud.html.bak and hud.css.bak. Vellum sees
     * the files change and reloads the HUD.
     */
    public static void useTemplate(String name) {
        if (!TEMPLATES.contains(name)) throw new IllegalArgumentException("No Hudsmith template " + name);
        copyTemplate(name, true);
        Hudsmith.LOG.info("Hudsmith: the HUD is now the {} template (the previous files are hud.html.bak and hud.css.bak)", name);
    }

    /** Copies a template's files into the folder: over the current ones (kept as .bak), or only where missing. */
    private static void copyTemplate(String name, boolean replace) {
        String from = "/assets/hudsmith/vellum/templates/" + name + "/";
        try {
            Files.createDirectories(dir());
            for (String file : FILES) {
                Path target = dir().resolve(file);
                boolean exists = Files.exists(target);
                if (exists && !replace) continue;
                try (InputStream in = HudFolder.class.getResourceAsStream(from + file)) {
                    if (in == null) throw new IllegalStateException("The Hudsmith jar has no " + from + file);
                    if (exists) Files.copy(target, dir().resolve(file + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't copy the " + name + " HUD into " + dir(), e);
        }
    }

    /** Rewrites VALUES.md from every registered value, Hudsmith's and other mods'. */
    public static void writeValuesReference() {
        // In the order they were registered: Hudsmith's own categories first, then other mods'.
        Map<String, List<Values.Value>> byCategory = new LinkedHashMap<>();
        for (Values.Value value : Values.all()) {
            byCategory.computeIfAbsent(value.category(), c -> new ArrayList<>()).add(value);
        }
        StringBuilder md = new StringBuilder("""
                # Hudsmith values

                Every value your HUD page can use. Hudsmith writes this file each time the game starts, so edits are
                lost. Use a value by name: `{{ fps }}`, `<div v-if="slime_chunk">`. A value with nothing to show is
                `null`. Functions like `fmt(x)` come from `hudsmith:vellum/lib/hud.js`. The Hudsmith screen (`/hudsmith`)
                shows what each value is right now.
                """);
        for (Map.Entry<String, List<Values.Value>> entry : byCategory.entrySet()) {
            md.append("\n## ").append(entry.getKey()).append("\n\n| Name | |\n|---|---|\n");
            for (Values.Value value : entry.getValue()) {
                md.append("| `").append(value.name()).append("` | ").append(value.description().replace("|", "\\|")).append(" |\n");
            }
        }
        Path file = dir().resolve("VALUES.md");
        try {
            Files.writeString(file, md);
        } catch (IOException e) {
            Hudsmith.LOG.error("Couldn't write {}", file, e);
        }
    }
}
