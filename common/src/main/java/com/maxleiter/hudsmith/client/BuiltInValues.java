package com.maxleiter.hudsmith.client;

import java.util.Comparator;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

/**
 * The values Hudsmith comes with, read from the client alone so they work on any server. They're raw data: numbers
 * stay numbers, and things with parts are objects (an item stack, the target, an effect). Formatting and anything
 * derivable (chunk coordinates, the other dimension's coordinates, durability colors) is in the pages' library,
 * hud.js. Where the game has a registry, a value covers all of it, so modded attributes, effects and items appear
 * without code here.
 */
final class BuiltInValues {
    /** Vanilla's salt for slime chunks (Slime#checkSlimeSpawnRules). */
    private static final long SLIME_SALT = 987234911L;
    private static final int MB = 1024 * 1024;
    /** The sidebar shows at most this many scores, as vanilla's does. */
    private static final int SIDEBAR_LINES = 15;

    private static @Nullable Vec3 lastPos;
    private static double speed;

    private BuiltInValues() {
    }

    static void register() {
        String perf = "Performance";
        value("fps", perf, "Frames per second", mc -> mc.getFps());
        // The smoothed tick time F3 shows.
        value("mspt", perf, "Milliseconds the server takes per tick, in singleplayer (null on servers, which don't say)", mc -> {
            MinecraftServer server = mc.getSingleplayerServer();
            return server == null ? null : round(server.getCurrentSmoothedTickTime(), 1);
        });
        value("tps", perf, "Server ticks per second, in singleplayer (null on servers)", mc -> {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) return null;
            double mspt = Math.max(server.getCurrentSmoothedTickTime(), 1e-3);
            return round(Math.min(mc.level.tickRateManager().tickrate(), 1000.0 / mspt), 1);
        });
        value("memory", perf, "Java's memory in MB: `{used, max}`", mc -> {
            Runtime rt = Runtime.getRuntime();
            JsonObject json = new JsonObject();
            json.addProperty("used", (rt.totalMemory() - rt.freeMemory()) / MB);
            json.addProperty("max", rt.maxMemory() / MB);
            return json;
        });
        value("entities", perf, "Entities the client knows of in this dimension", mc -> mc.level.getEntityCount());

        String pos = "Position";
        value("x", pos, "Your X, to 3 decimals. `fmt(x)` shows one; `chunk(x)`, `inChunk(x)` and `region(x, z)` are in hud.js", mc -> round(mc.player.getX(), 3));
        value("y", pos, "Your Y (your feet), to 3 decimals", mc -> round(mc.player.getY(), 3));
        value("z", pos, "Your Z, to 3 decimals", mc -> round(mc.player.getZ(), 3));
        value("bx", pos, "The X of the block you stand in", mc -> mc.player.getBlockX());
        value("by", pos, "The Y of the block you stand in", mc -> mc.player.getBlockY());
        value("bz", pos, "The Z of the block you stand in", mc -> mc.player.getBlockZ());
        value("yaw", pos, "Your yaw, -180 to 180 as F3 shows it. `heading(yaw)` is 0 (north) to 360", mc -> round(Mth.wrapDegrees(mc.player.getYRot()), 1));
        value("pitch", pos, "Your pitch, -90 (up) to 90 (down)", mc -> round(mc.player.getXRot(), 1));
        value("facing", pos, "north, south, east or west. `axis(facing)` gives -Z, +Z, +X or -X", mc -> mc.player.getDirection().getName());
        value("speed", pos, "How fast you're moving across the ground, in blocks per second", mc -> round(speed, 2));
        value("dimension", pos, "The dimension's id, like `minecraft:overworld`. `otherWorld(dimension, x, z)` gives where you'd be in the Nether or Overworld",
                mc -> McClient.id(mc.level.dimension()));

        String world = "World";
        value("biome", world, "The biome you're in: `{id, name}`", mc -> {
            var key = mc.level.getBiome(mc.player.blockPosition()).unwrapKey().orElse(null);
            JsonObject json = new JsonObject();
            json.addProperty("id", key == null ? null : McClient.id(key));
            json.addProperty("name", key == null ? null : Component.translatable(McClient.location(key).toLanguageKey("biome")).getString());
            return json;
        });
        value("block_light", world, "Light from blocks where you stand, 0 to 15", mc -> mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()));
        value("sky_light", world, "Light from the sky where you stand, 0 to 15", mc -> mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()));
        value("light", world, "The light level where you stand, 0 to 15, as mobs see it now", mc -> mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()));
        value("time", world, "The time of day in ticks, 0 (sunrise) to 23999. `clock(time)` shows it like 06:00, `isNight(time)` says whether it's night",
                mc -> Math.floorMod(McClient.dayTime(mc.level), 24000));
        value("day", world, "The day count, from 0", mc -> McClient.dayTime(mc.level) / 24000);
        value("weather", world, "clear, rain or thunder", mc -> mc.level.isThundering() ? "thunder" : mc.level.isRaining() ? "rain" : "clear");
        value("difficulty", world, "The difficulty, by name", mc -> mc.level.getDifficulty().getDisplayName().getString());
        value("slime_chunk", world, "Whether slimes spawn in this chunk; singleplayer only (null on servers, which keep the seed)", BuiltInValues::slimeChunk);

        String player = "Player";
        value("health", player, "Your health, in half-hearts", mc -> round(mc.player.getHealth(), 1));
        value("max_health", player, "Your maximum health, in half-hearts", mc -> round(mc.player.getMaxHealth(), 1));
        value("food", player, "Your food level, 0 to 20", mc -> mc.player.getFoodData().getFoodLevel());
        value("saturation", player, "Your saturation, which vanilla hides", mc -> round(mc.player.getFoodData().getSaturationLevel(), 1));
        value("armor", player, "Your armor points", mc -> mc.player.getArmorValue());
        value("xp_level", player, "Your experience level", mc -> mc.player.experienceLevel);
        value("air", player, "Your air, in ticks (300 is full)", mc -> mc.player.getAirSupply());
        value("gamemode", player, "Survival, Creative, Adventure or Spectator", mc -> mc.gameMode == null ? null : mc.gameMode.getPlayerMode().getShortDisplayName().getString());
        value("sprinting", player, "Whether you're sprinting", mc -> mc.player.isSprinting());
        value("sneaking", player, "Whether you're sneaking", mc -> mc.player.isShiftKeyDown());
        value("flying", player, "Whether you're flying", mc -> mc.player.getAbilities().flying);
        value("effects", player, "Your effects, a list of `{id, name, level, duration, ticks, icon}`. `duration` reads like 1:23; `<img :src=\"effect.icon\">` draws the icon",
                BuiltInValues::effects);
        value("attributes", player, "Every attribute you have, modded ones too, by id: `attributes['minecraft:movement_speed']`",
                BuiltInValues::attributes);

        String gear = "Equipment";
        slot("mainhand", gear, "What's in your main hand, or null. An item stack: `{id, name, count, durability, max_durability, "
                + "components}`. `<item :id=\"s.id\" :count=\"s.count\" :components=\"s.components\">` draws one, and "
                + "`wearColor(s)` colors its durability", EquipmentSlot.MAINHAND);
        slot("offhand", gear, "What's in your off hand: an item stack like `mainhand`, or null", EquipmentSlot.OFFHAND);
        slot("helmet", gear, "Your helmet: an item stack like `mainhand`, or null", EquipmentSlot.HEAD);
        slot("chestplate", gear, "Your chestplate or elytra: an item stack like `mainhand`, or null", EquipmentSlot.CHEST);
        slot("leggings", gear, "Your leggings: an item stack like `mainhand`, or null", EquipmentSlot.LEGS);
        slot("boots", gear, "Your boots: an item stack like `mainhand`, or null", EquipmentSlot.FEET);
        value("hotbar", gear, "Your hotbar: 9 item stacks like `mainhand`, or null for empty slots", mc -> {
            JsonArray list = new JsonArray();
            for (int i = 0; i < Inventory.getSelectionSize(); i++) list.add(stack(mc, mc.player.getInventory().getItem(i)));
            return list;
        });
        value("selected_slot", gear, "Which hotbar slot is selected, 0 to 8", mc -> McClient.selectedSlot(mc.player.getInventory()));

        value("target", "Looking at", "What's under the crosshair, or null. A block is `{kind: 'block', id, name, state, properties, x, y, z}` "
                + "(`<model :block=\"target.state\">` draws it); an entity is `{kind: 'entity', id, name, health, max_health, x, y, z}`",
                BuiltInValues::target);

        String server = "Server";
        value("server", server, "The server's address, or null in singleplayer", mc -> {
            ServerData data = mc.getCurrentServer();
            return mc.getSingleplayerServer() != null || data == null ? null : data.ip;
        });
        value("singleplayer", server, "Whether this is a singleplayer world (or one opened to LAN)", mc -> mc.getSingleplayerServer() != null);
        value("ping", server, "Your ping to the server, in ms (null in singleplayer)", mc -> {
            if (mc.getSingleplayerServer() != null || mc.getConnection() == null) return null;
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info == null ? null : info.getLatency();
        });
        value("players", server, "Players online", mc -> mc.getConnection() == null ? null : mc.getConnection().getOnlinePlayers().size());
        value("scoreboard", server, "The sidebar the server shows, or null: `{title, scores: [{name, score, text}]}`, highest first. `text` is the score as the server formats it",
                BuiltInValues::sidebar);

        value("debug", "HUD", "Whether the F3 screen is open", mc -> McClient.debugScreenShown(mc));
    }

    /** Once a tick, before the values: things measured over time. */
    static void tick(Minecraft mc) {
        Vec3 pos = mc.player.position();
        if (lastPos != null) speed = Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z) * mc.level.tickRateManager().tickrate();
        lastPos = pos;
    }

    private static void value(String name, String category, String description, Function<Minecraft, Object> compute) {
        Values.register(name, category, description, compute);
    }

    private static void slot(String name, String category, String description, EquipmentSlot slot) {
        Values.register(name, category, description, mc -> stack(mc, mc.player.getItemBySlot(slot)));
    }

    /**
     * An item stack as pages see it: its name and durability, and its components as SNBT, which is what <item> needs
     * to draw it as it is (damage, enchantment glint, dye, trims, modded components). Null when empty.
     */
    private static @Nullable JsonObject stack(Minecraft mc, ItemStack stack) {
        if (stack.isEmpty()) return null;
        JsonObject json = new JsonObject();
        json.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("name", stack.getHoverName().getString());
        json.addProperty("count", stack.getCount());
        json.addProperty("durability", stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : null);
        json.addProperty("max_durability", stack.isDamageableItem() ? stack.getMaxDamage() : null);
        json.addProperty("components", DataComponentPatch.CODEC
                .encodeStart(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack.getComponentsPatch())
                .result().map(Tag::toString).orElse("{}"));
        return json;
    }

    private static JsonArray effects(Minecraft mc) {
        JsonArray list = new JsonArray();
        float tickrate = mc.level.tickRateManager().tickrate();
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            String id = effect.getEffect().unwrapKey().map(McClient::id).orElse(null);
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("name", effect.getEffect().value().getDisplayName().getString());
            json.addProperty("level", effect.getAmplifier() + 1);
            json.addProperty("duration", MobEffectUtil.formatDuration(effect, 1.0f, tickrate).getString());
            json.addProperty("ticks", effect.isInfiniteDuration() ? null : effect.getDuration());
            // An effect's icon is the texture named after it, modded ones too.
            json.addProperty("icon", id == null ? null : id.replace(":", ":textures/mob_effect/") + ".png");
            list.add(json);
        }
        return list;
    }

    private static JsonObject attributes(Minecraft mc) {
        JsonObject json = new JsonObject();
        McClient.attributes().forEach(attribute -> {
            if (mc.player.getAttributes().hasAttribute(attribute)) {
                attribute.unwrapKey().ifPresent(key -> json.addProperty(McClient.id(key), round(mc.player.getAttributeValue(attribute), 4)));
            }
        });
        return json;
    }

    private static @Nullable JsonObject target(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = block.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            JsonObject json = new JsonObject();
            json.addProperty("kind", "block");
            json.addProperty("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            json.addProperty("name", state.getBlock().getName().getString());
            json.addProperty("state", BlockStateParser.serialize(state));
            JsonObject properties = new JsonObject();
            for (Property<?> property : state.getProperties()) properties.addProperty(property.getName(), propertyValue(state, property));
            json.add("properties", properties);
            json.addProperty("x", pos.getX());
            json.addProperty("y", pos.getY());
            json.addProperty("z", pos.getZ());
            return json;
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            JsonObject json = new JsonObject();
            json.addProperty("kind", "entity");
            json.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            json.addProperty("name", entity.getDisplayName().getString());
            json.addProperty("health", entity instanceof LivingEntity living ? round(living.getHealth(), 1) : null);
            json.addProperty("max_health", entity instanceof LivingEntity living ? round(living.getMaxHealth(), 1) : null);
            json.addProperty("x", entity.getBlockX());
            json.addProperty("y", entity.getBlockY());
            json.addProperty("z", entity.getBlockZ());
            return json;
        }
        return null;
    }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /**
     * The sidebar as vanilla draws it: scores not hidden, highest first, at most 15, names with their team's prefix
     * and suffix, and the score as the objective formats it (servers often blank it or replace it with text).
     */
    private static @Nullable JsonObject sidebar(Minecraft mc) {
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return null;
        NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        JsonArray scores = new JsonArray();
        scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(SIDEBAR_LINES)
                .forEach(entry -> {
                    JsonObject line = new JsonObject();
                    line.addProperty("name", PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString());
                    line.addProperty("score", entry.value());
                    line.addProperty("text", entry.formatValue(format).getString());
                    scores.add(line);
                });
        JsonObject json = new JsonObject();
        json.addProperty("title", objective.getDisplayName().getString());
        json.add("scores", scores);
        return json;
    }

    private static @Nullable Boolean slimeChunk(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return null;
        if (mc.level.dimension() != Level.OVERWORLD) return false;
        long seed = server.overworld().getSeed();
        int cx = SectionPos.blockToSectionCoord(mc.player.getBlockX());
        int cz = SectionPos.blockToSectionCoord(mc.player.getBlockZ());
        return WorldgenRandom.seedSlimeChunk(cx, cz, seed, SLIME_SALT).nextInt(10) == 0;
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
