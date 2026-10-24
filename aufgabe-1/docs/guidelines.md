# Guidelines – Task 1 (sensor app)

Pinned best practices for this app. Each rule was checked against the official documentation on **2026-10-24**.

How they're used:
- **Spec (Part A):** the most important rules go into the spec as non-functional requirements (see [Candidates for the spec](#candidates-for-the-spec)).
- **Generation (Part B):** the full list was given to the generating AI as coding rules, alongside the spec.
- **Review (Part C):** the full list is the review checklist and extends the checklist in the task PDF. Every finding cites a rule ID.

Levels: **MUST** = "Strongly recommended" in the source, **SHOULD** = "Recommended", **OWN** = our own decision without an official source; the rationale is given.

## Architecture

| ID | Level | Rule | Source |
|---|---|---|---|
| ARCH-1 | MUST | Sensors are data sources. Only a repository in the data layer talks to `SensorManager`; composables and ViewModels never do. The repository exposes each sensor as a `Flow`, built with `callbackFlow` whose `awaitClose { }` unregisters the listener. | [Arch. rec. › Layered architecture][ar-layer], [Kotlin flows › Callback-based APIs][flow-cb] |
| ARCH-2 | MUST | Unidirectional data flow: the ViewModel exposes one `uiState: StateFlow<…UiState>`, created with `stateIn(…, SharingStarted.WhileSubscribed(5000), …)`. The UI sends actions as method calls. No mutable types are exposed. | [Arch. rec. › UI layer][ar-ui], [› ViewModel][ar-vm], [Coroutines › Don't expose mutable types][co-mut] |
| ARCH-3 | MUST | ViewModels hold no `Activity`, `Context` or `Resources` and don't extend `AndroidViewModel`. Dependencies come in through the constructor; manual DI is enough at this size, so no Hilt. | [Arch. rec. › ViewModel][ar-vm], [› Handle dependencies][ar-di] |

## Sensors and lifecycle

| ID | Level | Rule | Source |
|---|---|---|---|
| SENS-1 | MUST | Sensors are only listened to while the UI is visible: `collectAsStateWithLifecycle()` → `WhileSubscribed` → `awaitClose` unregisters. No overridden Activity lifecycle callbacks; use lifecycle-aware effects (`LifecycleStartEffect`) where needed. *Trade-off to state in the spec:* `WhileSubscribed(5000)` keeps listeners registered for up to 5 s after leaving, so a screen rotation doesn't restart them. | [Sensors › Unregister sensor listeners][s-unreg], [› Only gather sensor data in the foreground][s-fg], [Arch. rec. › Lifecycle][ar-lc] |
| SENS-2 | MUST | Check that each sensor exists before use (`getDefaultSensor()` can return `null`). A missing sensor shows a clear "not available" state, doesn't crash the app, and the other sensors keep working. | [Sensors › Verify sensors before you use them][s-verify] |
| SENS-3 | MUST | `onSensorChanged()` only copies values and the timestamp and emits them. Filtering, aggregation and threshold checks happen downstream. | [Sensors › Don't block onSensorChanged()][s-block] |
| SENS-4 | MUST | The spec fixes each sensor's delay, justified by its purpose. No `SENSOR_DELAY_FASTEST` without a reason. With `targetSdk` ≥ 31, rates are capped at 200 Hz unless `HIGH_SAMPLING_RATE_SENSORS` is declared, and we don't declare it. | [Sensors › Choose sensor delays carefully][s-delay], [› Sensor rate-limiting][s-rate] |
| SENS-5 | MUST | Units as defined by `SensorEvent.values`: m/s², µT, hPa, lx, cm. Derived values use the platform formulas (`SensorManager.getAltitude()`, `getRotationMatrix()` + `getOrientation()`). No deprecated sensor types (`TYPE_ORIENTATION`, `TYPE_TEMPERATURE`). | [SensorEvent › values][se-values], [SensorManager][sm], [Sensors › Avoid deprecated methods or sensor types][s-depr] |

## UI (Jetpack Compose)

| ID | Level | Rule | Source |
|---|---|---|---|
| UI-1 | MUST | A new reading recomposes only what shows it. Read fast-changing state as late as possible: pass lambdas and read inside the chart's draw phase. Cache expensive calculations with `remember`. Use `derivedStateOf` (or `computedStateOf` with Compose Runtime ≥ 1.13) for derived states such as "threshold exceeded". | [Compose performance › remember][cp-remember], [› computedStateOf/derivedStateOf][cp-derived], [› Defer reads][cp-defer] |
| UI-2 | OWN | Memory stays bounded, and the UI refresh rate is decoupled from the sensor rate. Chart history lives in a fixed-size ring buffer (last *N* seconds), and the display updates at most at the rate the spec defines. *Rationale:* the task PDF's checklist asks "Is the UI redrawn for every reading? Does a list grow without bounds?" | Task PDF, Part C checklist |

## Permissions and notifications

| ID | Level | Rule | Source |
|---|---|---|---|
| PERM-1 | MUST | Ask for permissions in context, when the user turns on the feature that needs them, not at app start. Use `rememberLauncherForActivityResult` + `ActivityResultContracts.RequestPermission`. `POST_NOTIFICATIONS` is declared in the manifest, and before posting the app checks `areNotificationsEnabled()`. | [Permissions › Basic principles][p-basic], [Notification permission › Request the permission][np-req], [› Check whether your app can send notifications][np-check] |
| PERM-2 | MUST | Denial degrades gracefully. A specific message appears where the feature's data would be, the rest of the app keeps working, nothing blocks the screen, and the app doesn't nag. A permanent denial (denied twice) is handled with a pointer to the system settings. | [Permissions › Handle permission denial][p-deny] |
| PERM-3 | MUST | *Only if location is used:* foreground location only, `ACCESS_COARSE_LOCATION` unless the spec needs precise location. | [Permissions › Foreground location][p-loc] |

## Privacy and logging

| ID | Level | Rule | Source |
|---|---|---|---|
| PRIV-1 | OWN | Location data is neither stored nor logged beyond what the spec requires; any logged position has reduced precision. Note for Part E: Android rate-limits motion and position sensors "to protect potentially sensitive information", so the platform itself treats this data as sensitive. | Task PDF, Part C checklist; [Sensors › Sensor rate-limiting][s-rate] |
| LOG-1 | OWN | The processing chain is logged with one tag per stage (sensor event → processing → display) at DEBUG level, and is off in release builds (`Log.isLoggable` or a build flag). High-rate sensors are logged sampled, not every event. Rate measurements use `SensorEvent.timestamp` (ns, same time base as `elapsedRealtimeNanos()`), not the wall clock. | [Log › isLoggable][log], [SensorEvent › timestamp][se-ts] |

## Code and dependencies

| ID | Level | Rule | Source |
|---|---|---|---|
| CODE-1 | MUST | Kotlin coding conventions and the Android Kotlin style guide. | [Kotlin coding conventions][kt-conv], [Android Kotlin style guide][kt-style] |
| CODE-2 | MUST | All dependencies live in `gradle/libs.versions.toml` with exact versions (no `+`). Compose libraries come through the Compose BOM. Only libraries the spec allows. Every dependency and version is confirmed to exist. Deprecation warnings are fixed or justified. | [Migrate to version catalogs][cat], [Compose BOM › Why the BOM is recommended][bom] |

## PDF checklist → rules

| Task PDF checklist item (Part C) | Rules |
|---|---|
| Lifecycle: listeners unregistered when not visible | SENS-1, ARCH-1 |
| Permissions: requested at runtime, denial handled | PERM-1, PERM-2, PERM-3 |
| Missing sensors: no crash | SENS-2 |
| Units and formulas | SENS-5 |
| Sampling rate and energy | SENS-4, SENS-3 |
| UI and memory: redraw per reading, unbounded lists | UI-1, UI-2 |
| APIs and libraries: deprecated or non-existent | CODE-2, SENS-5 |
| Privacy: location stored or logged | PRIV-1, LOG-1 |

## Candidates for the spec

The spec is limited to one page, so it only carries the rules that shape the design. Final choice when writing the spec:
- ARCH-1 + ARCH-2: repository → ViewModel → UI with Flows
- SENS-1: listeners only while visible, including the 5 s trade-off
- UI-2: ring-buffer size and UI refresh rate
- LOG-1: tags per stage

The PDF already requires the spec to cover missing sensors, denied permissions and background behaviour, so SENS-2 and PERM-2 are covered there anyway.

[ar-layer]: https://developer.android.com/topic/architecture/recommendations#layered-architecture
[ar-ui]: https://developer.android.com/topic/architecture/recommendations#ui-layer
[ar-vm]: https://developer.android.com/topic/architecture/recommendations#viewmodel
[ar-lc]: https://developer.android.com/topic/architecture/recommendations#lifecycle
[ar-di]: https://developer.android.com/topic/architecture/recommendations#handle-dependencies
[co-mut]: https://developer.android.com/kotlin/coroutines/coroutines-best-practices#mutable-types
[flow-cb]: https://developer.android.com/kotlin/flow#callback
[s-unreg]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#unregister-sensor-listeners
[s-fg]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#only-gather-sensor-data-in-the-foreground
[s-verify]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#verify-sensors-before-you-use-them
[s-block]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#dont-block-the-onsensorchanged-method
[s-delay]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#choose-sensor-delays-carefully
[s-rate]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#sensors-rate-limiting
[s-depr]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#avoid-using-deprecated-methods-or-sensor-types
[se-values]: https://developer.android.com/reference/android/hardware/SensorEvent#values
[se-ts]: https://developer.android.com/reference/android/hardware/SensorEvent#timestamp
[sm]: https://developer.android.com/reference/android/hardware/SensorManager
[cp-remember]: https://developer.android.com/develop/ui/compose/performance/bestpractices#use-remember
[cp-derived]: https://developer.android.com/develop/ui/compose/performance/bestpractices#use-derivedstateof
[cp-defer]: https://developer.android.com/develop/ui/compose/performance/bestpractices#defer-reads
[p-basic]: https://developer.android.com/training/permissions/requesting#principles
[p-deny]: https://developer.android.com/training/permissions/requesting#handle-denial
[p-loc]: https://developer.android.com/training/permissions/requesting#foreground-location
[np-req]: https://developer.android.com/develop/ui/views/notifications/notification-permission#request-compose
[np-check]: https://developer.android.com/develop/ui/views/notifications/notification-permission#check-app-can-send
[log]: https://developer.android.com/reference/android/util/Log#isLoggable(java.lang.String,%20int)
[kt-conv]: https://kotlinlang.org/docs/coding-conventions.html
[kt-style]: https://developer.android.com/kotlin/style-guide
[cat]: https://developer.android.com/build/migrate-to-catalogs
[bom]: https://developer.android.com/develop/ui/compose/bom#why-bom-recommended
