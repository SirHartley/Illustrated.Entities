# Changelog guide

How agents write entries in the root `Changelog`. Read it for every implementation task. Version numbers, release steps and how the release tooling reads the file are in [RELEASE.md](RELEASE.md).

## When to write an entry

- Every implementation task adds or amends its entries in the current unreleased section before merge, in the same commit as the change.
- Read-only work and documentation-only changes add nothing. Say why in the PR when a change needs no entry.
- Check whether the feature has been released: look for it under every version heading without `UNRELEASED`.
  - Not released yet: edit its `Added -` entry so it describes the feature as it now stands. No `Fixed -`, `Adjusted -` or `Removed -` line for it.
  - Released: the change gets its own entry.
- Record only completed changes from the actual diff. Never list plans or unfinished work.

## File layout

`Changelog` at the repository root, plain text, no extension. Versions newest first, a blank line between them:

```text
2.0.d - UNRELEASED
Fixed - Images no longer vanish after abandoning a colony

2.0.c
Adjusted - Switched out the image for Madeira
```

- Version heading: the version alone on its line, optionally followed by ` - UNRELEASED`. No `v` prefix, no date, no Markdown. The release tooling reads these headings, so keep them in this form.
- Entries: one change per line, starting `Added - `, `Adjusted - `, `Fixed - ` or `Removed - `.
- A `Note - ` line directly below an entry explains a limit or something the player must do.
- Optional short category lines (`Images`, `Text editor`, `General`) when a version has many entries.
- If no unreleased section exists, add one above the newest version. Ask the user which version comes next if it has not been chosen; do not invent a new version per commit.

## Wording

Entries are player-facing: plain sentences in sentence case, not programmer speak.

- Say what changed and name the feature so a player can find it (`image selector`, `description editor`, a setting's name as shown in LunaLib).
- For a fix, describe the broken behavior that is now fixed, not the internal call chain: `Fixed - Crash when abandoning a colony`.
- Give old and new values for changed defaults or numbers.
- No agent or model names, links, validation logs or commit hashes. No claims about how thoroughly anything was tested.

## Amending versus adding

- Combine entries when several edits fix the same player-visible problem.
- An unreleased feature removed again before release loses its `Added -` entry; it gets no `Removed -` line.
- Never repeat a change once per file, step or PR.

## Maintenance

Update this guide when the `Changelog` layout or entry format changes. Release mechanics belong in [RELEASE.md](RELEASE.md).
