# Illustrated.Entities

Starsector mod (game `0.98a`) that adds illustrations to populated planets and stations, lets the player pick colony images and edit planet descriptions.

## Required workflow

These instructions apply to all coding agents. App-specific tools are scoped below.

Reviews, explanations, audits, and proposals are read-only unless the user requests changes. For implementation, finish the authorized edit, commit, validation, pull request, and merge steps. Use reasonable assumptions for routine details; ask when a missing decision would materially change the scope or behavior. A request to stop or propose only overrides the implementation workflow.

When the user asks for a plan, or to check something as a possible change, post the plan and start no work (no branch, commit or pull request) until the user approves it. Once the plan is approved, finish and merge as usual.

### Read the current documentation

1. Read the current remote `CLAUDE.md` in full at the start of each new task. Within the same task, reuse the version already read unless the instructions change. Read new or changed instruction files before following them.
2. Use the `starsector-knowledge` skill before answering or changing anything related to Starsector. It covers one exact game version. Do not rely on memory.
   - Load the `house-style` skill for every task. Its personal rules (answers, vocabulary, comments, commits, punctuation) take precedence over older wording in this repository.
   - Load the `patchlib` skill before deciding on, planning, writing or reviewing a PatchLib patch.
   - Load the `starsector-rules-writing` skill for any change to `data/campaign/rules.csv` or a rule command class.
3. If the skill is incomplete, inspect the read-only jars under `lib/` (game, LazyLib, LunaLib). Extract them to a temporary directory, never into the repository.
4. Select the required guides using [Which guide to read](#which-guide-to-read). Apply every matching row, including for reviews and fixes to existing behavior.

### Which guide to read

After reading this file, use the task to choose the next document. Read the relevant sections and their prerequisites. A task spanning several areas needs every matching route.

| Task | Required reading / order |
|---|---|
| Locate code or data, trace a feature, change an owner or lifecycle | [ARCHITECTURE.md](docs/ARCHITECTURE.md) before tracing code; follow its owner links. |
| Add, retag or assign images; change how images are picked | [ARCHITECTURE.md](docs/ARCHITECTURE.md#image-data) and the `image_data.csv` columns in [README.md](README.md#adding-images). |
| Change the image selector, description editor or any dialog panel | [ARCHITECTURE.md](docs/ARCHITECTURE.md#dialogue) and the panel contracts there. |
| Write or revise player-facing text (settings help, buttons, changelog) | [CHANGELOG.md](docs/CHANGELOG.md#wording) for wording; settings help text lives in `data/config/LunaSettings.csv`. |
| Edit a guide or add documentation | [Documentation upkeep](#documentation-upkeep), the owning guide and its inbound links. |
| Change the build, rebuild the release jar, or prepare a release | [RELEASE.md](docs/RELEASE.md) |
| Any implementation task | [CHANGELOG.md](docs/CHANGELOG.md): add or amend the task's entries in the current unreleased section before merge. |

### Technical references and conflicts

[ARCHITECTURE.md](docs/ARCHITECTURE.md) is the code and data map; [RELEASE.md](docs/RELEASE.md) owns versions, the release builder and publishing. Resolve a technical conflict against the exact source (repository code or the `starsector-knowledge` skill) and record the evidence; do not pick a winner by filename or recency.

### Make and record changes

- Use one task branch for the user's message.
- Make one commit per requested change. If one message contains several changes, commit them separately in the requested order.
- Commit messages are as short as possible: all lowercase, at most 8 words, programmer speak (`fix crash on abandoned colony`). One line only. No body, no conventional-commit prefix, no testing notes, agent or model names, co-author trailers, session metadata, links or trailing full stop.
- Update the affected documentation in the same commit, following [Documentation upkeep](#documentation-upkeep). Do this without a separate user request.
- Update `Changelog` in the same commit, using [CHANGELOG.md](docs/CHANGELOG.md).
- Rebuild and commit release artifacts, bump versions, and publish a release only when the user explicitly asks; follow [RELEASE.md](docs/RELEASE.md).
- Work only in the current task checkout. Live game installations and unrelated checkouts are read-only unless the user explicitly asks you to change them.
- Do not deploy or synchronize after merge unless the user explicitly requests it.
- The mod is published and players keep long saves. Anything stored in sector memory (`ImageDataMemory`, `TextDataMemory`, entity tags and memory keys) is save data: keep field names and keys stable, and say in the PR when a change affects existing saves.
- Image files are referenced by numeric id (`graphics/illustrated_entities/images/<id>.png`). Never renumber or reuse an id; saves store it.
- Keep all tools and automated checks under `jars/src/illustratedEntities/tools`, in the normal module. Do not add separate test source trees or duplicate platform classes. Tools may read existing constants and types; do not change runtime classes to support them. The one exception is `native-smoke`, a test-only mod that needs the running game (see [ARCHITECTURE.md](docs/ARCHITECTURE.md#live-checks)).
- Prefer listeners and existing callbacks over polling. Before adding or retaining a polling solution, verify against the exact source that no existing callback covers the requirement. Document the missing coverage and keep the scan scope and frequency to what the behavior needs.

### Validate and merge

- Every runtime-affecting final branch must pass the clean build described in [Building](#building).
- Build the exact final remote task-branch revision. Earlier builds, partial builds, IDE analysis and static checks do not satisfy the gate.
- Missing compilers or required dependencies are merge blockers. Documentation-only changes do not require a build.
- Changes to `data/config/image_data.csv` must keep the header and column count, and every `id` must have a matching `<id>.png`.
- A change to `data/config/LunaSettings.csv` or `helper/Setting` must pass `tools/SettingsChecks`. A change to `image_data.csv`, `ImagePicker`, `ImageTags` or `loading/Importer` must pass `tools/ImagePickerChecks`. Commands are in [ARCHITECTURE.md](docs/ARCHITECTURE.md).
- No CI runs on pushes or pull requests. The release workflow runs only when started by hand on request; do not add triggers or check workflows.
- Open a pull request and merge it when the work is complete. Do not leave finished work on an unmerged branch.
- PR titles follow the commit message rules. PR bodies use the house-style `PR_BODY.md` template: `## Changes` ending with a `Changelog:` line (or `no changelog entry: <reason>`), then `## Validation`. No attribution marker, "Requested by" line or generated-by footer.
- Use the connected GitHub app to create, inspect, and merge pull requests.
- Fetch current remote `master` before branching and again before merging. Merge intervening changes, push the exact final commit, and verify the merged remote revision.
- New changes and bug fixes may be committed and merged to `dev` without asking. For that work, fetch `dev` instead of `master`.

### Branch cleanup

GitHub normally deletes a pull request's head branch after merge. Verify that the branch is gone. Remove it manually when it was pushed without a pull request, its pull request was closed without merging, or it predates the repository setting. If deletion returns HTTP 403, report the branch name and leave it in place.

After merge, fast-forward the local task branch to the new `master`.

## Documentation upkeep

Update documents automatically as part of each relevant change. Before committing, check which entries the diff makes stale and update those entries in that commit.

| Document | Owns | Update when / how |
|---|---|---|
| `README.md` | What the mod is, requirements, install, settings overview, adding images, credits | Update when a setting, dependency, install step or the `image_data.csv` format changes. Link to the guides for build and release detail instead of copying it. |
| `AGENTS.md` | Discovery for other agents | Keep it a short pointer to this file. Do not copy workflow policy into it. |
| `CLAUDE.md` | Task scope, workflow, commits, PRs, build gate, comments, document maintenance | Replace changed policies here and update links. Keep provider-specific instructions explicitly scoped. |
| `docs/ARCHITECTURE.md` | Technical routing: owners, registrations, data flow, lifecycle, save data and cross-system constraints | Update the affected route or contract; remove obsolete owners. Use exact paths and symbols. No release history or duplicate policy. |
| `docs/RELEASE.md` | Version numbers, release builder, package contents and publishing | Keep the workflow and builder aligned. |
| `.claude/agents/researcher.md` | Read-only research subagent definition | Keep its steps in line with the subagent dispatch template and the required-reading routes. |
| `lib/` | Read-only compile jars (game, LazyLib, LunaLib, Kotlin runtime) | Replace only for a game or library update, as described in `docs/RELEASE.md`. |
| `Changelog` and `docs/CHANGELOG.md` | Shipped change notes and how agents write them | Add or amend unreleased entries with each implementation task; preserve published entries. |

Give each fact one home. Move useful detail to its proper owner and link to it. After moving a section, repair inbound links and check that no requirement or technical constraint was lost.

For a new or renamed guide, update the required-reading table, this ownership table and the architecture reference table in the same commit. State when to read it and what it does not govern.

Write documentation in ordinary English. Use direct verbs and concrete nouns; remove slogans, inflated claims and commentary about how important the document is. Keep identifiers exact. Describe the current state, not history. Preserve the distinction between absolute restrictions and defaults such as "normally" or "prefer."

## Answers and wording

- Keep answers short and succinct: lead with the result, no preamble or recap. Detailed evidence goes in the pull request.
- Programming-related text (commits, PRs, comments, identifiers, technical docs) uses the vocabulary programmers have used for the last 20 years. Prefer the old word: use, fix, add, show; not leverage, address, introduce, surface.
- No em dashes and no hyphen used as a dash. No emojis unless topical.

## Comments and documentation

- Default to no comments. Add one only where it explains what is not apparent from the code: engine quirks, required ordering, lifecycle rules, save compatibility, serialization, reflection, units, invariants, non-obvious math, cross-file coupling.
- Comments are minimal, all lowercase, terse programmer speak, one line where possible (`// also called for refit previews, listener never advances there`). Code identifiers keep their exact spelling.
- Add no other comments: no javadoc, section headers, restated code, history, TODOs or commented-out code.
- Fix or remove a comment your change makes stale; leave untouched comments alone.
- Documentation records current architecture, contracts, entry points, and live hazards. It must not become a development diary, pull-request history, or postmortem.

## Class layout

- Organize members for reading, not alphabetical sorting.
- Default order: nested enums; static fields and initializers; instance fields and initializers; other nested types; constructors; lifecycle and public entry points in execution or usage order; supporting helpers near their callers.
- Preserve initializer order when moving code could change behavior.
- Keep a cohesive subsystem together when strict declaration grouping would make the class harder to follow.
- Group fields by responsibility, one blank line between groups, none inside a group.
- Leave one blank line after a class, enum, interface, or record opening brace.
- One blank line between methods and major sections. Do not stack empty lines. No section header comments; split or reorder a class that needs them.
- New code goes in the existing package for its area (see [ARCHITECTURE.md](docs/ARCHITECTURE.md#source-owners)). Search the named owner before adding another helper.

## Agent behavior

- Keep the user's selected model. The main agent owns planning, merging and shipped code. Delegate bounded research only when authorized, to the read-only `researcher` agent (`.claude/agents/researcher.md`).
- Treat repository instructions as project requirements and skill guidance as scoped reference material. If a skill blocks work or changes its scope, identify the exact instruction and explain the effect.
- Give brief progress updates and concise final results. Put detailed evidence in the pull request.

### Subagent dispatch template

```text
Task: <specific question or result, and what counts as complete>
Checkout: <absolute path and revision; note relevant uncommitted changes>
Context: <user clarifications, constraints and work already completed>
Access: <allowed sources; permitted writes and paths, if any>
Starting points: <relevant guides, files or symbols, if known>
Return: <desired detail, format and output path, if needed>

1. Read AGENTS.md and CLAUDE.md, then follow the required-reading routes for
   this task. Starting points are navigation help, not the full reading list.
2. Use searches to locate material. Open the surrounding section before using
   a match as evidence; do not answer from filenames or snippets alone.
3. Preserve what each passage applies to: game version, vanilla or mod code,
   current implementation or example. Keep qualifiers such as "normally",
   "only", "never" and "not" when summarizing.
4. For behavioral claims, follow the documentation to the relevant code,
   callers, registration and data. Do not invent APIs, names or behavior.
5. Distinguish documented facts, source-verified behavior and your inferences.
   If evidence is missing or conflicts, say so instead of guessing.
6. Return the result with file/line references or source URLs for material
   claims, plus checks performed and remaining questions. State the scope
   searched; "not found there" does not prove absence everywhere.
```

The main agent checks the cited evidence before using a conclusion to change code or documentation. A subagent's report does not replace the required validation.

## Version names

Versions use major.minor.letter (`2.0.c`). See [RELEASE.md](docs/RELEASE.md); do not bump versions or publish tags as part of ordinary task work.

## Building

### Required compile gate

After the final runtime-affecting commit and before merge:

1. Fetch and build the exact remote task-branch revision.
2. Use a clean, empty output directory.
3. Compile every source file under `jars/src` with JDK 17 against the jars in `lib/`, using the release builder's `build` command in [RELEASE.md](docs/RELEASE.md#build-the-release-jar). It needs no game install.
4. Require a successful exit with no compile errors.
5. Record the command, branch commit, JDK version and result in the pull request. The final reply links the PR and states the result and any untested behavior.
6. Stop before merge if the build or a required dependency is unavailable.

```sh
java jars/src/illustratedEntities/tools/ReleaseBuild.java build
```

`lib/` holds the game's bundled libraries; never substitute Maven versions. Starsector's `JSONException` is checked and a newer JSON jar hides that. The `build` output is gitignored; committing or publishing it is release work.

## Repository navigation

After the required read of this file, follow [Which guide to read](#which-guide-to-read). [ARCHITECTURE.md](docs/ARCHITECTURE.md) is the code and data map, not a substitute for the task-specific guides.
