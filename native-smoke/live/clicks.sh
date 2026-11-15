#!/usr/bin/env bash
# does the input lines ClickChecks logs, as window messages to the game. the mouse and keyboard stay free
#   clicks.sh RUN_DIR GAMECTL
RUN=$1
GAMECTL=$2
LOG=$RUN/logs/starsector.log
done_input=0

wait_ms() {
    ping -n 1 -w "$1" 192.0.2.1 >/dev/null
}

while :; do
    if [[ ! -f $LOG || ! -f $RUN/game.pid ]]; then wait_ms 500; continue; fi
    export GAMECTL_PID
    GAMECTL_PID=$(cat "$RUN/game.pid")
    while IFS= read -r line; do
        number=${line%% *}
        what=${line#* }
        (( number > done_input )) || continue
        done_input=$number
        case $what in
            click\ *)
                read -r _ x y <<< "$what"
                "$GAMECTL" bmove $((x - 4)) "$y" >/dev/null
                wait_ms 200
                "$GAMECTL" bmove "$x" "$y" >/dev/null
                wait_ms 200
                "$GAMECTL" bclick "$x" "$y" >/dev/null
                ;;
            type\ *) "$GAMECTL" btype "${what#type }" >/dev/null ;;
            key\ *) "$GAMECTL" bkey "${what#key }" >/dev/null ;;
        esac
        wait_ms 200
    done < <(sed -n 's/.*LIVE_CHECK input //p' "$LOG")
    wait_ms 300
done
