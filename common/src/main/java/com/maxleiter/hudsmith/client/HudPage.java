package com.maxleiter.hudsmith.client;

import dev.vellum.mod.client.VellumPages;

/**
 * The HUD page is the player's own config/hudsmith/hud.html. Vellum serves the folder as the hudsmith_hud namespace
 * (only from the folder, never from resource packs) and reloads the page when any file it read is saved. Hudsmith's
 * own pages stay hudsmith: ids in the jar.
 */
public final class HudPage {
    public static final String NAMESPACE = "hudsmith_hud";
    public static final String URL = NAMESPACE + ":hud.html";

    private HudPage() {
    }

    /** Client setup, before the HUD overlay is registered. */
    static void register() {
        VellumPages.folder(NAMESPACE, HudFolder.dir());
    }
}
