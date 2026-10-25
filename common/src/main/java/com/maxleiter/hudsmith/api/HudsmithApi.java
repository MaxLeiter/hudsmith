package com.maxleiter.hudsmith.api;

import java.util.function.Supplier;

import com.maxleiter.hudsmith.client.Values;

/**
 * For other mods: add values that players can put on their HUD. Call it on the client during setup (before the first
 * world loads), from code that only runs when Hudsmith is installed.
 *
 * <pre>{@code
 * HudsmithApi.register("mymod_mana", "My Mod", "Your mana, 0 to 100", () -> Mana.of(player()));
 * }</pre>
 * A page shows it as {@code {{ mymod_mana }}}. Prefix names with your mod id so they don't clash with Hudsmith's or
 * another mod's.
 */
public final class HudsmithApi {
    private HudsmithApi() {
    }

    /**
     * @param name        snake_case, starting with your mod id
     * @param category    a heading for it on the Hudsmith screen and in VALUES.md, usually your mod's name
     * @param description one short sentence for players
     * @param value       called on the render thread once a tick while a world is loaded; returns a String, Number,
     *                    Boolean, Gson JsonElement or null (nothing to show). If it throws, the value shows as null
     *                    and the error is logged once.
     * @throws IllegalArgumentException if the name isn't snake_case or is taken
     */
    public static void register(String name, String category, String description, Supplier<?> value) {
        Values.register(name, category, description, mc -> value.get());
    }
}
