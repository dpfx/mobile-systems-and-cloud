# Measurements – Task 1 (Part D)

**Tested version:** commit `0e2987c` (after fixes F1–F5), debug build, installed on 2026-10-25 at 15:02 UTC; the app code hasn't changed since. Emulator "Medium Phone API 37.0", Android 17 (API 37).

**Method:** no physical device was available, so every experiment runs on the emulator and injects known values through its console (`emu sensor set`, `emu geo fix`). The script `tools/measure.sh` sets the values and reads what the app received (log tag `SensorLab.Sensor`), what it calculated (`SensorLab.Process`) and, for the event rate, what the Values tab shows. Each scenario ran 5 times (CPU time: 3 times); the raw results are in `docs/measurements/`. The emulator delivers injected values without noise or offset, so the experiments verify the app's processing chain (sensor event → processing → display) against a known input, not the accuracy of real sensors: a deviation comes from the app or from the emulator. Times in excerpts are emulator time (UTC).

## Overview

| # | Experiment | Expected | Measured (mean) | Deviation | Cause |
|---|---|---|---|---|---|
| 1 | Accelerometer: flat / tilted 30° / free fall | \|a\| = 1.000 / 1.000 / 0.000 g | 1.000 / 1.000 / 0.000 g | 0 | – |
| 2 | Barometer: 1 / 2 / 3 floors of 3 m | Δh = 3.00 / 6.00 / 9.00 m | 3.12 / 6.25 / 9.37 m | +4.1 % | The emulator rounds pressure to steps of 0.125 hPa; the app is exact for the pressure it receives |
| 3 | GPS: walk of 0.001° north, jitter, jump | 111.19 m; jitter and jump add 0 m | 111.2 m; +0 m; +0 m | none beyond the display's 0.1 m | – |
| 4 | Event rate at `GAME` / `NORMAL` | 50 / 5 Hz | 50.0 / 15.0 Hz (16.8 Hz including one disturbed reading) | 0 / +10 Hz | The system's screen-rotation detection keeps the accelerometer at 15 Hz |
| 5 | CPU time, `GAME` → `NORMAL` | – | 3.20 % → 2.67 % | −16.5 % CPU for −70 % events | Most of the app's work doesn't depend on the rate |

## 1. Accelerometer

**Setup:** each run starts with the phone upright at rest, (x, y, z) = (0, 9.80665, 0) m/s², for 1 s. Then the scenario's values are injected (`emu sensor set acceleration x:y:z`) and read from the app's log 2.5 s later. 5 runs per scenario (`accelerometer.csv`).

| Scenario | Injected (x, y, z) in m/s² |
|---|---|
| Flat on a table, screen up | (0, 0, 9.80665) |
| Flat, right edge raised by 30° (rotation about the y axis) | (g · sin 30°, 0, g · cos 30°) = (4.903325, 0, 8.492808) |
| Free fall | (0, 0, 0) |

**Expected:**

- Flat: z = g = 9.80665 m/s², \|a\| = 1.000 g. The [`SensorEvent` reference](https://developer.android.com/reference/android/hardware/SensorEvent#values): "When the device lies flat on a table, the acceleration value is +9.81, which correspond to the acceleration of the device (0 m/s^2) minus the force of gravity (-9.81 m/s^2)."
- Tilted 30°: gravity splits into x = g · sin 30° and z = g · cos 30°. The magnitude stays g · √(sin² 30° + cos² 30°) = g, so \|a\| = 1.000 g.
- Free fall: \|a\| = 0. Same reference: "when the device is in free-fall and therefore dangerously accelerating towards to ground at 9.81 m/s^2, its accelerometer reads a magnitude of 0 m/s^2."

**Result** (all 5 runs identical, so the mean equals every run):

| Scenario | Received (x, y, z) in m/s² | App: \|a\| | Expected \|a\| | Deviation |
|---|---|---|---|---|
| Flat | (0.000, 0.000, 9.807) | 9.807 m/s² = 1.000 g | 9.807 m/s² = 1.000 g | 0 |
| Tilted 30° | (4.903, 0.000, 8.493) | 9.807 m/s² = 1.000 g | 9.807 m/s² = 1.000 g | 0 |
| Free fall | (0.000, 0.000, 0.000) | 0.000 m/s² = 0.000 g | 0 | 0 |

**Explanation:** no deviation at the log's resolution (3 decimals). The emulator passes the injected values on unchanged, so the result confirms the axes, the magnitude formula and the conversion to g with 9.80665 m/s² (`SensorManager.STANDARD_GRAVITY`). The noise and offset of a real sensor are outside what this setup can show.

## 2. Barometer

### 2.1 Pressure resolution of the emulator

Before the floor experiment, 21 pressures from 1013.25 down to 1013.05 hPa in steps of 0.01 hPa were injected (`emu sensor set pressure`) and compared with the pressure the app received (`barometer-steps.csv`):

| Injected (hPa) | Received (hPa) |
|---|---|
| 1013.25 … 1013.19 | 1013.250 |
| 1013.18 … 1013.07 | 1013.125 |
| 1013.06 … 1013.05 | 1013.000 |

The emulator rounds to the nearest multiple of 0.125 hPa: the switching points lie halfway between the multiples (1013.1875 and 1013.0625). Near sea level, one step of 0.125 hPa corresponds to 1.04 m of altitude (formula in 2.2).

### 2.2 One to three floors

**Setup:** pressure 1013.25 hPa (standard atmosphere at sea level), tap **Reset**, then inject the pressure of a position 3, 6 or 9 m higher (1, 2 or 3 floors of 3 m) and read Δh from the app's log 2.5 s later. The injected pressures come from inverting Android's altitude formula: p = p₀ · (1 − h / 44330)^5.255. 5 runs per height (`barometer.csv`).

**Expected:** Δh = 3.00 / 6.00 / 9.00 m, by construction. The app computes Δh as the difference between two `SensorManager.getAltitude()` results (spec section 1), as the [reference](https://developer.android.com/reference/android/hardware/SensorManager#getAltitude(float,%20float)) describes: "To calculate altitude differences, you must calculate the difference between the altitudes at both points." The formula, from Android's source ([`SensorManager.java:1561-1563`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/hardware/SensorManager.java#1561), tag `android-17.0.0_r1`): h = 44330 · (1 − (p / p₀)^(1/5.255)).

**Result** (all 5 runs identical):

| Floors | Injected (hPa) | Received (hPa) | App: Δh | Formula for the received pressure | Expected Δh | Deviation |
|---|---|---|---|---|---|---|
| 1 | 1012.890 | 1012.875 | 3.12 m | 3.12 m | 3.00 m | +0.12 m |
| 2 | 1012.530 | 1012.500 | 6.25 m | 6.25 m | 6.00 m | +0.25 m |
| 3 | 1012.169 | 1012.125 | 9.37 m | 9.37 m | 9.00 m | +0.37 m |

**Explanation:** the app is exact for the pressure it receives (column "Formula" equals column "App"). The whole deviation comes from the emulator's rounding (2.1): one floor of 3 m corresponds to 0.360 hPa, the emulator delivers 3 steps of 0.125 hPa = 0.375 hPa, so every floor appears 4.1 % too high. With this emulator, Δh can't resolve less than about 1 m.

## 3. GPS

**Setup:** each run (`gps.csv`, 5 runs) has three phases:

1. **Walk:** position 48.2120° N, 9.0270° E, tap **Reset**, wait 6 s; then 10 steps of 0.0001° north, one every 4 s (`emu geo fix`): 0.001° in total, at 2.8 m/s.
2. **Jitter:** 20 fixes, 1 s apart, alternating 0.00002° and 0.00003° north and south of the end point (2.2 m and 3.3 m away).
3. **Jump:** one fix in Mountain View (37.4220° N, 122.0840° W), the emulator's default position, more than 9,000 km away.

The emulator reports every fix with an accuracy of 5.0 m (see the excerpt below).

**Expected:**

- Walk: 111.19 m. The app measures with [`Location.distanceBetween`](https://developer.android.com/reference/android/location/Location#distanceBetween(double,%20double,%20double,%20double,%20float[])): "Distance and bearing are defined using the WGS84 ellipsoid." Along a meridian, 0.001° is M · 0.001 · π / 180, with the WGS84 meridian radius of curvature M = a (1 − e²) / (1 − e² sin² φ)^1.5 = 6,370,973 m at φ = 48.2125° (a = 6,378,137 m, e² = 0.00669438). This gives 111.19 m.
- Jitter: no step counted. Every jitter fix is at most 3.3 m from the last counted fix (the end point), which is less than its accuracy of 5.0 m, so the app ignores it (spec section 1).
- Jump: rejected, distance unchanged. The step implies far more than 10 m/s (spec amendment A3).

**Result:**

| Phase | Expected | Measured (5 runs) | Deviation |
|---|---|---|---|
| Walk | 111.19 m | 111.2 m in all runs | none beyond the display's 0.1 m |
| Jitter | 0 steps counted, distance unchanged | 0 steps counted, 111.2 m, in all runs | 0 |
| Jump | rejected, distance unchanged | 111.2 m in all runs | 0 |

**Explanation:** the distance, the accuracy filter and the plausibility limit work as specified. Three details:

- **Missing log line in run 5:** the app logged the rejection in runs 1–4 but not in run 5 (`jump_rejected_logged`). The unchanged distance shows that the jump was rejected in run 5 too; the line is missing because fix decisions are logged at most once per second (spec section 6, `log/ChainLog.kt:26-36`), so a decision less than a second after the previous line isn't logged.
- **Single steps:** in an additional walk with the same procedure (`gps-steps.log`), the individual steps alternate between 10.9 m and 11.3 m instead of 11.12 m each:

  ```
  16:30:07.265 D/SensorLab.Process( 4604): GPS: counted: step 10.9 m > accuracy 5.0 m, distance=10.9 m
  16:30:11.267 D/SensorLab.Process( 4604): GPS: counted: step 11.3 m > accuracy 5.0 m, distance=22.2 m
  …
  16:30:40.281 D/SensorLab.Process( 4604): GPS: counted: step 10.9 m > accuracy 5.0 m, distance=99.9 m
  16:30:44.285 D/SensorLab.Process( 4604): GPS: counted: step 11.3 m > accuracy 5.0 m, distance=111.2 m
  ```

  So the emulator doesn't deliver every position exactly; the mechanism was not determined. After every second step the distance is on target (22.2, 44.5, 66.7, 89.0, 111.2 m; expected 22.24 m per pair), so the total is unaffected.
- **First series:** a first series of 5 runs (`gps-first-run.csv`) measured 100.3 m in runs 2 and 4. The script had started the walk only 2 s after Reset; when the start fix arrived late, the first step of 11 m came only 1 s after it, and the app correctly rejected it under the 10 m/s limit (`rejected: step 10.9 m in 1.0 s = 10.9 m/s > 10 m/s`). The script was fixed (6 s wait) and the series repeated; the results above are from the repeated series (`ai-errors.md`, entry 15).

## 4. Event rate: `SENSOR_DELAY_GAME` vs. `SENSOR_DELAY_NORMAL`

**Setup:** accelerometer at `GAME` (**Low rate** switch off), then at `NORMAL` (switch on). After 6 s, 5 readings about 7.5 s apart (`rate.csv`), each with two values:

- the rate the app shows ("Measured" on the Values tab): events in the last 2 s, from their timestamps;
- Android's own record of the last 50 accelerometer events (`dumpsys sensorservice`, section "Recent Sensor events"): 49 intervals divided by their time span.

Screen on, constant inputs, no other app in the foreground.

**Requested:** `GAME` = 20,000 µs = 50 Hz, `NORMAL` = 200,000 µs = 5 Hz. [Sensors Overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview): "The default data delay … uses a delay of 200,000 microseconds. You can specify other data delays, such as SENSOR_DELAY_GAME (20,000 microsecond delay) …".

**Result:**

| Setting | Requested | App shows (runs 1–5) | Android's record (runs 1–5) | Measured | Deviation |
|---|---|---|---|---|---|
| `GAME` | 50 Hz | 50.0 Hz in all runs | 50 events in 0.980 s = 50.0 Hz in all runs | 50.0 Hz | 0 |
| `NORMAL` | 5 Hz | 15.0 / 15.0 / 15.0 / 24.2 / 15.0 Hz | 15.0 / 15.0 / 15.0 / 23.9 / 15.0 Hz (50 events in 3.267 s; run 4: 2.053 s) | mean of 5: 16.8 Hz; without run 4: 15.0 Hz | +10.0 Hz (3 ×) |

The app's value and Android's record agree in every reading; in run 4 they differ slightly because they cover different time windows (last 2 s vs. last 50 events).

**Explanation:** at `GAME`, Android delivers exactly the requested rate; at `NORMAL`, three times the requested rate. The Sensors Overview warns about this: "The delay that you specify is only a suggested delay. The Android system and other applications can alter this delay." Android runs a sensor at the fastest period any client requests, and every client receives the events at that rate. The dump (`sensorservice.txt`, taken with Sensor Lab at `GAME`) shows the accelerometer's clients and the period selected:

```
0x00000000) active-count = 3; sampling_period(ms) = {66.7, 200.0, 20.0}, selected = 20.00 ms; …
```

Its registration history (newest first in the dump, here oldest first) names the clients:

```
14:54:13 + 0x00000000 … samplingPeriod=  200000us … (Goldfish 3-axis Accelerometer, com.android.server.power.FaceDownDetector)
14:54:16 + 0x00000000 … samplingPeriod=   66667us … (Goldfish 3-axis Accelerometer, com.android.server.wm.WindowOrientationListener$AccelSensorJudge)
15:32:47 + 0x00000000 … samplingPeriod=  200000us … (Goldfish 3-axis Accelerometer, de.penguin.sensorlab.data.SensorRepository$samples$1)
```

- `WindowOrientationListener` is the system's screen-rotation detection, active on the emulator (`window-orientation.txt`: `mEnabled=true`, sensor "Goldfish 3-axis Accelerometer", `mRate=2`). Android's source creates it with `SENSOR_DELAY_UI` ([`WindowOrientationListener.java:102`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/services/core/java/com/android/server/wm/WindowOrientationListener.java#102), via [`DisplayRotation.java:289`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/services/core/java/com/android/server/wm/DisplayRotation.java#289)), and `SensorManager.getDelay()` maps that to 66,667 µs ([`SensorManager.java:1991-1992`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/hardware/SensorManager.java#1991)).[^ui]
- `FaceDownDetector` (system, 200 ms) and Sensor Lab at `NORMAL` (200 ms) are slower.

So at `NORMAL` the fastest request is 66.7 ms = 15.0 Hz, exactly what both the app and Android's record show (49 / 3.267 s = 15.0 Hz).

**Run 4 (24.2 Hz):** for less than a second at 15:33:15, a Google Play services component registered the accelerometer at 20 ms:

```
15:33:15 + 0x00000000 pid= 1296 … samplingPeriod=   20000us … (Goldfish 3-axis Accelerometer, com.google.android.location.collectionlib.SensorScanner$SensorEventListenerAdapter)
15:33:16 - 0x00000000 pid= 1296 … (Goldfish 3-axis Accelerometer, com.google.android.location.collectionlib.SensorScanner$SensorEventListenerAdapter)
    Final Duration: 00:00:00 | …
```

The `NORMAL` series ran from the switch at 15:32:47 to the last write of `rate.csv` at 15:33:25 (file time 16:33:25 CET). With readings about 7.5 s apart, run 4 was read at about 15:33:17, so its window includes this burst at 50 Hz. The component repeats this about every 5 minutes (e.g. at 15:27:57 and 15:38:34). In the same second, another Google component (`droidguard`) briefly registered the fused rotation-vector and linear-acceleration sensors, also at 20 ms.

[^ui]: The Sensors Overview gives `SENSOR_DELAY_UI` as "60,000 microsecond delay" (16.7 Hz), while Android's code and the dump use 66,667 µs (15.0 Hz). The measured 15.0 Hz matches the code.

## 5. Consequence for energy use

**Setup:** CPU time of the app process (user + system, from `/proc/<pid>/stat`) over 60 s, alternating `GAME` and `NORMAL`, 3 runs each, starting 5 s after each switch (`cpu.csv`). Screen on, Values tab, constant inputs; 100 % means one CPU core busy all the time. According to the registration history in `sensorservice.txt`, no client requested more than the rotation listener's 15 Hz during a `NORMAL` window (the nearest 20 ms burst ended 2 s before window 3), so the app received 15 Hz there.

**Result:**

| Setting | Events/s received | CPU time, runs 1–3 | Mean |
|---|---|---|---|
| `GAME` | 50 | 3.27 / 3.12 / 3.21 % | 3.20 % |
| `NORMAL` | 15 | 2.68 / 2.73 / 2.61 % | 2.67 % |

70 % fewer events cost 16.5 % less CPU time (−0.53 percentage points). A straight line through the two points gives about 0.015 percentage points per Hz (0.15 ms of CPU time per event on the emulator) and 2.45 % that doesn't depend on the accelerometer rate: the UI snapshot every 100 ms, the chart data, the barometer, GPS and logging. Even the requested 5 Hz would save only about a fifth (2.52 %, −21 %, extrapolated).

**What follows for energy use:**

1. **The requested rate limits the savings, it doesn't guarantee them.** `NORMAL` delivered 15 Hz instead of 5 Hz because a system service asks for more. The reverse also holds: at `GAME`, Sensor Lab is the fastest client (`selected = 20.00 ms`), so it makes the sensor run at 50 Hz for every client.
2. **For this app, the sampling rate is a small lever.** Most of its CPU time is fixed work. The larger lever is not running at all when nothing is visible: the app stops its sensors 5 s after leaving the screen (review item 1), as the [`SensorManager` reference](https://developer.android.com/reference/android/hardware/SensorManager) advises: "Always make sure to disable sensors you don't need, especially when your activity is paused. Failing to do so can drain the battery in just a few hours." For measuring with the screen off, the lever is batching ([`registerListener` with `maxReportLatencyUs`](https://developer.android.com/reference/android/hardware/SensorManager#registerListener(android.hardware.SensorEventListener,%20android.hardware.Sensor,%20int,%20int))): "Setting maxReportLatencyUs to a positive value allows to reduce the number of interrupts the AP (Application Processor) receives, hence reducing power consumption". The emulator's accelerometer can't batch (`sensorservice.txt`, line 8: `no batching`), so this couldn't be measured.
3. **`GAME` stays the right setting for the shock warning** (review item 5): `NORMAL` would save little and could miss short peaks.

**Limits:** the emulator runs on a PC processor, so only the ratio between the settings carries over, not the absolute values. The sensor hardware's own power use was not measured.
