# Hudsmith

Hudsmith draws your HUD from a web page, `hud.html` and `hud.css` in your config folder. You pick which values go on
it and style them with CSS, and the HUD reloads when you save the file.

For NeoForge and Fabric on Minecraft 26.3, 26.2 and 1.21.1. Not released yet.

![The default HUD in a plains village by the sea: FPS, coordinates, Nether coordinates, facing, biome, light and time
top left; armor and tools with their durability bottom right](docs/default.png)

## What a HUD looks like

The Minimal template, a little shortened:

<table>
<tr>
<td width="50%">

```html
<link rel="stylesheet" href="hud.css">
<script src="hudsmith:vellum/lib/hud.js"></script>

<div class="pill" v-if="!debug">
  <span>{{ bx }} {{ by }} {{ bz }}</span>
  <span>{{ biome.name }}</span>
  <span>{{ clock(time) }}</span>
</div>
```

```css
.pill {
  position: absolute; top: 4px; left: 4px;
  display: flex; gap: 8px;
  padding: 2px 6px; border-radius: 6px;
  background: rgba(0, 0, 0, 0.45);
  color: #ffffff; text-shadow: minecraft;
}
```

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/minimal/preview.png" alt="The Minimal template in game: one line at the top left with coordinates, biome and time"></td>
</tr>
</table>

`{{ bx }}` puts a value into the page, and `v-if="!debug"` leaves the line out while F3 is open. Lists repeat with
`v-for`. Pages can also run scripts and use most of CSS, flexbox and grid included. Vellum, the library that draws
them, documents [what pages can do](https://github.com/MaxLeiter/vellum/blob/main/docs/SCRIPTING.md).

## Templates

<table>
<tr>
<td width="40%">

**[Minimal](common/src/main/resources/assets/hudsmith/vellum/templates/minimal/hud.html)**

Shows your block coordinates, the biome and the time on one line in the top-left corner. It's about a dozen lines
of HTML, so it's an easy one to start editing.

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/minimal/preview.png" alt="The Minimal template: one line with coordinates, biome and time"></td>
</tr>
<tr>
<td>

**[Compass](common/src/main/resources/assets/hudsmith/vellum/templates/compass/hud.html)**

Puts a compass strip across the top of the screen that slides as you turn. Under it are your coordinates, the
biome, the time and, in the Overworld, where you'd come out in the Nether. Your armor and tools are in the
bottom-right corner.

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/compass/preview.png" alt="The Compass template: a heading strip at the top with a coordinates line under it"></td>
</tr>
<tr>
<td>

**[Survival](common/src/main/resources/assets/hudsmith/vellum/templates/survival/hud.html)**

Each row starts with an item icon. Light turns red where hostile mobs can spawn, and effects show their icon and
the time left. Every armor piece has its own durability bar.

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/survival/preview.png" alt="The Survival template: a card with icons for coordinates, day and time, biome and light, then effects with timers"></td>
</tr>
<tr>
<td>

**[Builder](common/src/main/resources/assets/hudsmith/vellum/templates/builder/hud.html)**

Shows the block you're looking at as a slowly turning 3D model, with its ID, position and block state, or an
entity's health. Below that are your exact coordinates and a small map of your spot in the chunk.

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/builder/preview.png" alt="The Builder template: cherry stairs in 3D with their state, and a chunk map with coordinates"></td>
</tr>
<tr>
<td>

**[Performance](common/src/main/resources/assets/hudsmith/vellum/templates/performance/hud.html)**

Graphs your frame rate and the server's tick time over the last 30 seconds; the page's own script draws them on a
canvas. Ping, memory use and the loaded entity count go underneath. The screenshot was taken with a software
renderer, which is why the frame rate is so low.

</td>
<td><img src="common/src/main/resources/assets/hudsmith/vellum/templates/performance/preview.png" alt="The Performance template: FPS and MSPT graphs, memory and entity counts"></td>
</tr>
</table>

## Using it

- `/hudsmith`, or the Config button in the mods list, opens the Hudsmith screen. It lists every value with what it
  is right now, and clicking one copies it ready to paste into your page. "Start from" replaces your HUD with one of
  the templates; your old files are kept as `hud.html.bak` and `hud.css.bak`.
- "Open HUD folder" opens `config/hudsmith/`. Edit `hud.html` or `hud.css` in any editor and the HUD updates when you
  save. Stylesheets, scripts and PNG images next to `hud.html` can be linked by name.
- If the page has a mistake in it, the HUD shows the error in a corner until you fix it.
- F1 hides your HUD along with the vanilla one. The templates also hide while F3 is open, since F3 shows coordinates too.
- Two key mappings, unbound to start with: show or hide the HUD, and open the Hudsmith screen.

![The Hudsmith screen: a search box, template buttons, and every value by category with a description and what it
is right now](docs/screen.png)

## Values

Numbers come as numbers and anything with parts as an object, such as `biome.name`, `mainhand.durability`,
`target.properties` or `scoreboard.scores`. `attributes` and `effects` hold everything you have, including what
other mods add.

`hudsmith:vellum/lib/hud.js` has helpers for turning values into text, such as `fmt(x)`, `chunk(x)`, `region(x, z)`,
`otherWorld(dimension, x, z)` for Nether coordinates, `clock(time)`, `heading(yaw)`, `wearColor(stack)` and `roman(n)`. Add
`<script src="hudsmith:vellum/lib/hud.js"></script>` to use them.

[Every value](docs/values.md) is listed with its shape. In game, `config/hudsmith/VALUES.md` has the same list plus
the values your other mods add.

## Requirements

| Minecraft | NeoForge | Fabric |
|---|---|---|
| 26.3 | 26.3.0.26-beta+ | Loader 0.19.5+, Fabric API |
| 26.2 | 26.2.0.88+ | Loader 0.19.5+, Fabric API |
| 1.21.1 | 21.1.255+ | Loader 0.19.5+, Fabric API |

Hudsmith needs [Vellum](https://github.com/MaxLeiter/vellum) 0.7.0 or newer. It's client-only, so servers don't
need it.

## For mod developers

Your mod can add values that players put on their HUD. Register them on the client during setup:

```java
HudsmithApi.register("mymod_mana", "My Mod", "Your mana, 0 to 100", () -> Mana.of(Minecraft.getInstance().player));
```

A value is read once a tick while a world is loaded, and returns a `String`, `Number`, `Boolean`, Gson `JsonElement`
or `null`. Start the name with your mod id. If it throws, it shows as `null` and the error is logged once.

## Building

Gradle needs JDK 25. `./gradlew build` builds every Minecraft version and loader; one target is
`./gradlew :fabric:26.2:build`, and the jars land in `<loader>/versions/<mc>/build/libs/`.
`./gradlew :neoforge:26.3:runClient` starts a dev client.

`tools/headless.sh :neoforge:26.3:runClient -Pautopilot` runs a dev client without a screen. It makes a world,
checks the HUD, the Hudsmith screen, a template swap and a live edit, saves screenshots and quits.
`-Pautopilot=screenshots` takes this README's pictures instead, into `runs/client/screenshots/`.
