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
- Entries: one change per line, starting `Added - `, `Adjusted - `, `Fixed - ` or `Removed - `, in that order inside a category.
- A `Note - ` line directly below an entry explains a limit or something the player must do.
- Category lines are in-game feature names (`Images`, `Description editor`), `General` first, a blank line between categories. A version with only a few entries needs none.
- Never write or change the published heading's ` - CURRENT PUBLIC VERSION` suffix or the save compatibility line unasked.
- Use a `Development` category for useful changes to the build or authoring tools. Do not present a refactor or a documentation edit as a gameplay change.
- If no unreleased section exists, add one above the newest version. Ask the user which version comes next if it has not been chosen; do not invent a new version per commit.

## Wording

Entries are player-facing, in the user's own changelog voice. The `house-style` skill's "Changelog entries" section is the full reference and wins over this summary.

- Short: stay under 80 characters, past 100 only for a new feature that needs its numbers. Capital letter after the dash, no full stop on a one-clause line.
- In-game names and capitals: a setting's name as shown in LunaLib, `Image selector`, `Description editor`. What the player sees, never classes or the call chain.
- Parentheses carry the short extra: `(was 3)`, `(default off)`, `(tag typos)`. No semicolons, no curly quotes, no hyphen as a dash.
- Added: name, colon, what it does, or `Added - Setting to ...`.
- Adjusted: numbers as old `->` new with the unit (`Adjusted - Railgun charge up 3s -> 5s`), otherwise a direction word (Reduced, Increased, Halved).
- Fixed, most used forms first: `Fixed - X no longer does Y`, `Fixed - X is now properly ...`, `Fixed - Crash when ...`, or a short noun phrase (`Fixed - Typos`). Name the other mod in cross-mod bugs.
- Removed: the thing, then the reason or replacement in parentheses.
- Credits name real people only (`(thanks, name)`, `(reported by name)`). Keep the user's own lines verbatim, commentary included; agent lines have no first person, jokes, caps, exclamation marks or hedges.
- No agent or model names, links, validation logs or commit hashes. Untested status goes in the PR, not the changelog.

## Amending versus adding

- Combine entries when several edits fix the same player-visible problem.
- An unreleased feature removed again before release loses its `Added -` entry; it gets no `Removed -` line.
- Never repeat a change once per file, step or PR.

## Maintenance

Update this guide when the `Changelog` layout or entry format changes. Release mechanics belong in [RELEASE.md](RELEASE.md).
