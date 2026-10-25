package com.maxleiter.hudsmith.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.maxleiter.hudsmith.Hudsmith;

import dev.vellum.mod.client.VellumAutomation;
import dev.vellum.mod.client.VellumHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;

/**
 * Dev-only checks, driven by {@code -Pautopilot} on any loader and version ({@code -Dhudsmith.autopilot} in the game).
 * A step that can't do its job fails the run: the log ends with "FAILED". Screenshots go to
 * {@code runs/client/screenshots/}.
 * <ul>
 * <li>{@code -Pautopilot}: a creative superflat world with damaged armor; checks the HUD shows its values and loaded
 * hud.js, the Hudsmith screen lists and searches every value, a template swap and a live edit of hud.html.</li>
 * <li>{@code -Pautopilot=screenshots}: the README's pictures, one staged scene per template in seed 42
 * (template-&lt;name&gt;.png), then the Hudsmith screen (values.png).</li>
 * <li>{@code -Pautopilot=locate}: runs {@code -Phudsmith.autopilot.locate} commands in the screenshot world (results
 * in the log), and screenshots {@code -Phudsmith.autopilot.explore} views, for finding places to stage scenes.</li>
 * </ul>
 */
public final class DevAutopilot {
    private static final String MODE = System.getProperty("hudsmith.autopilot");
    public static final boolean ENABLED = MODE != null;
    private static final boolean SCREENSHOTS = "screenshots".equals(MODE);
    private static final boolean LOCATE = "locate".equals(MODE);
    private static final long SEED = Long.getLong("hudsmith.autopilot.seed", 42L);
    private static final String WORLD = "hudsmith-autopilot";
    private static final int MAX_POLL = 400;

    private static boolean started, loaded, finished;
    private static final Deque<Runnable> steps = new ArrayDeque<>();
    private static int wait;
    private static int failures;
    /** The template overlay on screen in screenshot mode. */
    private static @Nullable Identifier shownTemplate;
    /** Gives the shown template overlay the values, as the HUD gets them. */
    private static final Consumer<@Nullable JsonObject> TEMPLATE_VALUES = values -> {
        if (shownTemplate != null && values != null) VellumHud.push(shownTemplate, values);
    };

    private DevAutopilot() {
    }

    /** Client setup: in screenshot mode, an overlay per template (Vellum wants them registered before loading ends). */
    static void init() {
        if (!SCREENSHOTS) return;
        for (String name : HudFolder.TEMPLATES) VellumHud.register(template(name), "hudsmith:vellum/templates/" + name + "/hud.html");
        HudsmithClient.addListener(TEMPLATE_VALUES);
    }

    private static Identifier template(String name) {
        return Hudsmith.id("template_" + name);
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED || finished) return;
        if (!started) {
            if (!(McClient.screen() instanceof TitleScreen || McClient.screen() instanceof AccessibilityOnboardingScreen)) return;
            started = true;
            log("creating world " + WORLD);
            deleteWorld(mc);
            mc.options.onboardingAccessibilityFinished();
            mc.options.pauseOnLostFocus = false;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(Options.UNLIMITED_FRAMERATE_CUTOFF);
            boolean normal = SCREENSHOTS || LOCATE;
            //? if >=26 {
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(normal ? SEED : 0, normal, false),
                    normal ? WorldPresets::createNormalWorldDimensions : WorldPresets::createTestWorldDimensions, McClient.screen());
            //?} else {
            /*LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(normal ? SEED : 0, normal, false),
                    normal ? WorldPresets::createNormalWorldDimensions
                            : registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), McClient.screen());
            *///?}
            return;
        }
        if (!loaded) {
            if (mc.player == null || mc.level == null) return;
            loaded = true;
            until("the player stands on the ground", () -> mc.player.onGround(), 1);
            steps.add(() -> setUp(mc));
            if (LOCATE) planLocate(mc);
            else if (SCREENSHOTS) planScreenshots(mc);
            else plan(mc);
            // Chunks around the spawn (a normal world takes a while to draw on software rendering).
            wait = normalWorld() ? 300 : 100;
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Runnable step = steps.poll();
        if (step == null) {
            finished = true;
            if (failures == 0) log("finished, no failures");
            else Hudsmith.LOG.error("Hudsmith autopilot: FAILED, {} failed steps", failures);
            mc.stop();
            return;
        }
        try {
            step.run();
        } catch (RuntimeException e) {
            fail("a step threw", e);
        }
    }

    private static boolean normalWorld() {
        return SCREENSHOTS || LOCATE;
    }

    /** Starts from a fresh world: an earlier run's would still have its scenes in it. */
    private static void deleteWorld(Minecraft mc) {
        LevelStorageSource source = mc.getLevelSource();
        if (!source.levelExists(WORLD)) return;
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
            access.deleteLevel();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete the old autopilot world " + WORLD, e);
        }
    }

    /** What every mode starts with: this build's default HUD (the run directory may hold an older one) and a GUI scale. */
    private static void setUp(Minecraft mc) {
        HudFolder.useTemplate("default");
        mc.options.guiScale().set(2);
        if (normalWorld()) {
            // Software rendering draws a far view at a few frames a second.
            mc.options.renderDistance().set(8);
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        }
        //? if >=26 {
        mc.resizeGui();
        //?} else
        /*mc.resizeDisplay();*/
    }

    private static void plan(Minecraft mc) {
        steps.add(() -> run(mc, "time set 1000",
                "item replace entity @s armor.head with minecraft:iron_helmet",
                "item replace entity @s armor.chest with minecraft:diamond_chestplate",
                "item replace entity @s armor.legs with minecraft:golden_leggings",
                "item replace entity @s armor.feet with minecraft:netherite_boots",
                "item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe",
                "item replace entity @s weapon.offhand with minecraft:torch 32",
                "effect give @s minecraft:speed 90 1"));
        // Wear on the gear, so the durability colors show.
        steps.add(() -> {
            damage(mc, EquipmentSlot.HEAD, 0.9);
            damage(mc, EquipmentSlot.LEGS, 0.6);
            damage(mc, EquipmentSlot.MAINHAND, 0.3);
            wait = 40;
        });
        until("the HUD page shows the values", () -> hudText(".info").contains("XYZ") && hudText(".info").contains("Biome"), 20);
        steps.add(() -> {
            log("HUD text: " + hudText(".info").replaceAll("\\s+", " "));
            check(!hudText(".info").contains("{{"), "the HUD has no raw template text");
            check(VellumAutomation.hud(HudsmithClient.HUD).flatMap(page -> page.eval("typeof fmt"))
                    .map(t -> t.getAsString().equals("function")).orElse(false), "the HUD loaded hud.js");
        });
        shot(mc, "hud");

        openScreen();
        steps.add(() -> VellumAutomation.screen().ifPresent(page -> page.type("dura")));
        until("searching narrows the list", () -> rows() > 0 && rows() < Values.all().size(), 5);
        for (int i = 0; i < "dura".length(); i++) steps.add(() -> VellumAutomation.screen().ifPresent(page -> page.key("Backspace")));
        steps.add(() -> VellumAutomation.screen().ifPresent(VellumAutomation::leave));
        steps.add(() -> wait = 20);
        shot(mc, "values");

        // Templates and live edits: the HUD is config/hudsmith/hud.html, which Vellum reloads when it changes.
        steps.add(() -> check(VellumAutomation.screen().map(page -> page.click(".templates button:nth-of-type(2)")).orElse(false),
                "clicked the Minimal template"));
        steps.add(() -> {
            McClient.setScreen(null);
            wait = 10;
        });
        until("the HUD is the Minimal template", () -> VellumAutomation.hud(HudsmithClient.HUD)
                .map(page -> page.exists(".pill")).orElse(false), 5);
        steps.add(() -> {
            Path page = HudFolder.dir().resolve("hud.html");
            try {
                Files.writeString(page, Files.readString(page).replace("{{ biome.name }}", "{{ biome.name }} (edited)"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        until("the HUD reloads with the edit", () -> hudText(".pill").contains("(edited)"), 5);
        steps.add(() -> HudFolder.useTemplate("default"));
        until("the HUD is the default again", () -> VellumAutomation.hud(HudsmithClient.HUD)
                .map(page -> page.exists(".info")).orElse(false), 5);
    }

    /** Opens the Hudsmith screen and waits until it lists every value. */
    private static void openScreen() {
        steps.add(() -> ValuesScreen.open(null));
        until("the Hudsmith screen lists every value", () -> rows() == Values.all().size(), 10);
    }

    /** The rows the open Hudsmith screen lists, once it has settled; -1 before. */
    private static int rows() {
        return VellumAutomation.screen().filter(VellumAutomation::settled)
                .flatMap(page -> page.eval("document.querySelectorAll('.row').length"))
                .map(n -> n.getAsInt()).orElse(-1);
    }

    private static String hudText(String selector) {
        return VellumAutomation.hud(HudsmithClient.HUD).flatMap(page -> page.text(selector)).orElse("");
    }

    private static void planLocate(Minecraft mc) {
        String[] commands = System.getProperty("hudsmith.autopilot.locate",
                "locate structure minecraft:village_plains;locate biome minecraft:cherry_grove;locate biome minecraft:plains")
                .split(";");
        steps.add(() -> {
            var server = mc.getSingleplayerServer();
            for (String command : commands) {
                log("server: " + command);
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
            }
            wait = 100;
        });
        String explore = System.getProperty("hudsmith.autopilot.explore", "");
        if (explore.isBlank()) return;
        steps.add(() -> run(mc, "time set 1500"));
        String[] views = explore.split("\\|");
        for (int i = 0; i < views.length; i++) {
            goTo(mc, views[i].trim());
            steps.add(() -> wait = 300);
            clean(mc);
            shot(mc, "explore-" + i);
        }
    }

    /**
     * A staged shot: the template, where to stand (see {@link #goTo}), the time, game mode, what's worn and in hand,
     * and commands that build the scene around the player (run as the player, so {@code ~} and {@code ^} are relative
     * to them).
     */
    private record Scene(String template, String where, int time, String gamemode,
                         String mainhand, String offhand, String[] armor, String[] effects, String[] build) {
    }

    private static final String[] IRON = {"minecraft:iron_helmet[damage=140]", "minecraft:iron_chestplate[damage=60]",
            "minecraft:iron_leggings[damage=95]", "minecraft:iron_boots[damage=20]"};
    private static final String[] NONE = {};

    /**
     * Seed 42: a plains village on the coast at (656, -304), plains at (352, -352), a dark forest at the spawn (its
     * ground at -29.5 73 3.5). Things the scenes summon are tagged hudsmith, and cleared before the next scene.
     */
    private static List<Scene> scenes() {
        return List.of(
                // The default HUD on the hill above the village in the morning: the well, a house, the sea.
                new Scene("default", "640 -300 -90 8", 1000, "survival", "minecraft:iron_sword[damage=80]",
                        "minecraft:shield", IRON, NONE, NONE),
                // Minimal: looking out over the village and the sea at sunset.
                new Scene("minimal", "640 100 -340 -45 18", 12200, "survival",
                        "minecraft:fishing_rod[damage=12]", "minecraft:air", IRON, NONE, NONE),
                // Compass: flying high on an elytra, rockets in hand.
                new Scene("compass", "600 150 -390 -40 26", 3000, "survival",
                        "minecraft:firework_rocket 23", "minecraft:compass", new String[] {"minecraft:diamond_helmet[damage=210]",
                        "minecraft:elytra[damage=231]", "minecraft:diamond_leggings[damage=90]", "minecraft:diamond_boots[damage=40]"},
                        NONE, NONE),
                // Survival: the dark forest at nightfall, zombies by a campfire, where mobs can spawn.
                new Scene("survival", "-29.5 73 3.5 -80 4", 13800, "survival",
                        "minecraft:iron_pickaxe[damage=190]", "minecraft:torch 41", IRON,
                        new String[] {"minecraft:regeneration 45 1", "minecraft:absorption 110 0", "minecraft:haste 200 0"},
                        new String[] {
                                "difficulty easy",
                                "execute positioned ^3 ^ ^10 positioned over motion_blocking_no_leaves run setblock ~ ~ ~ minecraft:campfire",
                                "execute positioned ^-2 ^ ^7 positioned over motion_blocking_no_leaves run summon minecraft:zombie ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Rotation:[100f,0f],Tags:[\"hudsmith\"]}",
                                "execute positioned ^1 ^ ^12 positioned over motion_blocking_no_leaves run summon minecraft:zombie ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Rotation:[80f,0f],Tags:[\"hudsmith\"]}",
                                "execute positioned ^5 ^ ^9 positioned over motion_blocking_no_leaves run summon minecraft:skeleton ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Rotation:[120f,0f],Tags:[\"hudsmith\"]}",
                        }),
                // Builder: a cherry cabin going up, looking at the step to its door. A fixed spot on the plains' ground
                // (y 82), since spreadplayers can land half a block off and miss the step.
                new Scene("builder", "353.5 83 -351.5 -90 28", 5000, "creative",
                        "minecraft:cherry_stairs 64", "minecraft:cherry_planks 64", NONE, NONE,
                        new String[] {
                                "fill ~2 ~ ~-5 ~14 ~8 ~5 minecraft:air",
                                "fill ~4 ~-1 ~-3 ~11 ~-1 ~3 minecraft:cherry_planks",
                                "fill ~5 ~ ~-3 ~11 ~3 ~3 minecraft:cherry_planks hollow",
                                "fill ~5 ~ ~-3 ~5 ~3 ~-3 minecraft:cherry_log",
                                "fill ~5 ~ ~3 ~5 ~3 ~3 minecraft:cherry_log",
                                "fill ~11 ~ ~-3 ~11 ~3 ~-3 minecraft:cherry_log",
                                "fill ~11 ~ ~3 ~11 ~3 ~3 minecraft:cherry_log",
                                "setblock ~5 ~ ~0 minecraft:cherry_door[facing=east,half=lower]",
                                "setblock ~5 ~1 ~0 minecraft:cherry_door[facing=east,half=upper]",
                                "setblock ~5 ~1 ~-2 minecraft:glass_pane",
                                "setblock ~5 ~1 ~2 minecraft:glass_pane",
                                "fill ~4 ~-1 ~-1 ~4 ~-1 ~1 minecraft:cherry_stairs[facing=east]",
                                "setblock ~4 ~ ~-2 minecraft:lantern",
                                "fill ~4 ~4 ~-4 ~4 ~4 ~4 minecraft:cherry_stairs[facing=east]",
                                "fill ~5 ~5 ~-4 ~5 ~5 ~4 minecraft:cherry_stairs[facing=east]",
                                "fill ~6 ~6 ~-4 ~6 ~6 ~0 minecraft:cherry_stairs[facing=east]",
                                "fill ~12 ~ ~-4 ~12 ~6 ~-4 minecraft:scaffolding",
                                "setblock ~2 ~ ~-3 minecraft:crafting_table",
                                "setblock ~2 ~ ~-4 minecraft:chest[facing=east]",
                        }),
                // Performance: a pen full of animals by the village.
                new Scene("performance", "330 -330 -90 14", 6000, "survival",
                        "minecraft:wheat 37", "minecraft:shears[damage=64]", IRON, NONE, pen()));
    }

    /** A fenced pen in front of the player, and animals in it. */
    private static String[] pen() {
        List<String> commands = new ArrayList<>(List.of(
                "fill ~2 ~ ~-8 ~22 ~4 ~8 minecraft:air",
                "fill ~6 ~ ~-7 ~21 ~ ~-7 minecraft:oak_fence",
                "fill ~6 ~ ~7 ~21 ~ ~7 minecraft:oak_fence",
                "fill ~21 ~ ~-7 ~21 ~ ~7 minecraft:oak_fence",
                "fill ~6 ~ ~-7 ~6 ~ ~7 minecraft:oak_fence",
                "setblock ~6 ~ ~0 minecraft:oak_fence_gate[facing=east]",
                "setblock ~4 ~ ~-7 minecraft:hay_block",
                "setblock ~4 ~ ~-8 minecraft:hay_block",
                "setblock ~4 ~1 ~-8 minecraft:hay_block"));
        String[] animals = {"sheep", "sheep", "sheep", "cow", "pig", "chicken", "sheep", "cow"};
        Random random = new Random(42);
        for (int i = 0; i < 36; i++) {
            commands.add(String.format(Locale.ROOT,
                    "execute positioned ~%d ~ ~%d positioned over motion_blocking_no_leaves run summon minecraft:%s ~ ~ ~ {Rotation:[%df,0f],Tags:[\"hudsmith\"]}",
                    8 + random.nextInt(12), -5 + random.nextInt(11), animals[i % animals.length], random.nextInt(360)));
        }
        return commands.toArray(String[]::new);
    }

    /** The README's pictures: each scene with its template, then the Hudsmith screen over the default HUD. */
    private static void planScreenshots(Minecraft mc) {
        steps.add(() -> {
            HudsmithClient.setShown(false);
            run(mc, "weather clear 100000",
                    "item replace entity @s hotbar.1 with minecraft:iron_pickaxe[damage=40]",
                    "item replace entity @s hotbar.2 with minecraft:iron_axe[damage=12]",
                    "item replace entity @s hotbar.3 with minecraft:iron_shovel",
                    "item replace entity @s hotbar.4 with minecraft:cooked_beef 14",
                    "item replace entity @s hotbar.5 with minecraft:torch 41",
                    "item replace entity @s hotbar.6 with minecraft:cobblestone 57",
                    "item replace entity @s hotbar.7 with minecraft:water_bucket",
                    "item replace entity @s hotbar.8 with minecraft:oak_log 23");
        });
        for (Scene scene : scenes()) stage(mc, scene);

        steps.add(() -> {
            if (shownTemplate != null) VellumHud.hide(shownTemplate);
            shownTemplate = null;
            HudsmithClient.setShown(true);
        });
        openScreen();
        steps.add(() -> VellumAutomation.screen().ifPresent(VellumAutomation::leave));
        steps.add(() -> wait = 20);
        shot(mc, "values");
    }

    private static void stage(Minecraft mc, Scene scene) {
        steps.add(() -> {
            log("staging " + scene.template());
            List<String> commands = new ArrayList<>(List.of("kill @e[tag=hudsmith]", "effect clear @s",
                    "gamemode " + scene.gamemode(), "time set " + scene.time(),
                    "item replace entity @s weapon.mainhand with " + scene.mainhand(),
                    "item replace entity @s weapon.offhand with " + scene.offhand()));
            String[] slots = {"armor.head", "armor.chest", "armor.legs", "armor.feet"};
            for (int i = 0; i < slots.length; i++) {
                commands.add("item replace entity @s " + slots[i] + " with " + (i < scene.armor().length ? scene.armor()[i] : "minecraft:air"));
            }
            for (String effect : scene.effects()) commands.add("effect give @s " + effect + " true");
            run(mc, commands.toArray(String[]::new));
        });
        goTo(mc, scene.where());
        // The chunks load and draw before anything is built in them.
        steps.add(() -> wait = 200);
        steps.add(() -> run(mc, scene.build()));
        steps.add(() -> wait = 100);
        if (scene.template().equals("builder")) {
            until("the player looks at the cabin's step", () -> HudsmithClient.values() != null
                    && HudsmithClient.values().get("target") instanceof JsonObject target
                    && "minecraft:cherry_stairs".equals(GsonHelper.getAsString(target, "id", null)), 1);
        }
        // The performance template's graphs get half a minute of history.
        shootTemplate(mc, scene.template(), scene.template().equals("performance") ? 600 : 40);
    }

    /**
     * Steps that put the player at a view: "x z yaw pitch" stands on the ground there (spreadplayers), "x y z yaw
     * pitch" stands at that height on a barrier block (invisible), so it stays put in the air or on uneven ground.
     */
    private static void goTo(Minecraft mc, String where) {
        String[] at = where.split(" ");
        boolean exact = at.length == 5;
        int x = (int) Math.floor(Double.parseDouble(at[0]));
        int z = (int) Math.floor(Double.parseDouble(at[exact ? 2 : 1]));
        steps.add(() -> run(mc, "forceload add " + x + " " + z));
        if (exact) {
            int y = (int) Math.floor(Double.parseDouble(at[1]));
            steps.add(() -> run(mc, "setblock " + x + " " + (y - 1) + " " + z + " minecraft:barrier", "tp @s " + where));
        } else {
            steps.add(() -> run(mc, "spreadplayers " + at[0] + " " + at[1] + " 0 1 false @s", "tp @s ~ ~ ~ " + at[2] + " " + at[3]));
        }
    }

    /** Commands, one a tick, at the front of the queue. */
    private static void run(Minecraft mc, String... commands) {
        for (int i = commands.length - 1; i >= 0; i--) {
            String command = commands[i];
            steps.addFirst(() -> {
                mc.player.connection.sendCommand(command);
                wait = 1;
            });
        }
    }

    /**
     * Shows a template's overlay in place of the HUD, gives it {@code settle} ticks, and screenshots it as
     * template-&lt;name&gt;.png.
     */
    private static void shootTemplate(Minecraft mc, String name, int settle) {
        steps.add(() -> {
            if (shownTemplate != null) VellumHud.hide(shownTemplate);
            shownTemplate = template(name);
            // Values before the first frame, as the HUD gets them: a page drawn without them has no values to name.
            if (HudsmithClient.values() != null) VellumHud.push(shownTemplate, HudsmithClient.values());
            VellumHud.show(shownTemplate);
            wait = settle;
        });
        // Not settled(): an overlay that gets values every tick never is.
        until("template " + name + " shows", () -> VellumAutomation.hud(template(name))
                .map(page -> page.exists("div")).orElse(false), 20);
        clean(mc);
        shot(mc, "template-" + name);
    }

    /** No toasts, chat or dropped items in the shot. */
    private static void clean(Minecraft mc) {
        steps.add(() -> {
            mc.player.connection.sendCommand("kill @e[type=item]");
            McClient.clearToasts(mc);
            wait = 10;
        });
    }

    /** Uses up {@code fraction} of the durability of the item in a slot, on the server's copy too. */
    private static void damage(Minecraft mc, EquipmentSlot slot, double fraction) {
        var server = mc.getSingleplayerServer();
        var player = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> {
            var stack = player.getItemBySlot(slot);
            stack.setDamageValue((int) (stack.getMaxDamage() * fraction));
        });
    }

    private static void until(String what, BooleanSupplier done, int minTicks) {
        steps.add(new Runnable() {
            private int ticks;

            @Override
            public void run() {
                ticks++;
                if (ticks >= minTicks && done.getAsBoolean()) log(what + " (" + ticks + " ticks)");
                else if (ticks < MAX_POLL) steps.addFirst(this);
                else fail("gave up waiting until " + what, null);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        steps.add(() -> {
            //? if >=26 {
            Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> log(msg.getString()));
            //?} else
            /*Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> log(msg.getString()));*/
            wait = 10;
        });
    }

    private static void check(boolean ok, String what) {
        if (ok) log("ok: " + what);
        else fail("check failed: " + what, null);
    }

    private static void fail(String what, @Nullable Throwable cause) {
        failures++;
        if (cause == null) Hudsmith.LOG.error("Hudsmith autopilot: {}", what);
        else Hudsmith.LOG.error("Hudsmith autopilot: {}", what, cause);
    }

    private static void log(String message) {
        Hudsmith.LOG.info("Hudsmith autopilot: {}", message);
    }
}
