# Code review – Task 1 (Part C)

**Reviewed version:** commit `ea661d8` (raw AI output, generated from `specification.md`).

**Method:** for each item of the checklist in the task PDF, the relevant code was read and matched against `guidelines.md` (rule IDs in brackets). Where reading the code isn't enough, the behaviour was checked on the emulator (Android 17, API 37) with the app's log (`SensorLab.*` tags), Android's events log and `dumpsys sensorservice`. Times in the excerpts are emulator time (UTC).

## Overview

| # | Checklist item | Verdict | Findings |
|---|---|---|---|
| 1 | Lifecycle: listeners unregistered when not visible | correct | – |
| 2 | Permissions: requested at runtime, denial handled | open | |
| 3 | Missing sensors: no crash | open | |
| 4 | Units and formulas | open | |
| 5 | Sampling rate and energy | open | |
| 6 | UI and memory: redraw per reading, unbounded lists | open | |
| 7 | APIs and libraries: deprecated or non-existent | open | |
| 8 | Privacy: location stored or logged | open | |

## 1. Lifecycle (SENS-1, ARCH-1)

**Verdict: correct.** GPS location updates are checked under item 2, because they need the location permission.

**How we know – code:**

- `ui/SensorLabScreen.kt:58`: `collectAsStateWithLifecycle()` collects the UI state only while the screen is at least STARTED, i.e. visible.
- `ui/SensorLabViewModel.kt:85-97`: all sources run inside the flow behind `stateIn(…, SharingStarted.WhileSubscribed(5000))`, so they are cancelled 5 s after the last collector disappears.
- `data/SensorRepository.kt:58-61`, `data/LocationRepository.kt:105-108`: cancelling the flow runs `awaitClose`, which calls `unregisterListener` / `removeUpdates`.
- `ui/SensorLabViewModel.kt:156-159`: `collectLatest` cancels the old subscription before the sensor is registered with a new rate, so the old listener doesn't keep running.

**How we know – emulator (2026-10-25):**

| Action | Expected | Observed (excerpts) |
|---|---|---|
| App visible | both listeners active at the requested rates | sensorservice: `13:22:09 + … Accelerometer … samplingPeriod= 20000us`, `13:22:09 + … Pressure sensor … samplingPeriod= 200000us` |
| Home (`wm_on_stop_called` at 13:22:13.010) | listeners unregistered after ≈ 5 s | `13:22:18.023 I/SensorLab.Sensor: Accelerometer: listener unregistered`, `13:22:18.024 … Barometer: listener unregistered`; sensorservice lists no active connection of the app |
| Back to the app | listeners registered again | `13:22:20.151 … Barometer: listener registered (delay 3)`, `13:22:20.153 … Accelerometer: listener registered (delay 1)` |
| Rotation to landscape and back | activity recreated, listeners keep running | events log: `13:23:31 wm_on_destroy_called` → `wm_on_create_called` (again at 13:23:35); no `SensorLab.Sensor` line in between |
| Rate switch on, then off | exactly one unregister + register each | `13:24:11.623 unregistered` → `13:24:11.625 registered (delay 3)`; `13:24:15.940 unregistered` → `13:24:15.942 registered (delay 1)` |
