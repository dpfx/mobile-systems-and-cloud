# Code review – Task 1 (Part C)

**Reviewed version:** commit `ea661d8` (raw AI output, generated from `specification.md`).

**Method:** for each item of the checklist in the task PDF, the relevant code was read and matched against `guidelines.md` (rule IDs in brackets). Where reading the code isn't enough, the behaviour was checked on the emulator (Android 17, API 37) with the app's log (`SensorLab.*` tags), Android's events log and `dumpsys sensorservice`. Times in the excerpts are emulator time (UTC).

## Overview

| # | Checklist item | Verdict | Findings |
|---|---|---|---|
| 1 | Lifecycle: listeners unregistered when not visible | correct | – |
| 2 | Permissions: requested at runtime, denial handled | correct | – |
| 3 | Missing sensors: no crash | correct | – |
| 4 | Units and formulas | correct, but distance without plausibility check | F1 |
| 5 | Sampling rate and energy | appropriate | – |
| 6 | UI and memory: redraw per reading, unbounded lists | correct | – |
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
| Home (`wm_on_stop_called` at 13:55:00.474) | all sources stopped after ≈ 5 s | `13:55:05.479 I/SensorLab.Sensor: Accelerometer: listener unregistered`, `13:55:05.483 … Barometer: listener unregistered`, `13:55:05.484 … GPS: updates removed`; sensorservice lists no active connection of the app (first run at 13:22) |
| Back to the app (same activity instance) | all sources registered again | events log: `wm_on_start_called` / `wm_on_resume_called` for the same instance at 13:55:07.452, no new `performCreate`; `13:55:07.457 … Barometer: listener registered (delay 3)`, `13:55:07.459 … Accelerometer: listener registered (delay 1)`, `13:55:07.460 … GPS: updates requested` |
| Rotation to landscape and back | activity recreated, listeners keep running | events log: `13:23:31 wm_on_destroy_called` → `wm_on_create_called` (again at 13:23:35); no `SensorLab.Sensor` line in between |
| Rate switch on, then off | exactly one unregister + register each | `13:24:11.623 unregistered` → `13:24:11.625 registered (delay 3)`; `13:24:15.940 unregistered` → `13:24:15.942 registered (delay 1)` |

## 2. Permissions (PERM-1, PERM-2, PERM-3)

**Verdict: correct.** This item also covers the GPS part of item 1.

**How we know – code:**

- `ui/ValuesTab.kt:136-142`, `ui/SensorLabScreen.kt:101-110`: location is only requested via **Enable GPS**, notifications only when **Shock alerts** is turned on (Android 13+), i.e. in context.
- `ui/ValuesTab.kt:144-150`: after a denial the card names what's missing and offers **Grant**.
- `ui/SensorLabScreen.kt:112-125`: if `shouldShowRequestPermissionRationale()` is false after a denial (permanent denial), **Grant** opens the app settings instead of a dialog that Android would no longer show.
- `ui/SensorLabViewModel.kt:308-312`, `ui/ValuesTab.kt:172-177`: approximate location shows the position but no distance, with an explanation.
- `ui/SensorLabScreen.kt:64-66`: `LifecycleEventEffect(ON_START)` re-checks all permissions and the GPS switch whenever the screen becomes visible.
- `ui/ValuesTab.kt:90-95`, `data/ShockNotifier.kt:36-38`: with notifications blocked, the switch shows a hint and only the banner is used; `areNotificationsEnabled()` is checked before posting.

**How we know – emulator (2026-10-25):**

| Action | Expected | Observed (excerpts) |
|---|---|---|
| Enable GPS → Don't allow | specific note, **Grant** | card: "Location permission was denied, so position and distance can't be shown. The other sensors keep working. Tap Grant to allow location."; log `13:44:50.919 GPS: location permission denied` |
| Grant → Don't allow again | Android marks the denial permanent | dialog shown again; permission flags `USER_SET\|USER_FIXED` |
| Grant after permanent denial | app settings open | top activity `com.android.settings/.spa.SpaActivity` (App info of Sensor Lab); Back returns to the app |
| Enable GPS → Approximate | position, no distance, note | card: accuracy `2000.0 m`, distance `–`, "Approximate location only: the distance is not measured…"; log `13:47:20.183 GPS: approximate location only: no distance` |
| Upgrade to precise, leave and return | re-check on return, GPS starts | `13:45:46.907 GPS: location permission: Precise`, `13:45:46.925 GPS: updates requested (every 1000 ms)` |
| Home with GPS running | updates removed after ≈ 5 s | `wm_on_stop_called` at 13:45:57.335 → `13:46:02.342 GPS: updates removed`; location service: registration `Request[@+1s0ms HIGH_ACCURACY]` while visible, none afterwards; its statistics: active 15.4 s vs. foreground 10.4 s |
| Revoke location while in the background, reopen | no crash, back to Enable GPS | Android ends the process (pid 22166 → new pid 22328); card "GPS needs the location permission… Enable GPS"; 0 crashes |
| System location off / on while visible | card says GPS is off | `13:48:56.922 GPS: GPS switched off`, note "GPS is off. Turn on location in the system settings…"; `13:49:10.633 GPS: GPS switched on` |
| Shock alerts on → Don't allow, then a shock | hint at the switch, banner only | note "Notifications are blocked, so shocks are only shown as a banner in the app…"; log `13:48:28.858 Strong shock detected: … banner shown, no notification (notifications blocked)` |

## 3. Missing sensors (SENS-2)

**Verdict: correct.** Tested at runtime for the barometer; the "no GPS" path is verified by reading the code only (see below).

**How we know – code:**

- `data/SensorRepository.kt:30-33, 42`: `getDefaultSensor()` may return `null`; then `isAvailable` is false and `samples()` returns an empty flow, so nothing is registered.
- `ui/SensorLabViewModel.kt:153, 163, 170`: a missing source is simply not started; the other sources run independently.
- `ui/ValuesTab.kt:54-55, 108-109, 134` and `ui/SensorLabViewModel.kt:360-362`: the card shows "Not available on this device" and the chart of that source is hidden (`null`).
- `data/LocationRepository.kt:47-49`: GPS counts as missing if `LocationManager` has no GPS provider; the card then shows "Not available on this device" as well.

**How we know – emulator (2026-10-25):** the barometer was removed from the emulator (`hw.sensors.pressure=no` in the AVD's `config.ini`, cold boot; restored afterwards). Android then listed no pressure sensor (`dumpsys sensorservice`) and the emulator console reported `pressure: disabled`.

| Check | Expected | Observed |
|---|---|---|
| Values tab | barometer card "Not available", the rest works | barometer: "Not available on this device"; accelerometer 50.0 Hz; GPS updates requested |
| Chart tab | Δh chart hidden | charts shown: \|a\| and distance only |
| Shock with \|a\| = 30 m/s² | warning still works | banner "Strong shock detected"; log `14:01:10.692 Strong shock detected: …` |
| Stability | no crash | 0 entries in the crash log |

`hw.gps=no` did **not** remove the emulator's GPS provider (`dumpsys location` still lists an enabled `gps provider`), so "no GPS" could not be tested at runtime.

## 4. Units and formulas (SENS-5)

**Verdict: units and conversions are correct; one finding (F1) on the distance.**

**How we know – code:**

| Quantity | Code | Formula / unit |
|---|---|---|
| x, y, z | `ui/SensorLabViewModel.kt:190-192` | `SensorEvent.values` in m/s² |
| \|a\| | `ui/SensorLabViewModel.kt:193` | √(x² + y² + z²) |
| g | `ui/Format.kt:21` | \|a\| / `SensorManager.STANDARD_GRAVITY` (9.80665), not a hand-typed 9.81 |
| Δh | `ui/SensorLabViewModel.kt:269-277, 376-377` | `getAltitude(PRESSURE_STANDARD_ATMOSPHERE, p)` minus the value at the start |
| Distance | `processing/DistanceTracker.kt:79-83` | `Location.distanceBetween` (WGS84 ellipsoid) |
| Measured rate | `processing/RateMeter.kt:21-26` | (events − 1) / time span within 2 s, from the events' own timestamps (ns) |
| Requested rate | `ui/SensorLabUiState.kt:8-13` | `GAME` 20 ms = 50 Hz, `NORMAL` 200 ms = 5 Hz |

**How we know – emulator spot checks (2026-10-25):** inputs with a known result; repeated measurements follow in Part D.

| Check | Input | Expected | App shows |
|---|---|---|---|
| \|a\| and g | acceleration (3, 4, 12) m/s² | 13.00 m/s² = 1.326 g (3² + 4² + 12² = 13²) | `13.00 m/s² = 1.326 g` |
| Δh | Reset at 1013.25 hPa, then 1012.89 hPa | 3.00 m | `3.12 m` at `1012.88 hPa` |
| Distance | Reset, then 0.001° further north at 48.21° N | 111.19 m (WGS84 meridian) | `111.2 m` |
| Rate at `GAME` | – | Android's own event record: 50 events in 0.980 s = 50.0 Hz | `50.0 Hz` |
| Rate at `NORMAL` | – | Android's own event record: 50 events in 3.267 s = 15.0 Hz | `15.0 Hz` |

The Δh deviation comes from the emulator, not the app: the app's log shows that it received `p=1012.875 hPa` (the emulator rounds to steps of 0.125 hPa), and the platform formula gives exactly 3.122 m for that pressure. At `NORMAL`, Android delivers 15 Hz instead of the requested 5 Hz; the app measures this correctly (more in Part D).

### F1 – GPS distance has no plausibility check

| | |
|---|---|
| **Location** | `processing/DistanceTracker.kt:60-65` |
| **Problem** | Every step longer than the fix's accuracy is added, however implausible. The code follows the spec, which sets no limit (spec gap, not an AI error). |
| **Impact** | On the emulator, a jump from the default position (Mountain View) to the test position (Albstadt) within 1.7 s was counted as **9,359,974 m** (log: `counted: step 9359974.0 m > accuracy 5.0 m`). On a real phone, GPS outliers of tens of metres with an optimistic accuracy value inflate the distance in the same way. |
| **Fix** | Open: decided after the review. |

## 5. Sampling rate and energy (SENS-3, SENS-4)

**Verdict: appropriate for the purpose.**

**How we know – code, plus the measurements from items 1 and 4:**

| Aspect | Code | Assessment |
|---|---|---|
| Accelerometer at `SENSOR_DELAY_GAME` (50 Hz) | `ui/SensorLabViewModel.kt:51, 156-159` | Needed for the shock warning: an impact lasts only tens of milliseconds, at 5 Hz a peak can fall between two samples. The card says so when the rate is switched to `NORMAL`. |
| Barometer at `SENSOR_DELAY_NORMAL` (5 Hz) | `ui/SensorLabViewModel.kt:386` | Pressure changes slowly; `NORMAL` is the slowest of Android's presets. |
| GPS: 1 fix per second, high accuracy | `data/LocationRepository.kt:84-86, 124` | Matches walking speed (≈ 1.4 m/s). GPS is the largest energy consumer and runs only while the screen is visible (item 1). |
| No `SENSOR_DELAY_FASTEST`, no `HIGH_SAMPLING_RATE_SENSORS` | `AndroidManifest.xml`, `ui/SensorLabUiState.kt:8-13` | No more data than needed (SENS-4). |
| `onSensorChanged()` only copies and forwards | `data/SensorRepository.kt:46-48` | Little work per event (SENS-3). |
| Requested rates reach Android unchanged | sensorservice (item 1) | `samplingPeriod= 20000us` / `200000us`. |
| No batching (`maxReportLatencyUs`) | – | Not needed: batching mainly saves energy while the screen is off and the processor could sleep; this app only measures with the screen on, and batching would delay the shock warning. |

At `NORMAL`, Android delivers 15 Hz instead of the requested 5 Hz (item 4), so switching to `NORMAL` saves less than the factor 10 suggests. The consequences for energy use are examined in Part D.

## 6. UI and memory (UI-1, UI-2)

**Verdict: correct.** The UI is not redrawn per reading, and all buffers are bounded.

**How we know – code:**

| Aspect | Code | Assessment |
|---|---|---|
| UI decoupled from the sensors | `ui/SensorLabViewModel.kt:85-92` | Readings arrive at up to 50 Hz; the screen gets a new state snapshot every 100 ms (≤ 10/s, spec section 2). |
| Chart history | `processing/History.kt:21, 69`, `processing/RingBuffer.kt` | Ring buffer of 600 points per quantity (60 s × 10/s); new points overwrite the oldest. |
| Rate measurement | `processing/RateMeter.kt:10-15` | Keeps only the timestamps of the last 2 s. |
| Log sampling | `log/ChainLog.kt:24` | One entry per tag and source (fixed set of keys). |

**How we know – emulator (2026-10-25):**

| Measurement | Result |
|---|---|
| Frames rendered in 10 s on the Values tab, constant inputs (`dumpsys gfxinfo`) | 2: when nothing changes, nothing is redrawn |
| Frames rendered in 10 s on the Chart tab, constant inputs | 99 (≈ 10/s): the time axis moves, so the charts are redrawn per snapshot, not per reading (that would be ≈ 500) |
| App's Java heap on the Chart tab, every 20 s for 2 min (`dumpsys meminfo`) | 12.1 → 15.4 → 12.0 → 12.5 → 16.3 → 16.5 → 13.7 MB: allocation and garbage collection, no upward trend |

The emulator reports every frame as "janky", including the two idle frames on the Values tab; this value is not meaningful on the emulator and is not used.

Optimisation note, not a finding: the chart data (up to 3 × 600 points) is rebuilt for every snapshot, also while the Values tab is shown. It doesn't cause redraws there, only some extra allocation, visible in the heap's sawtooth.

## Candidate findings (to be checked under the remaining items)
- **Tapping the notification may stack a second screen** (`data/ShockNotifier.kt:50-55`): the intent has no launch flags. Not in the spec (generation report, assumption 6).
- **Features beyond the spec:** Dismiss button on the banner, notification tap opens the app (generation report, assumption 6).
- **`kotlinx-coroutines` used but not declared** (CODE-2; generation report, assumption 15).
- **All processing on the main thread** (generation report, assumption 13).
- **`SensorLab.Sensor` lines logged in the ViewModel**, not where the events arrive (generation report, assumption 14).
- **Hard-coded UI texts, no app icon** (lint).
- **A missing sensor is not logged** (`ui/SensorLabViewModel.kt:153, 163`): with no barometer the app logs nothing about it, so the log alone can't tell "missing" from "silent". Spec section 6 asks for state changes to be logged.
