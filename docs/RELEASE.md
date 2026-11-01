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
| Build libraries | `lib/` | Game and library jars copied from Industrial.Evolution's `lib/` |
| Release jar | `jars/release/IllustratedEntities.jar` | Built by `build`, gitignored, runtime classes only; shipped as `jars/src.jar` |
| Release workflow | `.github/workflows/release.yml` | Manual only: preview without a version, publish with one |

The IDE artifact (`jars/src.jar`, gitignored) is separate from the release jar, so an IDE build never replaces it.

## Build the release jar

From the repository root, with JDK 17:

```text
java jars/src/illustratedEntities/tools/ReleaseBuild.java build
```

- Compiles against the jars in `lib/`: `starfarer.api.jar`, `starfarer_obf.jar`, `fs.common_obf.jar`, `lwjgl.jar`, `lwjgl_util.jar`, `json.jar`, `log4j-1.2.9.jar`, `LazyLib.jar`, `LunaLib.jar` and `Kotlin-Runtime.jar` (for `org.jetbrains.annotations`). No game install is needed.
- Builds into a fresh `out/release-build-*`, keeps only runtime classes (the `tools` package never ships), and replaces the release jar only after a clean compile.
- After a game or library update, copy the new jars into `lib/` (Industrial.Evolution keeps the current set).

## Package

```text
java jars/src/illustratedEntities/tools/ReleaseBuild.java package          # preview
java jars/src/illustratedEntities/tools/ReleaseBuild.java package 2.0.d    # release
```

- Needs a release jar from `build`.
- Preview packages the newest changelog section.
- Release requires the version (`2.0.d`, no `v`) to equal the newest changelog heading.
- Ships Git-tracked files only: `mod_info.json`, `IllustratedEntities.version`, `Changelog`, `IllustratedEntities_icon.png`, licence and credit files, `data/` (`csv json shader ship wpn proj system variant skin`), `graphics/` (`png jpg jpeg`), `sounds/` (`ogg wav`) and the release jar as `jars/src.jar`. Source, docs, IDE, agent and workflow files, and any path under a dot, `unused`, `unedited`, `tools` or `docs` directory are left out.
- Fails if a `graphics/illustrated_entities/` path named in `data/` is not packaged.
- Output in `out/release/`: `Illustrated.Entities<version>.zip` (one `Illustrated.Entities/` folder, sorted, fixed timestamps), `release-notes.md`, the stamped version file and `version.txt`.

## Publish

1. Finish the newest `Changelog` section.
2. Run `build` and `package <version>` locally as the preview check, then merge to `master`.
3. Start the Release workflow on `master` with `version` set: Actions tab, Release, Run workflow, or `gh workflow run release.yml --ref master -f version=<version>`.
4. The workflow runs the builder checks, builds the jar, packages, creates the release and its tag on the packaged commit, then commits the stamped `IllustratedEntities.version` to `master`.
5. Check the release ZIP and notes, and that `master`'s version file names the new version.

The workflow has no push, pull request, tag or schedule trigger. Without a version it only uploads a preview artifact. It refuses to overwrite an existing release, and run on an existing version tag it publishes that tag. It compiles from `lib/` and downloads nothing.
