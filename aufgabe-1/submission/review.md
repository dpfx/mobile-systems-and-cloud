# Part C – Code review

**Reviewed version:** the raw AI output, commit [`ea661d8`][raw]; code locations refer to it, relative to `app/src/main/java/de/penguin/sensorlab/`. **Method:** each checklist item was checked in the code against our coding guidelines and, where reading isn't enough, on the emulator (Android 17) with the app's log, Android's event log and `dumpsys`.

| # | Checklist item | Verdict | Findings |
|---|---|---|---|
| 1 | Lifecycle: listeners unregistered when not visible | correct | – |
| 2 | Permissions: requested at runtime, denial handled | correct | – |
| 3 | Missing sensors: no crash | correct | – |
| 4 | Units and formulas | correct, but distance without plausibility check | F1 |
| 5 | Sampling rate and energy | appropriate | – |
| 6 | UI and memory: redraw per reading, unbounded lists | correct | – |
| 7 | APIs and libraries: deprecated or non-existent | correct, but one library not declared | F2 |
| 8 | Privacy: location stored or logged | correct | – |

Beyond the checklist: findings F3–F5 (section 9). All five findings were fixed, one commit each.

## 1. Lifecycle – correct

- **Code:** the UI collects the state only while the screen is visible (`collectAsStateWithLifecycle()`, `ui/SensorLabScreen.kt:58`); all sources run behind `stateIn(…, WhileSubscribed(5000))` (`ui/SensorLabViewModel.kt:85-97`), and cancelling them runs `awaitClose`, which unregisters the listeners (`data/SensorRepository.kt:58-61`, `data/LocationRepository.kt:105-108`).
- **Emulator:** Home at 13:55:00.474 → `13:55:05.479 … Accelerometer: listener unregistered`, barometer and GPS within 5 ms; back in the app, all three are registered again; a rotation recreates the screen without unregistering anything.

## 2. Permissions – correct

- **Code:** location is requested only via **Enable GPS**, notifications only when **Shock alerts** is turned on (`ui/ValuesTab.kt:136-142`, `ui/SensorLabScreen.kt:101-110`); after a permanent denial, **Grant** opens the app settings (`ui/SensorLabScreen.kt:112-125`); permissions are re-checked on every return to the screen (`ui/SensorLabScreen.kt:64-66`).
- **Emulator:** a denial shows an explanation and **Grant**; after a permanent denial, **Grant** opens App info; approximate location shows the position but no distance; with notifications blocked, the warning appears as a banner only, with a hint at the switch; revoking location in the background causes no crash.

## 3. Missing sensors – correct

- **Code:** `getDefaultSensor()` may return `null`; then nothing is registered and the card shows "Not available on this device" (`data/SensorRepository.kt:30-33`, `ui/ValuesTab.kt:54-55`).
- **Emulator** with the barometer removed (`hw.sensors.pressure=no`): barometer card "Not available on this device", Δh chart hidden, the accelerometer at 50 Hz and the shock warning still working, 0 crashes.

## 4. Units and formulas – correct, one finding

| Quantity | Code | Formula / unit |
|---|---|---|
| x, y, z | `ui/SensorLabViewModel.kt:190-192` | `SensorEvent.values` in m/s² |
| \|a\|, in g | `ui/SensorLabViewModel.kt:193`, `ui/Format.kt:21` | √(x² + y² + z²); divided by `SensorManager.STANDARD_GRAVITY` (9.80665) |
| Δh | `ui/SensorLabViewModel.kt:269-277` | `getAltitude(PRESSURE_STANDARD_ATMOSPHERE, p)` minus the value at the start |
| Distance | `processing/DistanceTracker.kt:79-83` | `Location.distanceBetween` (WGS84) |
| Measured rate | `processing/RateMeter.kt:21-26` | (events − 1) / time span within 2 s, from the events' timestamps |

Repeated measurements with known inputs: Part D.

**F1 – GPS distance has no plausibility check**

| | |
|---|---|
| **Location** | `processing/DistanceTracker.kt:60-65` |
| **Problem** | Every step longer than the fix's accuracy is added, however implausible. The code follows the spec, which sets no limit: a spec gap, not an AI error. |
| **Impact** | On the emulator, a jump from the default position (Mountain View) to the test position within 1.7 s was counted as **9,359,974 m** (`counted: step 9359974.0 m > accuracy 5.0 m`). |
| **Fix** | A step that implies more than 10 m/s is not counted, and its fix becomes the new reference; spec amendment A3. Verified: a jump to Mountain View now gives `rejected: … = 1336250.1 m/s > 10 m/s, new reference`, and the distance stays at 111.2 m. Commit [`6638a12`][f1]. |

## 5. Sampling rate and energy – appropriate

The accelerometer runs at `GAME` (50 Hz) because the shock warning needs it: an impact lasts only tens of milliseconds, and at 5 Hz a peak can fall between two samples. The barometer runs at `NORMAL`, the slowest preset, since pressure changes slowly; GPS delivers 1 fix per second and runs only while the screen is visible. The requested periods reach Android unchanged (`dumpsys sensorservice`: `samplingPeriod= 20000us` / `200000us`). At `NORMAL`, Android delivers 15 Hz instead of 5 Hz (Part D, section 4).

## 6. UI and memory – correct

- **Code:** the screen gets a new state snapshot every 100 ms, not per reading (`ui/SensorLabViewModel.kt:85-92`); the chart history is a ring buffer of 600 points per quantity (`processing/RingBuffer.kt`, `processing/History.kt:21`).
- **Emulator:** 2 frames rendered in 10 s on the idle Values tab and 99 on the Chart tab (≈ 10/s, per snapshot, not per reading); the Java heap stayed between 12.0 and 16.5 MB over 2 minutes, with no upward trend.

## 7. APIs and libraries – correct, one finding

The project builds cleanly from scratch, a full recompile shows 0 compiler warnings (so no deprecated APIs), and all pinned versions were current on 2026-10-25 (AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Vico 3.3.1).

**F2 – `kotlinx-coroutines` is used but not declared or pinned**

| | |
|---|---|
| **Location** | `app/build.gradle.kts`, `gradle/libs.versions.toml`; used in 16 imports, e.g. `data/SensorRepository.kt:8-11` |
| **Problem** | The library reaches the app only indirectly through AndroidX, so its version is whatever Gradle resolves (1.11.0); spec section 7 requires every library to be pinned. Root cause is a spec gap: the spec requires `callbackFlow` and `StateFlow`, which come from this library, but its library list leaves it out, and the AI resolved this silently. |
| **Impact** | No effect today; any AndroidX update can change the coroutines version unnoticed. |
| **Fix** | Declared with version 1.11.0; spec amendment A1. Verified: Gradle lists it as a direct dependency at 1.11.0, and the build succeeds. Commit [`ef533ad`][f2]. |

## 8. Privacy – correct

- **Code:** no storage API anywhere; positions are logged with 3 decimals only (`ui/SensorLabViewModel.kt:302-306`, `ui/ValuesTab.kt:194`), and logging is off in release builds (`log/ChainLog.kt:21`).
- **Emulator:** the app's data folder holds only an AndroidX marker file; 0 of 1,370 log lines with a position have more than 3 decimals.

## 9. Beyond the checklist

**F3 – A missing sensor is not logged**

| | |
|---|---|
| **Location** | `ui/SensorLabViewModel.kt:153, 163, 170` |
| **Problem** | If a sensor or GPS is missing, its source returns without a log line, although spec section 6 requires state changes to be logged always. |
| **Impact** | With the barometer removed, the log says nothing about it, so it can't tell a missing sensor from one that sends no events. |
| **Fix** | Each source logs "not available on this device". Verified: `Barometer: not available on this device` with the barometer removed. Commit [`249f929`][f3]. |

**F4 – Tapping the notification opens a second screen with fresh data**

| | |
|---|---|
| **Location** | `data/ShockNotifier.kt:50-55` |
| **Problem** | The notification starts `MainActivity` without launch flags, so Android creates a second instance with its own ViewModel. Opening the app from the notification isn't in the spec; the AI added it. |
| **Impact** | With the app open, a tap created a new instance on top (`wm_on_create_called`). By the code, it starts with Δh and distance at 0, Shock alerts off and an empty chart, while the real data sits underneath. |
| **Fix** | The notification uses the app's launcher intent, which brings the existing screen to the front; spec amendment A2. Verified: a tap leaves the same instance in front, with no new `wm_on_create_called`. Commit [`ad46acf`][f4]. |

**F5 – No app icon**

| | |
|---|---|
| **Location** | `app/src/main/AndroidManifest.xml:21` (no `android:icon`) |
| **Problem** | The project skeleton, created by the AI, has no launcher icon, although it was described as equivalent to Android Studio's template. Lint: `MissingApplicationIcon`. |
| **Impact** | Android's generic placeholder icon on the home screen and in the notification. |
| **Fix** | An adaptive vector icon. Verified: Lint no longer reports `MissingApplicationIcon`. Commit [`0e2987c`][f5]. |

Full version with all excerpts and emulator tables: [`docs/review.md`][full].

[raw]: https://github.com/dpfx/mobile-systems-and-cloud/tree/ea661d8/aufgabe-1
[f1]: https://github.com/dpfx/mobile-systems-and-cloud/commit/6638a12
[f2]: https://github.com/dpfx/mobile-systems-and-cloud/commit/ef533ad
[f3]: https://github.com/dpfx/mobile-systems-and-cloud/commit/249f929
[f4]: https://github.com/dpfx/mobile-systems-and-cloud/commit/ad46acf
[f5]: https://github.com/dpfx/mobile-systems-and-cloud/commit/0e2987c
[full]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/review.md
