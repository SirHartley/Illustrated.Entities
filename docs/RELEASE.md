# Release

How Illustrated.Entities is built, packaged and published. Nothing here runs unless a release is asked for: no version bumps, rebuilt jars, tags or published releases for ordinary work.

## Pieces

| Piece | Path | Role |
|---|---|---|
| Changelog | `Changelog` | Source of the version and the release notes. The newest version heading (`2.0.c`) is the release |
| Mod metadata | `mod_info.json` | Version stamped into the ZIP's copy; loads `jars/src.jar` |
| Version file | `IllustratedEntities.version` | Online Version Checker file on `master`; stamped at packaging, committed back after a release |
| Release builder | `jars/src/illustratedEntities/tools/ReleaseBuild.java` | `build` and `package`, run with `java <file>` |
| Builder checks | `jars/src/illustratedEntities/tools/ReleaseBuildChecks.java` | Headless checks of changelog parsing, stamping and file filtering |
| Release jar | `jars/release/IllustratedEntities.jar` | Committed, runtime classes only, carries a source hash; shipped as `jars/src.jar` |
| Release workflow | `.github/workflows/release.yml` | Manual only: preview without a version, publish with one |

The IDE artifact (`jars/src.jar`, gitignored) is separate from the release jar, so an IDE build never replaces it.

## Build the release jar

From the repository root, with JDK 17:

```text
java jars/src/illustratedEntities/tools/ReleaseBuild.java build "<starsector-core>" ["<mods>"]
```

- `<starsector-core>` may also be the install directory that contains `starsector-core`. On Linux, use the `starsector` directory that holds `lwjgl.jar`.
- Compiles against `starfarer.api.jar`, `starfarer_obf.jar`, `lwjgl.jar`, `lwjgl_util.jar`, `json.jar` and `log4j-1.2.9.jar` from the game, and every jar in the `jars/` folder of the LazyLib and LunaLib mods.
- `<mods>` defaults to the game's `mods` folder next to (or inside) the game directory.
- Builds into a fresh `out/release-build-*`, keeps only runtime classes (the `tools` package never ships), writes `Source-SHA256` over the Java sources, and replaces the release jar only after a clean compile.

Rebuild and commit the jar only right before a release.

## Package

```text
java jars/src/illustratedEntities/tools/ReleaseBuild.java package          # preview
java jars/src/illustratedEntities/tools/ReleaseBuild.java package 2.0.d    # release
```

- Preview packages the newest changelog section and accepts a stale jar with a warning.
- Release requires the version (`2.0.d`, no `v`) to equal the newest changelog heading and the release jar to be fresh.
- Ships Git-tracked files only: `mod_info.json`, `IllustratedEntities.version`, `Changelog`, `IllustratedEntities_icon.png`, licence and credit files, `data/` (`csv json shader ship wpn proj system variant skin`), `graphics/` (`png jpg jpeg`), `sounds/` (`ogg wav`) and the release jar as `jars/src.jar`. Source, docs, IDE, agent and workflow files, and any path under a dot, `unused`, `unedited`, `tools` or `docs` directory are left out.
- Fails if a `graphics/illustrated_entities/` path named in `data/` is not packaged.
- Output in `out/release/`: `Illustrated.Entities<version>.zip` (one `Illustrated.Entities/` folder, sorted, fixed timestamps), `release-notes.md`, the stamped version file and `version.txt`.

## Publish

1. Finish the newest `Changelog` section.
2. Rebuild the release jar with `build` and commit it with the changelog.
3. Run `package <version>` locally as the preview check, then merge to `master`.
4. Start the Release workflow on `master` with `version` set: Actions tab, Release, Run workflow, or `gh workflow run release.yml --ref master -f version=<version>`.
5. The workflow runs the builder checks, packages, creates the release and its tag on the packaged commit, then commits the stamped `IllustratedEntities.version` to `master`.
6. Check the release ZIP and notes, and that `master`'s version file names the new version.

The workflow has no push, pull request, tag or schedule trigger. Without a version it only uploads a preview artifact. It refuses to overwrite an existing release, and run on an existing version tag it publishes that tag. It never downloads game libraries; it packages the committed release jar.
