package com.maxleiter.hudsmith.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.maxleiter.hudsmith.Hudsmith;

import net.minecraft.client.Minecraft;

/**
 * Every value a HUD page can use, by name. Each is a top-level key of the page's data, present on every update (null
 * when there's nothing to show), so a misspelt name in a page is an error rather than an empty space.
 */
public final class Values {
    /** Names are page identifiers: snake_case, so they never need quoting in a template. */
    public static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]*");
    /** Keys the pages already have, which a value must not hide. */
    private static final Set<String> RESERVED = Set.of("vellum", "window", "document", "console", "item", "this");
    private static final Gson GSON = new Gson();

    private static final Map<String, Value> VALUES = new LinkedHashMap<>();
    /** Values that threw, each logged once until it works again. */
    private static final Set<String> FAILING = new HashSet<>();

    private Values() {
    }

    /**
     * @param category groups the value on the Hudsmith screen and in VALUES.md
     * @param compute  given the client with a player and a level; returns a String, Number, Boolean, JsonElement or
     *                 null
     */
    public record Value(String name, String category, String description, Function<Minecraft, Object> compute) {
    }

    public static synchronized void register(String name, String category, String description, Function<Minecraft, Object> compute) {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Hudsmith value names are snake_case (" + NAME.pattern() + "): " + name);
        }
        if (RESERVED.contains(name)) throw new IllegalArgumentException("Hudsmith value name is reserved: " + name);
        if (VALUES.containsKey(name)) throw new IllegalArgumentException("Hudsmith already has a value named " + name);
        VALUES.put(name, new Value(name, category, description, compute));
    }

    public static synchronized boolean has(String name) {
        return VALUES.containsKey(name);
    }

    public static synchronized Collection<Value> all() {
        return Collections.unmodifiableCollection(VALUES.values());
    }

    /** Every value now, as the page's data. Needs a player and a level. */
    public static synchronized JsonObject compute(Minecraft mc) {
        JsonObject data = new JsonObject();
        for (Value value : VALUES.values()) {
            data.add(value.name(), computeOne(mc, value));
        }
        return data;
    }

    /**
     * A value that throws shows as null and is logged once; one broken value (often another mod's) mustn't take the
     * whole HUD down. The log line names it.
     */
    private static JsonElement computeOne(Minecraft mc, Value value) {
        try {
            JsonElement json = toJson(value.compute().apply(mc));
            FAILING.remove(value.name());
            return json;
        } catch (RuntimeException e) {
            if (FAILING.add(value.name())) Hudsmith.LOG.error("Hudsmith value {} failed; it shows as null", value.name(), e);
            return JsonNull.INSTANCE;
        }
    }

    static JsonElement toJson(Object result) {
        return switch (result) {
            case null -> JsonNull.INSTANCE;
            case JsonElement json -> json;
            case String s -> new JsonPrimitive(s);
            case Number n -> new JsonPrimitive(n);
            case Boolean b -> new JsonPrimitive(b);
            default -> throw new IllegalStateException("A Hudsmith value must give a String, Number, Boolean, JsonElement "
                    + "or null, not " + result.getClass().getName() + ": " + GSON.toJson(result));
        };
    }
}
