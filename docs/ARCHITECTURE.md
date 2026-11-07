# Architecture

Technical routing for the current implementation, checked against game `0.98a`. Code paths below are relative to `jars/src/illustratedEntities/`; data paths are repository-relative. Search the named owner before adding another implementation.

| Reference | Scope |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | Workflow, build gate and document upkeep |
| [README.md](../README.md) | Player overview, settings, `image_data.csv` format for adding images |
| [RELEASE.md](RELEASE.md) | Version numbers, release builder, package contents and publishing |
| [CHANGELOG.md](CHANGELOG.md) | Change-note format and upkeep for every implementation task |

## Start here

| Change / symptom | Route |
|---|---|
| Wrong or no image picked for a planet | `ImagePicker.getTags -> ImageTags maps -> ImagePicker.generateChoices -> ImageHandler.applyFittingRandomImageToEntity` |
| Preset image for a named vanilla planet | `image_data.csv` `planet_id` -> `ImageHandler.addPresetImagesToMarkets`; skips or reverts entities with a vanilla illustration unless `Setting.PRESET_REPLACE_VANILLA`; skips entities with `$illent_player_pick` |
| Image missing or white after a jump or reload | `ImageOnJumpLoader` / `PlayerOpenColonyListener` -> `ImageDataEntry.load`; `UnusedImageUnloader -> ImageDataEntry.unload` |
| Orbital station shows a different image than its planet | `OrbitalStationSynchListener` / `ImageOnJumpLoader -> ImageHandler.applyPlanetImageToOrbitalStations` |
| Image left on a dead or abandoned colony | `DecivImageRemover`, `NewColonyImageAdder.reportPlayerAbandonedColony`, `ImageHandler.cleanseLeftoverImagesFromRemovedMarkets` |
| Change image button, selector, filters | `rules.csv` `illustratedEntities_marketOpen -> ShowDefaultVisualWithSelector -> ImageDisplayPanel -> ImageSelectorPanel` |
| Description editor | `ImageDisplayPanel` "desc." -> `TextChangerPanel` (one `MultilineTextField` per description) `-> TextDataEntry / TextDataMemory -> TextHandler.applyAllEntries` |
| A setting has no effect | `data/config/LunaSettings.csv` id -> `helper/Setting` constant -> `Settings` cache -> caller |

## Registration and lifecycle

| Registry / hook | Owners |
|---|---|
| `mod_info.json` | `modPlugin` `illustratedEntities.plugins.ModPlugin`; jar `jars/src.jar`; dependencies LazyLib, LunaLib |
| `ModPlugin.onGameLoad()` | Registers every listener, refreshes image data, applies images and texts (order below) |
| `data/config/settings.json` | `illent_csv_path`, `illent_default_image_path`, `ruleCommandPackages` (`illustratedEntities.dialogue.rules`) |
| `data/config/image_data.csv` | Image metadata, merged across mods by `Importer.loadImageData` via `getMergedSpreadsheetDataForMod("id", ...)` |
| `data/config/LunaSettings.csv` | Player settings and help text, read through `Settings` |
| `data/campaign/rules.csv` | `illustratedEntities_marketOpen` replaces the vanilla market visual with `ShowDefaultVisualWithSelector` (score 2) |
| `data/strings/descriptions.csv` | Placeholder `CUSTOM` descriptions `illent_desc_1` to `illent_desc_99` the text editor writes into |
| `data/config/version/version_files.csv` | Version Checker entry for `IllustratedEntities.version` |

Load order in `ModPlugin.onGameLoad`: register listeners (`DecivImageRemover -> NewColonyImageAdder -> ImageOnJumpLoader -> PlayerOpenColonyListener -> PostDialogueVisualPanelCleaner -> OrbitalStationSynchListener -> UnusedImageUnloader`) -> `ImageDataMemory.forceRefresh` -> `ImageHandler.cleanseLeftoverImagesFromRemovedMarkets` -> `ImageHandler.reapplyAllImages` -> `TextHandler.applyAllEntries` -> image application (player markets, presets, random) or `ImageHandler.removeAllImages` when images are disabled -> `ImageHandler.loadImagesForCurrentLocation` -> dev mode only: `MarketInfoPrinter` and market dump to the log.

### Settings

`helper/Setting` owns ids, defaults, bounds and typed access (`Setting.ENABLE_IMAGES.enabled()`, `value()` for int settings). `data/config/LunaSettings.csv` owns the tab layout and help text; its defaults and bounds mirror `Setting`. `helper/Settings` installs a LunaLib listener in `onApplicationLoad`, reinstalls it in `onGameLoad`, and caches the values; callers read the cache, not LunaLib. `Settings.reload` sanitizes each value (missing or wrong type falls back to the default, ints are clamped), writes corrected values back to the LunaLib file, then `helper/LunaSettingsBridge` copies them into the open LunaLib settings panel, which keeps pending values and controls after Save. `Settings.CSV_PATH` and `Settings.DEFAULT_IMAGE_PATH` come from `settings.json`. `tools/SettingsChecks` checks that the CSV and `Setting` agree.

### Build and release tools

`tools/ReleaseBuild.java` (`build`, `package`) and `tools/ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar. `tools/SettingsChecks.java` and `tools/ImagePickerChecks.java` run headless from the repository root against the classes `build` leaves in `out/release-build-*/classes`: `java -cp "out/release-build-<n>/classes:lib/*" illustratedEntities.tools.SettingsChecks` (same for `ImagePickerChecks`). They, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md).

### Live checks

`native-smoke/live` is a test-only mod (id `live_checks`) from the `starsector-native-smoke` skill's `templates/live-check-mod`. It sits outside `jars/src`, so the build never compiles it, and `ReleaseBuild.package` never ships it. `Ui` holds the helpers the groups share.

| Group | Class | Covers |
|---|---|---|
| `images`, `colony`, `text` | `IllustratedChecks` | presets, picker, colonies, descriptions through a reload |
| `panels` | `PanelChecks` | selector and editor panels, buttons pressed with `setChecked` |
| `stations` | `StationChecks` | orbital station sync, station images |
| `editor` | `EditorChecks` | `MultilineTextField` keys sent to `processInput`, with Ctrl and Shift |
| `clicks` | `ClickChecks` | real mouse and keyboard: it logs `LIVE_CHECK input` lines, `clicks.sh` does them with `gamectl` |
| `settings` | `SettingChecks` | each setting through a reload. Run it last, settings stay changed |
| `perf` | `PerfChecks` | times the same steps on any build, see below |
| `seed` | `SeedChecks` | leaves a save for `upgrade`, run on a release zip |
| `upgrade` | `UpgradeChecks` | loads that save, checks the changelog `Note` lines |

`native-smoke/live/run.sh [groups ...]` builds the release jar, stages it, takes `C:\Modding\.smoke-lock` and starts the skill's `run-live-checks.sh`, one game per argument. Set `SMOKE_SCRIPTS` to `scripts/live` in a writable copy of the skill with `gamectl.exe` built there; the top of `run.sh` lists the other settings. Results and captures land in `<SMOKE_WORK>/runs/`. Look at each `check-*.png`, the checks cannot see what a dialog draws.

Performance: every run appends its `PERF` and `PROFILE` lines to `perf.csv` in the main work folder, so report any `PROFILE WARN` and compare with the rows before it. `Timers` holds test-only PatchLib hooks on the mod's classes and on `Display.update` and `sync`; `Perf` turns them into per-call times and frame times, with the wait inside `Display` taken off, so no frame cap has to be turned off. The baseline is the last release, not `master`:

```sh
SMOKE_ZIP=<release zip> SMOKE_WORK=/c/Modding/.smoke-work/Illustrated.Entities.2.0.c bash native-smoke/live/run.sh seed
SMOKE_WORK=/c/Modding/.smoke-work/Illustrated.Entities.upgrade SMOKE_SAVE=/c/Modding/.smoke-work/Illustrated.Entities.2.0.c bash native-smoke/live/run.sh upgrade
```

`run.sh` swaps in `native-smoke/baseline/Groups.java` for a zip, because only `seed` and `perf` compile against the 2.0.c jar. Update the classes it removes when a group changes that.

## Image data

- Each row in `image_data.csv` is one image `graphics/illustrated_entities/images/<id>.png` (`ImageDataEntry.getImagePath`). The id is the save key; never renumber.
- Columns: `id`, `tags_required`, `tags_optional`, `population`, `faction`, `weight`, `planet_id`, `note`. Tags are space separated, leading `!` (one or more) excludes, tokens shorter than 3 chars are skipped. Empty `population` means `A` (any). Player-facing column description is in [README.md](../README.md#adding-images).
- Tag codes and the planet type and condition maps that produce them are in `helper/ImageTags`. `ImagePicker.getTags` builds an entity's tag set from planet type, market conditions, hazard (`> 1.75` adds `int`), ruins, faction size and ownership (`dvl`), and adds `index<size - 2>`.
- `ImagePicker.generateChoices` skips used images when avoiding duplicates, then `ImagePicker.score` rejects by faction, station (`stn` required or optional on the image), gas giant (`gas` must be required), population index (`EXACT` equal, `BROAD` within 1, `ANY` ignored) and tags (all required present, no required exclusion present). Score is `weight` + required tags + optional tags present - optional exclusions present, +1 for an exact population match, +2 when the image's faction is the entity's. Exclusions never add score. Pick weight is `ImagePicker.pickWeight`: `(1 + max(0, score))^2`. Empty results fall back to `BROAD`, then to duplicates allowed with `ANY`.
- The image selector lists `generateChoices` for the selected tags with no faction, best score first, then by id.
- Picked images are stored on the entity, so scoring changes only affect new picks. `tools/ImagePickerChecks` checks the scoring and the `image_data.csv` rows.
- Random picks are seeded with the entity id hash when `respectSeed` is set, so the same planet gets the same image.

## Data identity

| Key | Where | Meaning |
|---|---|---|
| `$illustrated_entity_image` | entity tag and memory key (`ModPlugin.HAS_INTERACTION_IMAGE`) | entity has a mod image; memory value is the image id |
| `$illustrated_entity_image_orbital` | entity tag and memory key (`ModPlugin.ORBITAL_HAS_INTERACTION_IMAGE`) | orbital station mirrors its planet's image |
| `$illent_vanilla_visual` | entity memory (`ModPlugin.VANILLA_VISUAL`) | vanilla illustration replaced by a mod image, restored on removal |
| `$illent_vanilla_description` | entity memory (`ModPlugin.VANILLA_DESCRIPTION`) | vanilla description id replaced by an edited description, restored on delete |
| `$illent_player_pick` | entity memory (`ModPlugin.PLAYER_PICK`) | image was picked in `ImageSelectorPanel`; presets skip it; cleared by `ImageDataEntry.removeFromEntity` |
| `$illustrated_entities_data_memory` | sector memory (`ImageDataMemory.IMAGE_MEMORY`) | `ImageDataMemory` instance |
| `$illustrated_entities_text_memory` | sector memory (`TextDataMemory.TEXT_MEMORY`) | `TextDataMemory` instance |
| `$Illent_TextMemoryID` | entity memory (`TextDataEntry.ID_MEM_KEY`) | number of the entity's edited description |
| `illent_desc_<n>` | description id (`TextDataEntry.DEFAULT_PREFIX`) | custom description slot |
| `illustrated_entities` | mod id (`ModPlugin.MOD_ID`, `Settings.MOD_ID`) | LunaLib and merged CSV mod id |

The prefixes are inconsistent (`$illustrated_entity_`, `$illustrated_entities_`, `$Illent_`, `illent_`). They are save data; keep them as they are and use `illent_` for new data ids.

## Source owners

Use `rg --files jars/src` for the full inventory. The entries below name state and integration owners.

### `plugins`

| File | Owner / connection |
|---|---|
| `plugins/ModPlugin.java` | Lifecycle, listener registration, memory key constants, logger |

### `helper`

| File | Owner / connection |
|---|---|
| `helper/ImageHandler.java` | Applying, removing, reapplying and loading images for entities and orbital stations |
| `helper/ImagePicker.java` | Entity tag set and weighted image choice |
| `helper/ImageTags.java` | Tag codes, display names, type and condition maps, `MatchMode` |
| `helper/TextHandler.java` | Applying and deleting edited descriptions |
| `helper/Setting.java` | Setting ids, defaults, bounds, sanitizing and typed access |
| `helper/Settings.java` | LunaLib listener and value cache; TASC compatibility members |
| `helper/LunaSettingsBridge.java` | Copies sanitized values into the open LunaLib settings panel |

### `memory`

| File | Owner / connection |
|---|---|
| `memory/ImageDataMemory.java` | Map of image id to `ImageDataEntry`, stored in sector memory |
| `memory/ImageDataEntry.java` | One image: tags, preset planet, `usedEntityIds`, texture load and unload |
| `memory/TextDataMemory.java` | Map of description number to `TextDataEntry`, stored in sector memory |
| `memory/TextDataEntry.java` | One edited description: two line maps written to `text1` and `text3`, the whole text in line 1 (`setText`, `getText`) |

### `loading`

| File | Owner / connection |
|---|---|
| `loading/Importer.java` | CSV import, texture load and unload |

### `listeners`

Each class has a static `register()` called from `ModPlugin.onGameLoad`. Transient listeners (`addTransientListener`) and listener manager entries registered with `true` (transient) are re-added on every load. A `BaseCampaignEventListener` needs `addTransientListener`: the listener manager only calls its own interfaces, such as `EconomyTickListener`, and would never call it.

| File | Owner / connection |
|---|---|
| `listeners/DecivImageRemover.java` | Removes the image when a colony decivilizes |
| `listeners/NewColonyImageAdder.java` | Picks an image for a new player colony; removes it on abandon |
| `listeners/ImageOnJumpLoader.java` | Loads textures and syncs stations for the system the player jumps into |
| `listeners/PlayerOpenColonyListener.java` | Loads the texture when the player opens a market |
| `listeners/OrbitalStationSynchListener.java` | Syncs station images when the player closes a market |
| `listeners/UnusedImageUnloader.java` | Unloads textures of unused entries on each economy tick |
| `listeners/PostDialogueVisualPanelCleaner.java` | Clears `VisualCustomPanel` statics just after an interaction dialog is shown |

### `dialogue`

| File | Owner / connection |
|---|---|
| `dialogue/rules/ShowDefaultVisualWithSelector.java` | Rule command; shows the image with the change button or falls back to vanilla `ShowDefaultVisual` behavior |
| `dialogue/impl/ImageDisplayPanel.java` | Current image with change and "desc." buttons |
| `dialogue/impl/ImageSelectorPanel.java` | Filterable image list with preview and apply |
| `dialogue/impl/TextChangerPanel.java` | Description editor: one `MultilineTextField` per description, filled from `TextDataEntry.getText` or the vanilla `text1`/`text3`; Apply writes `TextDataEntry.setText` |
| `dialogue/impl/ConfirmDialoguePanel.java` | Delete confirmation for edited descriptions |
| `dialogue/panel/MultilineTextField.java` | Multiline text box (`CustomUIPanelPlugin`): own word wrap and rendering with LazyLib `LazyFont`, cursor, new lines, scroll bar, `MAX_CHARS` cap; fields sharing a focus group unfocus each other |
| `dialogue/panel/*` | Custom panel plumbing adapted from Histidine's code: `VisualCustomPanel` holds the static panel, tooltip and plugin |

### `dev`

| File | Owner / connection |
|---|---|
| `dev/MarketInfoPrinter.java` | Dev mode only: logs the image id of opened markets |

## Contracts

### Engine

- Images are loaded on demand with `SettingsAPI.loadTexture` and freed with `GL11.glDeleteTextures` plus `unloadTexture` (`Importer`). Any code that shows an image must call `ImageDataEntry.load()` first or the visual renders white.
- The visual size is fixed at `480 x 300` in `ImageDataEntry.addToEntity` and `addToOrbitalStation`.
- `VisualCustomPanel` keeps static references to the panel and plugin. They must be cleared when the dialog closes or they leak; `PostDialogueVisualPanelCleaner` does this with a `DelayedActionScript`.
- `VisualPanelAPI.showCustomPanel` adds the custom panel to the dialog after the option list, and panels hand input to their children last added first, dropping consumed events. A focused `MultilineTextField` consumes every keyboard event, so option hotkeys (number keys, space, Esc) and the dialog never see them. Option shortcuts fire on key down.
- The stock scroller around `VisualCustomPanel`'s tooltip (`ScrollPanelAPI`) takes the mouse wheel before its children whenever its content is taller than its view. `TextChangerPanel` builds its tooltip without a scroller (`VisualCustomPanel.createPanel(dialog, true, false)`) so the wheel always reaches a `MultilineTextField`; its page must fit `VisualCustomPanel.PANEL_HEIGHT`.
- Vanilla clips scroll panels and clipping panels with a scissor stack. `MultilineTextField` intersects its own scissor with the current one and restores it after drawing; a plugin inside a clipped panel must not end its render with `glDisable(GL_SCISSOR_TEST)`.
- `ShowDefaultVisualWithSelector` replaces the vanilla market visual. It must keep the vanilla fallback branches (planet info, fleet info) so markets without a mod image look unchanged.

### Save data

- `ImageDataMemory` and `TextDataMemory` are stored in sector memory and serialized with the save. `isLoaded` and `imageId` on `ImageDataEntry` are `transient`. Textures outlive a save load, so `forceRefresh` sets `isLoaded` from the texture cache.
- `ImageDataMemory.forceRefresh` rebuilds the map from the CSV on every game load, so `usedEntityIds` starts empty after a load. The persistent record of which entity has which image is the entity's tag, memory key and `customInteractionDialogImageVisual`; `reapplyAllImages` and `isUsed` only see entries applied since the load.
- `ImageDataEntry.addToEntity` stores a vanilla illustration it replaces in `$illent_vanilla_visual`; `removeFromEntity` puts it back. Entities overwritten before this key existed have no copy and lose the vanilla illustration on removal.
- `TextDataEntry.apply` stores a vanilla description id it replaces in `$illent_vanilla_description`; `TextHandler.deleteDescription` puts it back. Entities edited before this key existed have no copy and lose the vanilla description on delete.
- `TextDataEntry.stringMap1/2` are save data and TASC copies them line by line. `setText` stores a description's whole text in line 1; entries from older saves keep one entry per line until the player applies an edit. `parseStringMap` reads up to the highest line key and treats a null line as empty.
- The text editor has 99 description slots (`descriptions.csv`). `TextDataMemory.getNexFreetNum` does not check that limit.
- LunaLib's multiline `LunaTextfield` (`LunaLib-Kotlin.jar`, unchanged up to 2.0.5) appends and deletes at the end only, closes on Enter and stops at its box height. `MultilineTextField` is a Java rewrite of it without those limits; do not swap it back.

### Image setting

With `Setting.ENABLE_IMAGES` off: `ModPlugin.onGameLoad` calls `ImageHandler.removeAllImages` (planets, stations and orbital station copies), `NewColonyImageAdder` skips new colonies, and `ImageDisplayPanel` hides the "image" button. `ShowDefaultVisualWithSelector` shows the panel only while images or text edits are on. A change to the setting takes full effect on the next load.

### Other mods

- TASC (Astropolis) calls `Settings.getInt(Settings.TEXT_LINE_NUM)`, `TextDataEntry` (`getString`, `setString`, `apply`, `descriptionNum`), `TextDataMemory.getNexFreetNum/set`, `ImageHandler.hasImage/getImageId/removeImageFrom/setImage` and `ImageDataMemory.get` when it rebuilds a station, and older builds read `Settings.LINE_AMT`. Keep these members and the line-map layout of `TextDataEntry`. The line count setting is gone; `Settings.getInt(Settings.TEXT_LINE_NUM)` returns `LINE_AMT` (7) so TASC still copies line 1.
- TASC makes a new `TextDataEntry` for the rebuilt station and never removes the old one, so every rebuild of a station with an edited description uses up a description slot.
- Kaleidoscope planet names map to tags in `ImageTags.kaleidoscopeNameMap`; Unknown Skies planet types are in `ImageTags.typeTagMap`.

## Dead or dormant

| Component | State |
|---|---|
| `data/config/image_data_alt.csv` | Not read; `settings.json` points at `image_data.csv` |
| `graphics/illustrated_entities/images/archive/` | Not referenced by any data row |
| `graphics/illustrated_entities/images/1108 - cutoff for addition.txt` | Authoring marker, not read |
| `ImageTags.kaleidoscopeNameMap` | Defined, not read by `ImagePicker.getTags` |
| `ModPlugin.devActions` glow texture block | Commented out |
| `Illustrated.Entities.iml` exclude `dialogue/custompanel` | Folder does not exist |
