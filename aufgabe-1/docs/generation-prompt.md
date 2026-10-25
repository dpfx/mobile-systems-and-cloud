# Generation prompt – Task 1 (Part B)

The app was generated from this prompt: a short process wrapper plus `specification.md`, copied verbatim as committed in `fcb0a85`. The text below was taken from the Claude Code session log, i.e. exactly what the sub-agent received.

| | |
|---|---|
| Date and time | 2026-10-24, 17:49–18:11 CEST |
| Tool | Claude Code sub-agent (general-purpose), model Claude Opus 5.5 |
| Result | commit `ea661d8` ("raw AI output: Sensor Lab app generated from the spec") |
| Loaded automatically | `CLAUDE.md`, `aufgabe-1/CLAUDE.md` |
| Read by the agent | `docs/guidelines.md` (as instructed), `docs/specification.md`, `docs/ai-errors.md` |

## Prompt (verbatim)

```text
This run is approved by the user. Work autonomously; there is no user to
ask during this run.

Task: implement the specification below in the existing Android project
in aufgabe-1/ (skeleton with Gradle wrapper, app module and version
catalog already in place). The specification is the only source of
requirements. Follow the coding rules in aufgabe-1/docs/guidelines.md
while implementing.

Process:
- Only change files needed for the app (in aufgabe-1/, outside docs/).
- Build with ./gradlew assembleDebug in aufgabe-1/ and fix build errors
  until the build succeeds. Do not use the emulator or adb.
- Do not stage, commit or push anything in Git.
- Where the specification is unclear, make a reasonable assumption
  instead of asking.
- At the end, report: (1) the files you created or changed, (2) every
  build error you hit and how you fixed it, (3) every assumption you made
  where the specification was unclear, (4) any documentation you looked up.

Specification:
---
# Specification – Sensor Lab

Hiking companion: measures movement, altitude change and distance walked, warns of strong shocks; every value must be verifiable. Android, Kotlin, one screen, minSdk 26, compile/targetSdk 37.

## 1. Sensors, quantities, rates

| Sensor | Displayed (units) | Requested rate |
|--------|---------------------|---------------|
| Accelerometer | x, y, z and \|a\| = √(x² + y² + z²) in m/s²; \|a\| also in g (1 g = 9.80665 m/s²) | `SENSOR_DELAY_GAME` (20 ms); switch: `SENSOR_DELAY_NORMAL` |
| Barometer | pressure (hPa); altitude change Δh since start (m) | `SENSOR_DELAY_NORMAL` (200 ms) |
| GPS (`LocationManager`) | latitude, longitude (°, 5 decimals); accuracy (m); distance since start (m) | 1 fix per second |

- Δh: `SensorManager.getAltitude()` (standard atmosphere), relative to the start.
- Distance uses only fixes with accuracy ≤ 20 m and grows only when the step from the last counted fix exceeds the new fix's accuracy.
- **Reset** restarts Δh and distance. Each sensor shows its requested and measured event rate (Hz over the last 2 s, from event timestamps).

## 2. Display modes

Tabs **Values** (card per sensor: values, requested/measured rate, status) and **Chart** (Vico `compose-m3:3.3.1`: one line chart each for |a|, Δh and distance over the last 60 s). History: 60 s ring buffer, ≤ 10 points/s; UI updates ≤ 10 times/s, independent of sensor rates.

## 3. Warning rule

|a| ≥ 25 m/s² (≈ 2.5 g) triggers an in-app banner "Strong shock detected" (time, peak |a|) and, if the **Shock alerts** switch is on, a notification; at most once per 30 s. Turning the switch on (off by default) requests `POST_NOTIFICATIONS`. At `SENSOR_DELAY_NORMAL` the card notes reduced reliability.

## 4. Missing sensors and permissions

- Missing sensor: "Not available on this device" on its card, chart hidden, the rest keeps working.
- Location (fine + coarse) via **Enable GPS** on the GPS card. Denied: explanation + **Grant** (opens app settings after permanent denial). Approximate only: no distance. GPS off: the card says so. Permissions are re-checked whenever the app returns to the screen.
- Notifications denied: banner only, hint at the switch. Never block the screen or ask repeatedly.

## 5. Background

Sensors and GPS run only while visible: they stop 5 s after leaving the screen (survives rotation) and resume on return. Tracking pauses, also with the screen off (accepted); the chart shows a gap, distance resumes from the next fix. No background location, foreground service or disk storage.

## 6. Logging

Logcat, debug builds only: `SensorLab.Sensor` (raw events, measured rate), `SensorLab.Process` (derived values, fix decisions, warnings), `SensorLab.UI` (displayed values). At most one line per sensor and tag per second; warnings and state changes always. Positions rounded to 3 decimals.

## 7. Architecture

Repository per source (`callbackFlow`, `awaitClose` unregisters); `onSensorChanged()` only forwards. ViewModel: `StateFlow` via `stateIn(WhileSubscribed(5000))`, no `Context`, constructor injection. UI: `collectAsStateWithLifecycle()`. Only AndroidX, Compose (BOM, Material 3), Vico, all pinned.
---
```
