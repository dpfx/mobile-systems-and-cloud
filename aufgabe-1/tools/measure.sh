#!/usr/bin/env bash
# Part D measurements for Sensor Lab on the Android emulator.
#
# Injects known sensor values into the running emulator, reads what the app received,
# calculated and displayed, and writes the raw results as CSV files to docs/measurements/.
# docs/measurements.md describes the experiments, the expected values and the evaluation.
#
# Usage:  tools/measure.sh [all|accelerometer|barometer|gps|rate|cpu]   (default: all)
#
# Needs:  the emulator running, Sensor Lab installed as a debug build (logging on) with precise
#         location granted, and WSL with python3.
# adb:    the Windows adb.exe is used, because WSL's own adb can't reach the Windows emulator.
#         Its path comes from $ADB or is derived from Windows' %LOCALAPPDATA% at runtime,
#         so no machine-specific path is stored in this file.

set -u -o pipefail

PKG=de.penguin.sensorlab
OUT="$(cd "$(dirname "$0")/.." && pwd)/docs/measurements"
TMP=$(mktemp -d)
SETUP_DONE=false
mkdir -p "$OUT"

# ---------------------------------------------------------------- Helpers

find_adb() {
    if [ -n "${ADB:-}" ]; then
        echo "$ADB"
        return
    fi
    local appdata
    appdata=$(cd /mnt/c && cmd.exe /c echo %LOCALAPPDATA% 2>/dev/null | tr -d '\r')
    echo "$(wslpath "$appdata")/Android/Sdk/platform-tools/adb.exe"
}
ADB=$(find_adb)

log() { echo "[$(date +%T)] $*"; }
die() { log "ERROR: $*"; exit 1; }

# Runs adb with a timeout and removes Windows line endings. Only for text output.
a() { timeout 20 "$ADB" "$@" 2>&1 | tr -d '\r'; }

# Prints the last app log line with tag $1 that contains $2 (since the last "logcat -c").
last_log() { a logcat -d -v time -s "$1:V" | grep -F -- "$2" | tail -1; }

# Counts the app log lines with tag $1 that contain $2 (since the last "logcat -c").
count_log() { a logcat -d -v time -s "$1:V" | grep -cF -- "$2"; }

# Saves the screen's accessibility tree to $TMP/ui.xml.
dump_ui() {
    a shell uiautomator dump /sdcard/ui.xml >/dev/null
    timeout 20 "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"
}

# Taps the first element on screen whose text matches the regular expression $1.
tap_text() {
    dump_ui
    local x y
    read -r x y < <(python3 -I - "$TMP/ui.xml" "$1" <<'EOF'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).getroot().iter("node"):
    if re.search(sys.argv[2], (node.get("text") or "").strip()):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
EOF
)
    [ -n "${x:-}" ] || die "'$1' not found on screen"
    a shell input tap "$x" "$y" >/dev/null
}

# Sets the switch next to the label matching $2 to $1 (true or false).
set_switch() {
    local want=$1 label=$2 x y checked
    dump_ui
    read -r x y checked < <(python3 -I - "$TMP/ui.xml" "$label" <<'EOF'
import re, sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
middle = lambda n: sum(map(int, re.findall(r"\d+", n.get("bounds"))[1::2])) // 2
labels = [n for n in nodes if re.search(sys.argv[2], n.get("text") or "")]
switches = [n for n in nodes if n.get("checkable") == "true"]
if labels and switches:
    switch = min(switches, key=lambda n: abs(middle(n) - middle(labels[0])))
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", switch.get("bounds")))
    print((x1 + x2) // 2, (y1 + y2) // 2, switch.get("checked"))
EOF
)
    [ -n "${x:-}" ] || die "switch '$label' not found on screen"
    [ "$checked" = "$want" ] || a shell input tap "$x" "$y" >/dev/null
}

# Prints the text shown right after the label $1 inside the card titled $2 (Values tab).
shown_value() {
    dump_ui
    python3 -I - "$TMP/ui.xml" "$1" "$2" <<'EOF'
import sys, xml.etree.ElementTree as ET
texts = [n.get("text") for n in ET.parse(sys.argv[1]).getroot().iter("node") if n.get("text")]
start = texts.index(sys.argv[3]) if sys.argv[3] in texts else 0
for i in range(start, len(texts) - 1):
    if texts[i] == sys.argv[2]:
        print(texts[i + 1])
        break
EOF
}

# Brings Sensor Lab to the front like the launcher icon does and selects the Values tab.
launch_app() {
    a shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
        -n "$PKG/.MainActivity" >/dev/null
    sleep 3
    # Output is captured first and tested afterwards: with pipefail, "| grep -q" would fail
    # exactly when it finds a match (it stops reading, and adb fails with a broken pipe).
    local top
    top=$(a shell dumpsys activity activities | grep -m1 'topResumedActivity=')
    [[ "$top" == *"$PKG/.MainActivity"* ]] || die "Sensor Lab is not in front"
    tap_text '^Values$'
    sleep 1
}

# Neutral inputs: phone upright at rest, standard pressure, start position of the GPS walk.
neutral_inputs() {
    a emu sensor set acceleration 0:9.80665:0 >/dev/null
    a emu sensor set pressure 1013.25 >/dev/null
    a emu geo fix 9.0270 48.2120 >/dev/null
}

setup() {
    [ "$(a get-state)" = "device" ] || die "no emulator connected: start it first"
    # Wake the screen, remove the lock screen and keep the screen on during the measurements;
    # the previous screen setting is restored at the end.
    a shell input keyevent KEYCODE_WAKEUP >/dev/null
    a shell wm dismiss-keyguard >/dev/null
    STAY_ON=$(a shell settings get global stay_on_while_plugged_in)
    a shell svc power stayon true >/dev/null
    launch_app
    neutral_inputs
    set_switch false '^Low rate'
    SETUP_DONE=true
    sleep 2
}

cleanup() {
    if [ "$SETUP_DONE" = true ]; then
        log "Restoring neutral inputs, GAME rate and the screen setting"
        neutral_inputs
        set_switch false '^Low rate'
        a shell settings put global stay_on_while_plugged_in "$STAY_ON" >/dev/null
    fi
    rm -rf "$TMP"
}
trap cleanup EXIT

# ---------------------------------------------------------------- Accelerometer

measure_accelerometer() {
    log "Accelerometer: flat, tilted 30°, free fall; 5 runs each"
    local csv="$OUT/accelerometer.csv" tilt_x tilt_z
    read -r tilt_x tilt_z < <(python3 -I -c '
import math
g = 9.80665
print(f"{g * math.sin(math.radians(30)):.6f} {g * math.cos(math.radians(30)):.6f}")')
    echo "scenario,run,x_set,y_set,z_set,x_received,y_received,z_received,magnitude_ms2,magnitude_g" > "$csv"
    local scenario name x y z run raw processed
    for scenario in "flat 0 0 9.80665" "tilted_30 $tilt_x 0 $tilt_z" "free_fall 0 0 0"; do
        read -r name x y z <<< "$scenario"
        for run in 1 2 3 4 5; do
            # Each run starts from the neutral position, then the scenario is set.
            a emu sensor set acceleration 0:9.80665:0 >/dev/null
            sleep 1
            a emu sensor set acceleration "$x:$y:$z" >/dev/null
            a logcat -c
            sleep 2.5
            # "Accelerometer: x=0.000 y=0.000 z=9.807 m/s², t=… ns, measured 50.0 Hz"
            raw=$(last_log SensorLab.Sensor "Accelerometer: x=" |
                sed -E 's/.*x=(-?[0-9.]+) y=(-?[0-9.]+) z=(-?[0-9.]+).*/\1,\2,\3/')
            # "Accelerometer: |a|=9.807 m/s² = 1.000 g"
            processed=$(last_log SensorLab.Process "Accelerometer: |a|=" |
                sed -E 's/.*\|a\|=([0-9.]+) [^ ]+ = ([0-9.]+) g.*/\1,\2/')
            echo "$name,$run,$x,$y,$z,$raw,$processed" >> "$csv"
        done
        log "  $name done"
    done
    a emu sensor set acceleration 0:9.80665:0 >/dev/null
}

# ---------------------------------------------------------------- Barometer

# Which pressure arrives in the app for a given input: 21 inputs in steps of 0.01 hPa.
measure_barometer_steps() {
    log "Barometer: pressure steps of the emulator (21 inputs)"
    local csv="$OUT/barometer-steps.csv" k p received
    echo "p_set_hpa,p_received_hpa" > "$csv"
    for k in $(seq 0 20); do
        p=$(python3 -I -c "print(f'{1013.25 - $k * 0.01:.2f}')")
        a emu sensor set pressure "$p" >/dev/null
        a logcat -c
        sleep 2.5
        # "Barometer: p=1013.250 hPa, t=… ns, measured 5.0 Hz"
        received=$(last_log SensorLab.Sensor "Barometer: p=" | sed -E 's/.*p=([0-9.]+) hPa.*/\1/')
        echo "$p,$received" >> "$csv"
    done
}

# One to three floors of 3 m: Reset at 1013.25 hPa, then the pressure 3 m, 6 m or 9 m higher.
measure_barometer_floors() {
    log "Barometer: 1-3 floors of 3 m; 5 runs each"
    local csv="$OUT/barometer.csv" p0=1013.25 floors height p run received delta_h
    echo "floors,run,height_m,p_start_hpa,p_set_hpa,p_received_hpa,delta_h_m" > "$csv"
    for floors in 1 2 3; do
        height=$((3 * floors))
        # Inverse of SensorManager.getAltitude(): p = p0 * (1 - h / 44330)^5.255
        p=$(python3 -I -c "print(f'{$p0 * (1 - $height / 44330) ** 5.255:.3f}')")
        for run in 1 2 3 4 5; do
            a emu sensor set pressure "$p0" >/dev/null
            sleep 2
            tap_text '^Reset$'
            sleep 1.5
            a emu sensor set pressure "$p" >/dev/null
            a logcat -c
            sleep 2.5
            received=$(last_log SensorLab.Sensor "Barometer: p=" | sed -E 's/.*p=([0-9.]+) hPa.*/\1/')
            # "Barometer: h=26.03 m (standard atmosphere), Δh=3.12 m"
            delta_h=$(last_log SensorLab.Process "Barometer: h=" | sed -E 's/.*Δh=(-?[0-9.]+) m.*/\1/')
            echo "$floors,$run,$height,$p0,$p,$received,$delta_h" >> "$csv"
        done
        log "  $floors floor(s) done"
    done
    a emu sensor set pressure "$p0" >/dev/null
}

# ---------------------------------------------------------------- GPS

# Last distance in the app's processing log ("…, distance=111.2 m").
last_distance() { last_log SensorLab.Process "GPS: " | sed -E 's/.*distance=([0-9.]+) m.*/\1/'; }

# Per run: Reset at the start point, a walk of 10 steps of 0.0001° north every 4 s (2.8 m/s),
# 20 s of jitter around the end point (± 2-3 m), then a jump to Mountain View.
measure_gps() {
    log "GPS: walk, jitter, jump; 5 runs"
    local permission
    permission=$(a shell dumpsys package "$PKG" | grep -m1 'ACCESS_FINE_LOCATION: granted=')
    [[ "$permission" == *"granted=true"* ]] || die "precise location is not granted to Sensor Lab"
    local csv="$OUT/gps.csv" lon=9.0270 lat0=48.2120 run i lat walk jitter_counted after_jitter
    local jump_rejected after_jump offset
    echo "run,walk_m,jitter_counted_steps,after_jitter_m,jump_rejected_logged,after_jump_m" > "$csv"
    for run in 1 2 3 4 5; do
        a emu geo fix "$lon" "$lat0" >/dev/null
        sleep 4
        tap_text '^Reset$'
        # The first fix after Reset becomes the start point and can arrive up to ~1.5 s late.
        # Waiting 6 s keeps the first step as slow as the others (≥ 4 s for 11 m), so it isn't
        # rejected by the app's 10 m/s plausibility limit.
        sleep 6

        a logcat -c
        for i in $(seq 1 10); do
            lat=$(python3 -I -c "print(f'{$lat0 + $i * 0.0001:.6f}')")
            a emu geo fix "$lon" "$lat" >/dev/null
            sleep 4
        done
        walk=$(last_distance)

        a logcat -c
        for i in $(seq 1 20); do
            offset=$(python3 -I -c "print([0.00002, -0.00002, 0.00003, -0.00003][$i % 4])")
            lat=$(python3 -I -c "print(f'{$lat0 + 0.0010 + $offset:.6f}')")
            a emu geo fix "$lon" "$lat" >/dev/null
            sleep 1
        done
        jitter_counted=$(count_log SensorLab.Process "GPS: counted")
        after_jitter=$(last_distance)

        a logcat -c
        a emu geo fix -122.0840 37.4220 >/dev/null
        sleep 4
        jump_rejected=$(count_log SensorLab.Process "GPS: rejected")
        after_jump=$(last_distance)

        echo "$run,$walk,$jitter_counted,$after_jitter,$jump_rejected,$after_jump" >> "$csv"
        log "  run $run: walk $walk m, after jitter $after_jitter m, after jump $after_jump m"
    done
    a emu geo fix "$lon" "$lat0" >/dev/null
}

# ---------------------------------------------------------------- Event rate

# Rate of the accelerometer from Android's own record of the last events (dumpsys sensorservice),
# printed as "events,span_s,rate_hz", plus the number of active connections to the sensor.
android_rate() {
    a shell dumpsys sensorservice | python3 -I -c '
import re, sys
lines = sys.stdin.read().splitlines()
start = next(i for i, l in enumerate(lines) if re.match(r"Goldfish 3-axis Accelerometer: last \d+ events", l))
ts = []
for line in lines[start + 1:]:
    m = re.search(r"ts=([0-9.]+)", line)
    if not m:
        break
    ts.append(float(m.group(1)))
connections = re.search(r"Goldfish 3-axis Accelerometer.*connections=(\d+)", "\n".join(lines))
span = ts[-1] - ts[0]
print(f"{len(ts)},{span:.3f},{(len(ts) - 1) / span:.1f},{connections.group(1) if connections else ""}")'
}

measure_rate() {
    log "Event rate: GAME and NORMAL; 5 readings each"
    local csv="$OUT/rate.csv" setting low requested run app
    echo "setting,run,requested_hz,app_measured_hz,android_events,android_span_s,android_rate_hz,sensor_connections" > "$csv"
    for setting in GAME NORMAL; do
        if [ "$setting" = NORMAL ]; then low=true; requested=5; else low=false; requested=50; fi
        set_switch "$low" '^Low rate'
        sleep 6
        for run in 1 2 3 4 5; do
            app=$(shown_value Measured Accelerometer | sed -E 's/ Hz//')
            echo "$setting,$run,$requested,$app,$(android_rate)" >> "$csv"
            sleep 5
        done
        log "  $setting done"
    done
    set_switch false '^Low rate'
}

# ---------------------------------------------------------------- CPU time

# CPU time (user + system) of the app process in clock ticks, from /proc/<pid>/stat.
cpu_ticks() {
    local stat
    stat=$(a shell cat "/proc/$1/stat")
    case "$stat" in *"$PKG"*) ;; *) stat=$(a shell run-as "$PKG" cat "/proc/$1/stat") ;; esac
    # Fields after the process name: state is the 1st, utime the 12th, stime the 13th.
    echo "$stat" | python3 -I -c 'import sys; f = sys.stdin.read().rsplit(")", 1)[1].split(); print(int(f[11]) + int(f[12]))'
}

measure_cpu() {
    log "CPU time: 60 s at GAME and at NORMAL; 3 runs each"
    local csv="$OUT/cpu.csv" pid hz run setting low t0 t1 s0 s1
    pid=$(a shell pidof "$PKG")
    [ -n "$pid" ] || die "Sensor Lab is not running"
    hz=$(a shell getconf CLK_TCK 2>/dev/null | grep -E '^[0-9]+$' || echo 100)
    echo "run,setting,wall_s,cpu_ticks,ticks_per_s,cpu_s,cpu_percent" > "$csv"
    for run in 1 2 3; do
        for setting in GAME NORMAL; do
            if [ "$setting" = NORMAL ]; then low=true; else low=false; fi
            set_switch "$low" '^Low rate'
            sleep 5
            t0=$(cpu_ticks "$pid"); s0=$(date +%s.%N)
            sleep 60
            t1=$(cpu_ticks "$pid"); s1=$(date +%s.%N)
            python3 -I -c "
wall = $s1 - $s0; ticks = $t1 - $t0; cpu = ticks / $hz
print(f'$run,$setting,{wall:.1f},{ticks},$hz,{cpu:.2f},{100 * cpu / wall:.2f}')" >> "$csv"
            log "  run $run, $setting done"
        done
    done
    set_switch false '^Low rate'
}

# ---------------------------------------------------------------- Main

section=${1:-all}
log "Sensor Lab measurements ($section), results in $OUT"
setup
case "$section" in
    accelerometer) measure_accelerometer ;;
    barometer) measure_barometer_steps; measure_barometer_floors ;;
    gps) measure_gps ;;
    rate) measure_rate ;;
    cpu) measure_cpu ;;
    all)
        measure_accelerometer
        measure_barometer_steps
        measure_barometer_floors
        measure_gps
        measure_rate
        measure_cpu
        ;;
    *) die "unknown section '$section' (all, accelerometer, barometer, gps, rate, cpu)" ;;
esac
log "Done"
