# Hudsmith

A client-only HUD mod: the HUD is a Vellum page (HTML/CSS) that gets every value (coordinates, biome, durability...)
as a top-level data key, once a tick. NeoForge + Fabric, Minecraft 26.3 / 26.2 / 1.21.1 from one source tree.

## Layout
- `common/` vanilla-only code, compiled into both loader jars (MultiLoader-Template style). `neoforge/`, `fabric/`
  hold entrypoints and the `Platform` service (`META-INF/services`).
- Stonecutter: subprojects are `:common:26.3`, `:neoforge:26.2`, `:fabric:1.21.1`... Sources are written for **26.3**
  (the vcs version). Differences use `//? if >=26 {` comments, the regex renames in `stonecutter.gradle`
  (`Identifier`/`ResourceLocation`, `GuiGraphicsExtractor`/`GuiGraphics`, Fabric API's key mapping and command
  classes), or files under
  `common/versions/<mc>/src`. Keep `stonecutter.active '26.3'` when committing.
- Version shims: `client/McClient`. Put new version differences there when they are small.
- Values: `client/BuiltInValues` (Hudsmith's), `api/HudsmithApi` (other mods'), `client/Values` (the registry). A
  value's name is a page identifier, so snake_case; every value is in every update, null when empty. Values are raw
  data (numbers, objects); formatting and anything derivable belongs in the pages' library
  `assets/hudsmith/vellum/lib/hud.js`, not in a new value. Prefer one generic value over a registry (attributes,
  effects, the hotbar) to one value per thing.
- Pages, in `assets/hudsmith/vellum/`: `templates/<name>/hud.{html,css}` are the HUDs players start from, `default`
  (copied to `config/hudsmith/` on first run) and the README's templates, each with a `preview.png` (left out of the
  jars); `values.{html,css}` is the Hudsmith screen (`client/ValuesScreen`); `lib/hud.js` the pages' helpers. Add a
  template to `HudFolder.TEMPLATES`, a scene for it in `DevAutopilot.scenes()`, and a row in the README's table.
  Resource packs and mods add templates in `assets/<namespace>/hudsmith/templates/<name>/` (`HudFolder.packTemplates`);
  picking one copies its .html, .css, .js and .png files into the folder.
- `HudsmithClient` computes the values once a tick and pushes them to the HUD and to its listeners (the open screen,
  the autopilot's template overlays); nothing else computes them.
- README pictures: `tools/headless.sh :neoforge:26.3:runClient -Pautopilot=screenshots` (seed 42, a staged scene per
  shot; `-Pautopilot=locate -Phudsmith.autopilot.explore=...` finds new spots), then
  copy `template-default.png` to `docs/default.png`, `values.png` to `docs/screen.png`, the other
  `template-<name>.png` to `templates/<name>/preview.png`, and `runs/client/config/hudsmith/VALUES.md` to
  `docs/values.md`.
- The HUD is the player's `config/hudsmith/hud.html`: `client/HudPage` registers the folder with Vellum 0.7.0's
  `VellumPages.folder("hudsmith_hud", ...)`, so the page is `hudsmith_hud:hud.html`, read only from the folder and
  reloaded when a file it read is saved. Hudsmith's own `hudsmith:` pages stay in the jar.
  `HudFolder.useTemplate` copies a template over it (the screen's "Start from" buttons), keeping `.bak` copies.
- Per-version build settings: `stonecutter.properties.toml`. Shared: `gradle.properties`.

## Building
- Gradle needs JDK 25+.
- Vellum (`dev.vellum:vellum-<loader>-<mc>`) comes from https://maven.maxleiter.com; `vellum_version` is per
  Minecraft version in stonecutter.properties.toml. An unreleased Vellum resolves from mavenLocal
  (`./gradlew publishToMavenLocal` in a Vellum checkout). Vellum is a required mod, never bundled.
- `./gradlew build` builds every version and loader. One target: `./gradlew :fabric:26.2:build`. Add
  `--configure-on-demand` to only configure what you run.
- Run: `./gradlew :neoforge:26.3:runClient`. Headless (cloud sessions): `tools/headless.sh :neoforge:26.3:runClient
  -Pautopilot` makes a world, checks the HUD and the Hudsmith screen, screenshots them into
  `<loader>/versions/<mc>/runs/client/screenshots/` and quits; the log says "finished, no failures" or "FAILED".
- Jars: `<loader>/versions/<mc>/build/libs/hudsmith-<loader>-<mc>-<version>.jar`.

## Conventions
- No fallbacks or simulated behavior that hide a failure: if something can't work, fail loudly.
- Comments explain non-obvious behavior only.
- American spelling everywhere (color, armor, center).
- No middle dot characters in the README, docs or example pages (the default HUD and the templates): separate with
  layout (a gap) or ordinary punctuation.
- Messages from pages (`vellum.send`) are untrusted input (a resource pack can replace the page): validate.
- Values only read what the client already has; nothing here talks to the server.

## Releasing
Bump `version` in gradle.properties, add a `## <version>` section to CHANGELOG.md, commit, then
`./bump_release.sh <version>` (or tag `v<version>` and push). `.github/workflows/release.yml` builds, makes the GitHub
release and uploads to Modrinth/CurseForge (needs `modrinth_id`/`curseforge_id` in gradle.properties and the
`MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` secrets). Dry run: `./gradlew publishMods -PpublishDryRun`.
