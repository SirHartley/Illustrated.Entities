#!/usr/bin/env bash
# builds the jar, then per argument takes a slot from smoke-slot.sh, stages the jar in that slot and runs the checks.
# git bash on windows
#   run.sh [groups ...]    one game per argument, default images,colony,text,panels,stations,editor,clicks,settings then perf
#                          a perf argument takes the machine alone, every other one shares it
# SMOKE_SCRIPTS    scripts/live in a writable copy of the whole starsector-native-smoke skill, with gamectl.exe built there
# SMOKE_GAME       game root
# SMOKE_WORK       scratch folder, a slotN folder per slot keeps its base save. no spaces, run-live-checks.sh passes
#                  it through SMOKE_JAVA_ARGS
# SMOKE_OWNER      owner name for the slot, default illustrated-entities.checks
# SMOKE_JAVAC_DIR  jdk 17 bin folder
# SMOKE_ZIP        release zip to test in place of the checkout, only the groups in native-smoke/baseline/Groups.java
# SMOKE_MODS       more mods to enable, space separated mod ids. give it its own SMOKE_WORK
# SMOKE_SAVE       slot folder of an earlier run, its last save is the base save here
# SMOKE_PERF       csv that gets the PERF and PROFILE lines of each run
# PROFILE_BASE     profile.jfr of an earlier run of the same groups, the run then writes compare.txt
set -euo pipefail
: "${SMOKE_SCRIPTS:?set SMOKE_SCRIPTS to the scripts/live folder of a skill copy}"
ROOT=$(cd -- "$(dirname -- "$0")/../.." && pwd)
GAME=${SMOKE_GAME:-E:/Games/Starsector 0.98/Starsector 098a-RC8 - Modding}
BASE_WORK=${SMOKE_WORK:-/c/Modding/.smoke-work/Illustrated.Entities}
OWNER=${SMOKE_OWNER:-illustrated-entities.checks}
PERF=${SMOKE_PERF:-/c/Modding/.smoke-work/Illustrated.Entities/perf.csv}
SLOTS=$SMOKE_SCRIPTS/smoke-slot.sh
export SMOKE_JAVAC_DIR=${SMOKE_JAVAC_DIR:-$USERPROFILE\\.jdks\\corretto-17.0.12\\bin}
JAVA=$(cygpath -u "$SMOKE_JAVAC_DIR")/java.exe
CHECK=$ROOT/native-smoke/live
BUILD=$(git -C "$ROOT" rev-parse --short HEAD)$(git -C "$ROOT" diff --quiet HEAD || echo -dirty)
if (( $# )); then RUNS=("$@"); else RUNS=(images,colony,text,panels,stations,editor,clicks,settings perf); fi

cd -- "$ROOT"
if [[ -n ${SMOKE_ZIP:-} ]]; then
    # a release is only read, so every slot shares one copy
    BUILD=$(basename "$SMOKE_ZIP" .zip)
    ZIP_STAGE=$BASE_WORK/stage/$BUILD
    CHECK=$BASE_WORK/check
    [[ -d $ZIP_STAGE ]] || { mkdir -p "$BASE_WORK/stage"; unzip -q "$SMOKE_ZIP" -d "$BASE_WORK/stage"; }
    rm -rf "$CHECK"
    cp -r native-smoke/live "$CHECK"
    rm -f "$CHECK"/src/livecheck/{Groups,ClickChecks,EditorChecks,IllustratedChecks,KeyEvent,MouseEvent,PanelChecks,SettingChecks,StationChecks,UpgradeChecks,VisualChecks,Grab}.java
    cp native-smoke/baseline/Groups.java "$CHECK/src/livecheck/"
else
    "$JAVA" jars/src/illustratedEntities/tools/ReleaseBuild.java build
fi

slot=
bridge=
STAGE=
unstage() {
    if [[ -n $bridge ]]; then kill "$bridge" 2>/dev/null || true; bridge=; fi
    # junctions, a recursive delete would go through them into the checkout
    if [[ -n $STAGE && -z ${SMOKE_ZIP:-} ]]; then
        for dir in data graphics; do
            if [[ -e "$STAGE/$dir" ]]; then MSYS_NO_PATHCONV=1 cmd.exe /c rmdir "$(cygpath -w "$STAGE/$dir")" >/dev/null 2>&1; fi
        done
    fi
    if [[ -n $slot ]]; then bash "$SLOTS" release "$slot" "$OWNER" >/dev/null || true; slot=; fi
}
trap unstage EXIT

record() {
    local stamp line
    stamp=$(date +%Y-%m-%dT%H:%M:%S)
    { sed -n 's/^LIVE_CHECK note: PERF //p' "$WORK/run/results.txt"; sed -n 's/^PROFILE/profile |/p' "$WORK/run/profile.txt"; } 2>/dev/null |
        while IFS= read -r line; do printf '%s,%s,%s,"%s"\n' "$stamp" "$BUILD" "$1" "$line"; done >> "$PERF"
}

status=0
for groups in "${RUNS[@]}"; do
    mode=--shared
    [[ $groups == perf ]] && mode=--perf
    slot=$(bash "$SLOTS" acquire "$OWNER" "$mode" --holder $$) || { status=1; slot=; break; }
    WORK=$BASE_WORK/slot$slot
    mkdir -p "$WORK"
    if [[ ! -d $WORK/base-saves && -d $BASE_WORK/base-saves ]]; then
        cp -r "$BASE_WORK/base-saves" "$WORK/base-saves"
        cp "$BASE_WORK/base-saves.mods" "$WORK/base-saves.mods"
    fi
    if [[ -n ${SMOKE_SAVE:-} ]]; then
        rm -rf "$WORK/base-saves"
        cp -r "$SMOKE_SAVE/run/saves" "$WORK/base-saves"
        cp "$SMOKE_SAVE/base-saves.mods" "$WORK/base-saves.mods"
    fi

    if [[ -n ${SMOKE_ZIP:-} ]]; then
        STAGE=$ZIP_STAGE
    else
        # a jar per slot, windows keeps it locked while that slot's game runs
        STAGE=$WORK/stage/Illustrated.Entities
        mkdir -p "$STAGE/jars"
        cp mod_info.json IllustratedEntities.version "$STAGE/"
        cp jars/release/IllustratedEntities.jar "$STAGE/jars/src.jar"
        for dir in data graphics; do
            [[ -e "$STAGE/$dir" ]] || MSYS_NO_PATHCONV=1 cmd.exe /c mklink /J "$(cygpath -w "$STAGE/$dir")" "$(cygpath -w "$ROOT/$dir")" >/dev/null
        done
    fi

    if [[ $groups == *clicks* ]]; then
        mkdir -p "$WORK/run/logs"
        rm -f "$WORK/run/logs/starsector.log"
        bash native-smoke/live/clicks.sh "$WORK/run" "$SMOKE_SCRIPTS/gamectl.exe" &
        bridge=$!
    fi
    CHECK_JARS="$(cygpath -m "$STAGE/jars/src.jar");$GAME/mods/LunaLib/jars/LunaLib.jar;$GAME/mods/LazyLib/jars/LazyLib.jar;$GAME/mods/LazyLib/jars/internal/Kotlin-Runtime.jar;$GAME/mods/PatchLib/jars/PatchLibAPI.jar" \
    PROFILE_PKG=illustratedEntities SMOKE_SLOT=$slot SMOKE_OWNER=$OWNER PERF=$([[ $mode == --perf ]] && echo 1 || echo 0) \
        bash "$SMOKE_SCRIPTS/run-live-checks.sh" "$GAME" "$WORK" "$CHECK" "$groups" patchlib lw_lazylib lunalib ${SMOKE_MODS:-} "$STAGE" || status=1
    record "$groups"
    unstage
    STAGE=
done
exit $status
