# Generation report – Task 1 (Part B)

Final report of the sub-agent that generated the app (run and prompt: `generation-prompt.md`, result: commit `ea661d8`), received 2026-10-24 18:11 CEST and copied verbatim from the Claude Code session log. It is the AI's own account; its claims are checked in the code review (`review.md`).

---

I've implemented the full spec in `aufgabe-1/`, and `./gradlew assembleDebug` succeeds. The app has not been run: per your instructions I didn't use the emulator or adb, so the permission flows, the notification and the chart drawing have only been compiled, not tried. Nothing was staged or committed, and nothing in `docs/` was touched.

One conflict to know about: `CLAUDE.md` says to ask before changing files and to ask rather than guess when the spec is unclear. This run told me to work on my own and make assumptions, so I did that. Every assumption is listed in section 3.

## 1. Files created or changed
All paths are under `/home/dpfx/projects/mobile-systeme-und-cloud/aufgabe-1/`.

**Changed:**
- `gradle/libs.versions.toml` adds three pinned versions:
  - `androidx.core:core` 1.19.1
  - `lifecycle-runtime-compose` and `lifecycle-viewmodel-compose` 2.11.0
  - `com.patrykandpatrick.vico:compose-m3` 3.3.1
- `app/build.gradle.kts` adds those dependencies and turns on `buildConfig` so logging can be switched off in release builds.
- `app/src/main/AndroidManifest.xml` adds the permissions FINE, COARSE and POST_NOTIFICATIONS. It also marks the accelerometer, barometer and GPS as optional hardware.
- `app/src/main/java/de/penguin/sensorlab/MainActivity.kt` now shows the app screen instead of the placeholder.

**Created** (code under `app/src/main/java/de/penguin/sensorlab/`):
- `AppViewModelProvider.kt`: builds the data sources and passes them into the ViewModel. The ViewModel never gets a Context.
- `log/ChainLog.kt`: the three log tags. Lines are limited to one per second per tag and sensor; warnings and state changes are always logged; everything is off in release builds.
- `data/SensorRepository.kt`, `data/LocationRepository.kt`: one per source, each a `callbackFlow` whose `awaitClose` unregisters. `onSensorChanged` only copies the values and forwards them.
- `data/ShockNotifier.kt`: sends the notification and checks first whether notifications are allowed.
- `processing/`: `RateMeter.kt` (rate over the last 2 s), `RingBuffer.kt`, `History.kt` (600 points for 60 s), `DistanceTracker.kt` (the ≤ 20 m and step > accuracy rules).
- `ui/`: `SensorLabUiState.kt`, `SensorLabViewModel.kt`, `SensorLabScreen.kt`, `ValuesTab.kt`, `ChartTab.kt` (Vico), `Format.kt`.
- `app/src/main/res/drawable/ic_shock.xml`: the notification icon.

## 2. Build errors
There were none. The first `assembleDebug` succeeded, and a forced recompile showed no Kotlin warnings. As an extra check I ran `lintDebug`:
- **One finding in my code, fixed.** Lint flagged `InlinedApi` on `POST_NOTIFICATIONS` in `ShockNotifier.canNotify()`. The core sources show `ContextCompat.checkSelfPermission` handles this permission correctly below Android 13, so I suppressed the warning with a comment explaining why.
- **Two findings from the skeleton, left alone.** Lint says Gradle 9.8.2 is available, and the app has no `android:icon`.

## 3. Assumptions where the spec was unclear
1. **Repositories:** one shared `SensorRepository` class, with one instance for the accelerometer and one for the barometer, plus a `LocationRepository`.
2. **Δh:** the start point is the first pressure reading after app start or Reset. It is kept across pauses, because pressure is absolute.
3. **Distance:**
   - The first fix with accuracy ≤ 20 m becomes the reference point and adds 0 m.
   - After a pause, the next good fix becomes the new reference, so movement during the pause is not counted. The distance total is kept.
   - Fixes without an accuracy value are rejected.
4. **Reset:** sets Δh and distance back to zero and clears their chart history. If a pressure reading exists, Δh shows 0 m immediately. The |a| history is kept.
5. **What counts as one shock:**
   - The warning fires on the first sample with |a| ≥ 25 m/s², if the last warning was at least 30 s ago (measured with event timestamps).
   - Following samples that stay ≥ 25 belong to the same shock and only raise the peak shown in the banner.
   - A shock inside the 30 s cooldown is only logged.
6. **Shock display:** the notification shows |a| and the time at detection (the spec doesn't define its text). The banner shows the peak. The banner stays until the user taps Dismiss (which I added) or a new warning replaces it. Tapping the notification opens the app (which I also added).
7. **Placement:** the "Low rate (SENSOR_DELAY_NORMAL)" and "Shock alerts" switches are on the accelerometer card. Reset is in the header. Switch settings survive rotation but are not saved to disk.
8. **Notification permission:**
   - It is requested only on Android 13 and newer, and only when the switch is turned on while notifications aren't allowed.
   - If the user denies it, the switch stays on and a text hint appears. There is no settings button.
   - The app never asks on its own; Android itself stops showing the dialog after two denials.
9. **Location permission:**
   - FINE and COARSE are requested together.
   - Grant asks again while Android still allows a dialog; otherwise it opens the app settings.
   - The "denied" state is not saved, so after a restart the card shows "Enable GPS" again. Tapping it after a permanent denial leads to the Grant/settings path.
   - If permission was already granted, GPS starts without tapping Enable GPS.
10. **Approximate location:** the app still uses the GPS provider (Android 12+ makes the position coarse), shows position and accuracy, and shows no distance. "GPS off" means the GPS provider is disabled.
11. **Measured rate:** (number of events − 1) divided by the time between the oldest and newest event, over the 2 s before the newest event. It shows 0 Hz when no event arrived in the last 2 s.
12. **Charts:**
    - The |a| chart keeps the highest value of each 100 ms, so short shocks stay visible. Δh and distance keep the latest value.
    - The x-axis is a fixed window of −60 s to 0 s.
    - A gap starts whenever a source restarts after a pause.
13. **Screen updates:** the ViewModel publishes a snapshot every 100 ms. As a side effect, a switch can take up to 100 ms to show its new state. All processing runs on the main thread.
14. **Logging:** raw events are logged in the ViewModel when they arrive, because `onSensorChanged` must only forward. Displayed values are logged from the screen code after it draws.
15. **Libraries and resources:**
    - kotlinx-coroutines is used without being declared (it comes in through the AndroidX libraries), because the spec allows only AndroidX, Compose and Vico.
    - The notification channel uses high importance.
    - UI text is written directly in the code (English only), not in `strings.xml`.

## 4. Documentation I looked up
- **Vico 3.3.1 sources** (Maven Central). I wrote the chart code against the actual 3.x API:
  - `lineSeries` is deprecated; I use `lineModel`.
  - `rememberCartesianChart(getXStep = { _, _, _ -> … })` is the non-deprecated version.
  - Each line series must contain at least one point.
  - Also checked: `CartesianChartHost` with a model, the fixed x-range setting, the axis label placement, and `rememberM3VicoTheme`.
- **androidx.core 1.19.1 sources:** `LocationManagerCompat`, `LocationRequestCompat`, `LocationListenerCompat` (the compat listener avoids a known crash on older Android versions) and `ContextCompat.checkSelfPermission` (how it handles POST_NOTIFICATIONS before Android 13).
- **developer.android.com, `LocationManager` reference:** with only coarse permission, the providers still return locations but coarsened.
- **Versions:** Google Maven listings (latest stable lifecycle 2.11.0 and core 1.19.1), Maven Central metadata for Vico 3.3.1, and the Compose BOM 2026.09.00, which contains material3 1.4.0. The `TabRow` signatures in material3 1.4.0 carry deprecation markers, so I used `PrimaryTabRow`, which compiled without an opt-in.
- The repo's `CLAUDE.md` files and `docs/guidelines.md`, `docs/specification.md`, `docs/ai-errors.md`.

The downloaded sources are in the scratchpad, not in the project.
