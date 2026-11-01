# Illustrated.Entities

A Starsector mod that adds illustrations to populated planets and stations. Every vanilla core world gets a hand-picked image, the player can choose images for their own colonies, and planet descriptions can be edited in game.

Forum thread: [fractalsoftworks.com/forum/index.php?topic=25649](https://fractalsoftworks.com/forum/index.php?topic=25649.0)

## Features

- Preset images for vanilla core world planets that have no vanilla illustration.
- About 1300 images, picked to fit the planet's type, conditions, faction and colony size.
- New player colonies get a fitting image automatically.
- Image selector on player colonies: filter by tags, match colony size (exact, broad or any), preview and apply.
- Description editor: rewrite a planet's or colony's description text.
- Optional random images for modded and NPC colonies without one.
- Images are loaded only for the current system and unloaded when unused, so VRAM use stays low.

## Requirements

- Starsector `0.98a`
- LazyLib
- LunaLib

## Install

1. Download `Illustrated.Entities<version>.zip` from the [latest release](https://github.com/SirHartley/Illustrated.Entities/releases/latest).
2. Extract it into `Starsector/mods/`.
3. Enable Illustrated.Entities, LazyLib and LunaLib in the launcher.

Version Checker picks up updates through `IllustratedEntities.version`.

## Settings

Settings are in the LunaLib settings menu in game. The help text for each one is shown there and lives in `data/config/LunaSettings.csv`.

| Setting | Default |
|---|---|
| Add images | on |
| Add preset images to core world planets | on |
| Random colony images | off |
| Skip core for random images | off |
| Always show image selector button | off |
| Default broad match | on |
| Allow text edits | on |
| Number of text lines | 7 |

## Adding images

Images are `graphics/illustrated_entities/images/<id>.png`, 480 x 300, described by one row each in `data/config/image_data.csv`. Another mod can add rows to the same file path; the game merges them by `id`. Ids are stored in saves, so never renumber or reuse one.

| Column | Meaning |
|---|---|
| `id` | Image number, matches the file name |
| `tags_required` | Tags the planet must have. Prefix `!` for tags it must not have |
| `tags_optional` | Tags that raise the image's score when present (or absent, with `!`) |
| `population` | Fitting colony size index (colony size minus 2, `0` to `4`); empty means any |
| `faction` | Faction id the image is limited to; empty means any |
| `weight` | Extra score; empty means `0` |
| `planet_id` | Entity id to always give this image (preset) |
| `note` | Free text, not read |

Tags, separated by spaces:

| Tag | Meaning | Tag | Meaning |
|---|---|---|---|
| `der` | derelict | `int` | hostile environment |
| `rns` | ruins | `hab` | habitable |
| `bar` | barren | `mne` | mining |
| `dst` | desert | `stn` | station (images for stations must list it as optional) |
| `snw` | snow | `irr` | irradiated |
| `tun` | tundra | `tox` | toxic |
| `ard` | arid | `hot` | hot |
| `wtr` | water | `cld` | cold |
| `jun` | jungle | `pol` | polluted |
| `ter` | terran | `dvl` | developed |
| `lav` | lava | `atm` | atmosphere |
| `aln` | alien | `red` | red |
| `gas` | gas giant (gas giants only get images that require it) | `slt` | salt |

How tags are derived from a planet and how picking works is described in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#image-data).

## Building and releasing

- Build requirements and the compile command: [CLAUDE.md](CLAUDE.md#building).
- Versions, the release builder and publishing: [docs/RELEASE.md](docs/RELEASE.md).
- Change notes go in `Changelog`, following [docs/CHANGELOG.md](docs/CHANGELOG.md).

## Credits

- Custom dialog panel code adapted from Histidine.
- Image unloading thanks to starficz.
