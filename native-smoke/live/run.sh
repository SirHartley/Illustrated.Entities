#!/usr/bin/env bash
# builds the jar, stages it, runs the checks under the machine wide lock. git bash on windows
#   run.sh [groups]    default images,colony,text,panels,stations,settings
# SMOKE_SCRIPTS    scripts/live in a writable copy of the starsector-native-smoke skill, with gamectl.exe built there
# SMOKE_GAME       game root
# SMOKE_WORK       scratch folder, keeps the base save. no spaces, run-live-checks.sh passes it through SMOKE_JAVA_ARGS
# SMOKE_JAVAC_DIR  jdk 17 bin folder
set -euo pipefail
: "${SMOKE_SCRIPTS:?set SMOKE_SCRIPTS to the scripts/live folder of a skill copy}"
ROOT=$(cd -- "$(dirname -- "$0")/../.." && pwd)
CHECK_GROUPS=${1:-images,colony,text,panels,stations,settings}
GAME=${SMOKE_GAME:-E:/Games/Starsector 0.98/Starsector 098a-RC8 - Modding}
WORK=${SMOKE_WORK:-/c/Modding/.smoke-work/Illustrated.Entities}
LOCK=/c/Modding/.smoke-lock
export SMOKE_JAVAC_DIR=${SMOKE_JAVAC_DIR:-$USERPROFILE\\.jdks\\corretto-17.0.12\\bin}
JAVA=$(cygpath -u "$SMOKE_JAVAC_DIR")/java.exe
STAGE=$WORK/stage/Illustrated.Entities

cd -- "$ROOT"
"$JAVA" jars/src/illustratedEntities/tools/ReleaseBuild.java build

locked=
cleanup() {
    # junctions, a recursive delete would go through them into the checkout
    for dir in data graphics; do
        if [[ -e "$STAGE/$dir" ]]; then MSYS_NO_PATHCONV=1 cmd.exe /c rmdir "$(cygpath -w "$STAGE/$dir")" >/dev/null 2>&1; fi
    done
    if [[ -n $locked ]]; then rm -rf "$LOCK"; fi
}
trap cleanup EXIT

# one game at a time on this machine, up to an hour of waiting
for _ in $(seq 1 360); do
    if mkdir "$LOCK" 2>/dev/null; then locked=1; break; fi
    ping -n 11 127.0.0.1 >/dev/null
done
[[ -n $locked ]] || { echo "$LOCK still held by: $(cat "$LOCK/owner.txt" 2>/dev/null)" >&2; exit 1; }
echo "Illustrated.Entities live checks" > "$LOCK/owner.txt"

mkdir -p "$STAGE/jars"
cp mod_info.json IllustratedEntities.version "$STAGE/"
cp jars/release/IllustratedEntities.jar "$STAGE/jars/src.jar"
for dir in data graphics; do
    MSYS_NO_PATHCONV=1 cmd.exe /c mklink /J "$(cygpath -w "$STAGE/$dir")" "$(cygpath -w "$ROOT/$dir")" >/dev/null
done

CHECK_JARS="$(cygpath -m "$STAGE/jars/src.jar");$GAME/mods/LunaLib/jars/LunaLib.jar;$GAME/mods/LazyLib/jars/LazyLib.jar;$GAME/mods/LazyLib/jars/internal/Kotlin-Runtime.jar" \
PROFILE_PKG=illustratedEntities \
    bash "$SMOKE_SCRIPTS/run-live-checks.sh" "$GAME" "$WORK" "$ROOT/native-smoke/live" "$CHECK_GROUPS" lw_lazylib lunalib "$STAGE"
