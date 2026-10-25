# Hudsmith values

Every value your HUD page can use. Hudsmith writes this file each time the game starts, so edits are
lost. Use a value by name: `{{ fps }}`, `<div v-if="slime_chunk">`. A value with nothing to show is
`null`. Functions like `fmt(x)` come from `hudsmith:vellum/lib/hud.js`. The Hudsmith screen (`/hudsmith`)
shows what each value is right now.

## Performance

| Name | |
|---|---|
| `fps` | Frames per second |
| `mspt` | Milliseconds the server takes per tick, in singleplayer (null on servers, which don't say) |
| `tps` | Server ticks per second, in singleplayer (null on servers) |
| `memory` | Java's memory in MB: `{used, max}` |
| `entities` | Entities the client knows of in this dimension |

## Position

| Name | |
|---|---|
| `x` | Your X, to 3 decimals. `fmt(x)` shows one; `chunk(x)`, `inChunk(x)` and `region(x, z)` are in hud.js |
| `y` | Your Y (your feet), to 3 decimals |
| `z` | Your Z, to 3 decimals |
| `bx` | The X of the block you stand in |
| `by` | The Y of the block you stand in |
| `bz` | The Z of the block you stand in |
| `yaw` | Your yaw, -180 to 180 as F3 shows it. `heading(yaw)` is 0 (north) to 360 |
| `pitch` | Your pitch, -90 (up) to 90 (down) |
| `facing` | north, south, east or west. `axis(facing)` gives -Z, +Z, +X or -X |
| `speed` | How fast you're moving across the ground, in blocks per second |
| `dimension` | The dimension's id, like `minecraft:overworld`. `otherWorld(dimension, x, z)` gives where you'd be in the Nether or Overworld |

## World

| Name | |
|---|---|
| `biome` | The biome you're in: `{id, name}` |
| `block_light` | Light from blocks where you stand, 0 to 15 |
| `sky_light` | Light from the sky where you stand, 0 to 15 |
| `light` | The light level where you stand, 0 to 15, as mobs see it now |
| `time` | The time of day in ticks, 0 (sunrise) to 23999. `clock(time)` shows it like 06:00, `isNight(time)` says whether it's night |
| `day` | The day count, from 0 |
| `weather` | clear, rain or thunder |
| `difficulty` | The difficulty, by name |
| `slime_chunk` | Whether slimes spawn in this chunk; singleplayer only (null on servers, which keep the seed) |

## Player

| Name | |
|---|---|
| `health` | Your health, in half-hearts |
| `max_health` | Your maximum health, in half-hearts |
| `food` | Your food level, 0 to 20 |
| `saturation` | Your saturation, which vanilla hides |
| `armor` | Your armor points |
| `xp_level` | Your experience level |
| `air` | Your air, in ticks (300 is full) |
| `gamemode` | Survival, Creative, Adventure or Spectator |
| `sprinting` | Whether you're sprinting |
| `sneaking` | Whether you're sneaking |
| `flying` | Whether you're flying |
| `effects` | Your effects, a list of `{id, name, level, duration, ticks, icon}`. `duration` reads like 1:23; `<img :src="effect.icon">` draws the icon |
| `attributes` | Every attribute you have, modded ones too, by id: `attributes['minecraft:movement_speed']` |

## Equipment

| Name | |
|---|---|
| `mainhand` | What's in your main hand, or null. An item stack: `{id, name, count, durability, max_durability, components}`. `<item :id="s.id" :count="s.count" :components="s.components">` draws one, and `wearColor(s)` colors its durability |
| `offhand` | What's in your off hand: an item stack like `mainhand`, or null |
| `helmet` | Your helmet: an item stack like `mainhand`, or null |
| `chestplate` | Your chestplate or elytra: an item stack like `mainhand`, or null |
| `leggings` | Your leggings: an item stack like `mainhand`, or null |
| `boots` | Your boots: an item stack like `mainhand`, or null |
| `hotbar` | Your hotbar: 9 item stacks like `mainhand`, or null for empty slots |
| `selected_slot` | Which hotbar slot is selected, 0 to 8 |

## Looking at

| Name | |
|---|---|
| `target` | What's under the crosshair, or null. A block is `{kind: 'block', id, name, state, properties, x, y, z}` (`<model :block="target.state">` draws it); an entity is `{kind: 'entity', id, name, health, max_health, x, y, z}` |

## Server

| Name | |
|---|---|
| `server` | The server's address, or null in singleplayer |
| `singleplayer` | Whether this is a singleplayer world (or one opened to LAN) |
| `ping` | Your ping to the server, in ms (null in singleplayer) |
| `players` | Players online |
| `scoreboard` | The sidebar the server shows, or null: `{title, scores: [{name, score, text}]}`, highest first. `text` is the score as the server formats it |

## HUD

| Name | |
|---|---|
| `debug` | Whether the F3 screen is open |
