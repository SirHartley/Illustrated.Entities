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
| Preset image for a named vanilla planet | `image_data.csv` `planet_id` -> `ImageHandler.addPresetImagesToMarkets` |
| Image missing or white after a jump or reload | `ImageOnJumpLoader` / `PlayerOpenColonyListener` -> `ImageDataEntry.load`; `UnusedImageUnloader -> ImageDataEntry.unload` |
| Orbital station shows a different image than its planet | `OrbitalStationSynchListener` / `ImageOnJumpLoader -> ImageHandler.applyPlanetImageToOrbitalStations` |
| Image left on a dead or abandoned colony | `DecivImageRemover`, `NewColonyImageAdder.reportPlayerAbandonedColony`, `ImageHandler.cleanseLeftoverImagesFromRemovedMarkets` |
| Change image button, selector, filters | `rules.csv` `illustratedEntities_marketOpen -> ShowDefaultVisualWithSelector -> ImageDisplayPanel -> ImageSelectorPanel` |
| Description editor | `ImageDisplayPanel` "desc." -> `TextChangerPanel -> TextDataEntry / TextDataMemory -> TextHandler.applyAllEntries` |
| A setting has no effect | `data/config/LunaSettings.csv` id -> `helper/Settings` constant -> caller |

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

Load order in `ModPlugin.onGameLoad`: register listeners (`DecivImageRemover -> NewColonyImageAdder -> ImageOnJumpLoader -> PlayerOpenColonyListener -> PostDialogueVisualPanelCleaner -> OrbitalStationSynchListener -> UnusedImageUnloader`) -> `ImageDataMemory.forceRefresh` -> `ImageHandler.cleanseLeftoverImagesFromRemovedMarkets` -> `ImageHandler.reapplyAllImages` -> `TextHandler.applyAllEntries` -> image application (player markets, presets, random) or removal when images are disabled -> `ImageHandler.loadImagesForCurrentLocation` -> dev mode only: `MarketInfoPrinter` and market dump to the log.

### Settings

`helper/Settings` owns the LunaLib setting ids and reads them through `LunaSettings.getInt/getBoolean` on every call; there is no cache. Ids, defaults, bounds and help text live in `data/config/LunaSettings.csv`. `Settings.CSV_PATH` and `Settings.DEFAULT_IMAGE_PATH` come from `settings.json`. `Settings.LINE_AMT` is kept for TASC, which reads it; the mod does not.

### Build and release tools

`tools/ReleaseBuild.java` (`build`, `package`) and `tools/ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar. They, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md).

## Image data

- Each row in `image_data.csv` is one image `graphics/illustrated_entities/images/<id>.png` (`ImageDataEntry.getImagePath`). The id is the save key; never renumber.
- Columns: `id`, `tags_required`, `tags_optional`, `population`, `faction`, `weight`, `planet_id`, `note`. Tags are space separated, `!` excludes, tokens shorter than 3 chars are skipped. Empty `population` means `A` (any). Player-facing column description is in [README.md](../README.md#adding-images).
- Tag codes and the planet type and condition maps that produce them are in `helper/ImageTags`. `ImagePicker.getTags` builds an entity's tag set from planet type, market conditions, hazard (`> 1.75` adds `int`), ruins, faction size and ownership (`dvl`), and adds `index<size - 2>`.
- `ImagePicker.generateChoices` filters by duplicate use, faction, station (`stn` must be optional on the image), gas giant (`gas` must be required), population index (`EXACT` equal, `BROAD` within 1, `ANY` ignored) and tags; score is `weight + matched tags`, pick weight is `choices * score^2`. Empty results fall back to `BROAD`, then to duplicates allowed with `ANY`.
- Random picks are seeded with the entity id hash when `respectSeed` is set, so the same planet gets the same image.

## Data identity

| Key | Where | Meaning |
|---|---|---|
| `$illustrated_entity_image` | entity tag and memory key (`ModPlugin.HAS_INTERACTION_IMAGE`) | entity has a mod image; memory value is the image id |
| `$illustrated_entity_image_orbital` | entity tag and memory key (`ModPlugin.ORBITAL_HAS_INTERACTION_IMAGE`) | orbital station mirrors its planet's image |
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
| `helper/Settings.java` | Setting ids and access |
| `helper/WordUtils.java` | Line wrapping for the text editor |

### `memory`

| File | Owner / connection |
|---|---|
| `memory/ImageDataMemory.java` | Map of image id to `ImageDataEntry`, stored in sector memory |
| `memory/ImageDataEntry.java` | One image: tags, preset planet, `usedEntityIds`, texture load and unload |
| `memory/TextDataMemory.java` | Map of description number to `TextDataEntry`, stored in sector memory |
| `memory/TextDataEntry.java` | One edited description: two line maps written to `text1` and `text3` |

### `loading`

| File | Owner / connection |
|---|---|
| `loading/Importer.java` | CSV import, texture load and unload |

### `listeners`

Each class has a static `register()` called from `ModPlugin.onGameLoad`. Transient listeners (`addTransientListener`) and listener manager entries registered with `true` (transient) are re-added on every load.

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
| `dialogue/impl/TextChangerPanel.java` | Description editor (LunaLib `LunaUITextField`) |
| `dialogue/impl/ConfirmDialoguePanel.java` | Delete confirmation for edited descriptions |
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
- `ShowDefaultVisualWithSelector` replaces the vanilla market visual. It must keep the vanilla fallback branches (planet info, fleet info) so markets without a mod image look unchanged.

### Save data

- `ImageDataMemory` and `TextDataMemory` are stored in sector memory and serialized with the save. `isLoaded` and `imageId` on `ImageDataEntry` are `transient`.
- `ImageDataMemory.forceRefresh` rebuilds the map from the CSV on every game load, so `usedEntityIds` starts empty after a load. The persistent record of which entity has which image is the entity's tag, memory key and `customInteractionDialogImageVisual`; `reapplyAllImages` and `isUsed` only see entries applied since the load.
- The text editor has 99 description slots (`descriptions.csv`). `TextDataMemory.getNexFreetNum` does not check that limit.

### Integrations

- TASC (Astropolis) reads `Settings.LINE_AMT`. Keep the field.
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
