# Part D – Verification by measurement

**Method:** no physical device was available, so every experiment injects known values into the emulator (`emu sensor set`, `emu geo fix`); the script [`tools/measure.sh`][script] then reads what the app received, calculated and displayed, 5 runs per scenario ([raw data][data]). This verifies the app's processing chain (sensor event → processing → display) against a known input, not the accuracy of real sensors, so a deviation comes from the app or from the emulator. Tested version: commit `0e2987c` (after fixes F1–F5); times in excerpts are emulator time (UTC).

## 1. Accelerometer

**Setup:** from the phone upright at rest, inject the scenario's acceleration and read the app's \|a\| 2.5 s later.

**Expected:** flat on a table, the [`SensorEvent` reference][se] gives "+9.81" (gravity, so \|a\| = 1 g); tilted, gravity splits into x = g · sin 30° and z = g · cos 30°, and \|a\| = g · √(sin² 30° + cos² 30°) stays 1 g; in free fall the accelerometer "reads a magnitude of 0 m/s^2" (same reference).

| Scenario | Injected (x, y, z) in m/s² | Expected \|a\| | App shows (all 5 runs) | Deviation |
|---|---|---|---|---|
| Flat on a table | (0, 0, 9.80665) | 9.807 m/s² = 1.000 g | 9.807 m/s² = 1.000 g | 0 |
| Right edge raised by 30° | (4.903325, 0, 8.492808) | 9.807 m/s² = 1.000 g | 9.807 m/s² = 1.000 g | 0 |
| Free fall | (0, 0, 0) | 0 | 0.000 m/s² = 0.000 g | 0 |

**Explanation:** the emulator passes the values on unchanged, so this confirms the axes, the magnitude formula and the conversion to g (9.80665 m/s²), not the noise or offset of a real sensor.

## 2. Barometer

**Setup:** tap **Reset** at 1013.25 hPa, then inject the pressure of a position 3, 6 or 9 m higher (1–3 floors), computed by inverting Android's altitude formula h = 44330 · (1 − (p / p₀)^(1/5.255)) ([source][alt]).

**Expected:** Δh = 3.00 / 6.00 / 9.00 m, by construction; the app computes Δh with that formula (spec section 1).

| Floors | Injected (hPa) | Received (hPa) | App: Δh (all 5 runs) | Formula for the received pressure | Expected | Deviation |
|---|---|---|---|---|---|---|
| 1 | 1012.890 | 1012.875 | 3.12 m | 3.12 m | 3.00 m | +0.12 m |
| 2 | 1012.530 | 1012.500 | 6.25 m | 6.25 m | 6.00 m | +0.25 m |
| 3 | 1012.169 | 1012.125 | 9.37 m | 9.37 m | 9.00 m | +0.37 m |

**Explanation:** the app is exact for the pressure it receives. A pre-test with 21 pressures in steps of 0.01 hPa showed that the emulator rounds pressure to the nearest multiple of 0.125 hPa ([`barometer-steps.csv`][steps]). One floor of 3 m is 0.360 hPa; the emulator delivers 3 steps = 0.375 hPa, so every floor appears 4.1 % too high.

## 3. GPS

**Setup:** tap **Reset** at 48.2120° N, 9.0270° E; walk 10 steps of 0.0001° north, one every 4 s (2.8 m/s); then 20 s of jitter 2.2–3.3 m around the end point; then a jump to Mountain View (more than 9,000 km). The emulator reports an accuracy of 5.0 m for every fix.

**Expected:** the walk is 111.19 m: [`Location.distanceBetween`][dist] uses "the WGS84 ellipsoid", and 0.001° along the meridian at φ = 48.21° is M · 0.001 · π / 180 with M = a (1 − e²) / (1 − e² sin² φ)^1.5 = 6,370,973 m. Jitter fixes stay within their accuracy, so no step counts (spec section 1); the jump implies far more than 10 m/s, so it is rejected (amendment A3).

| Phase | Expected | Measured (all 5 runs) | Deviation |
|---|---|---|---|
| Walk | 111.19 m | 111.2 m | none beyond the display's 0.1 m |
| Jitter | 0 steps counted | 0 steps counted, 111.2 m | 0 |
| Jump | rejected, distance unchanged | 111.2 m | 0 |

**Explanation:** distance, accuracy filter and plausibility limit work as specified. Single steps alternate between 10.9 and 11.3 m in the emulator (one extra walk, [`gps-steps.log`][gpslog]; mechanism not determined), but after every second step the distance is on target. A first series with a script error (first step too fast, correctly rejected by the 10 m/s limit) was repeated ([`ai-errors.md`][errors], entry 15).

## 4. Event rate: `SENSOR_DELAY_GAME` vs. `SENSOR_DELAY_NORMAL`

**Setup:** accelerometer at `GAME`, then at `NORMAL` (**Low rate** switch); 5 readings each of the rate the app shows (events in the last 2 s) and of Android's own record of the last 50 events (`dumpsys sensorservice`).

**Requested:** `GAME` = 20,000 µs = 50 Hz, `NORMAL` = 200,000 µs = 5 Hz ([Sensors Overview][overview]).

| Setting | Requested | App shows (runs 1–5) | Android's record (runs 1–5) | Measured | Deviation |
|---|---|---|---|---|---|
| `GAME` | 50 Hz | 50.0 Hz in all runs | 50.0 Hz in all runs | 50.0 Hz | 0 |
| `NORMAL` | 5 Hz | 15.0 / 15.0 / 15.0 / 24.2 / 15.0 Hz | 15.0 / 15.0 / 15.0 / 23.9 / 15.0 Hz | mean of 5: 16.8 Hz; without run 4: 15.0 Hz | +10.0 Hz (3 ×) |

**Explanation:** "The delay that you specify is only a suggested delay. The Android system and other applications can alter this delay" ([Sensors Overview][overview]). Android runs the sensor at the fastest period any client requests ([`sensorservice.txt`][dump], taken at `GAME`):

```
0x00000000) active-count = 3; sampling_period(ms) = {66.7, 200.0, 20.0}, selected = 20.00 ms; …
14:54:16 + … samplingPeriod=   66667us … (…, com.android.server.wm.WindowOrientationListener$AccelSensorJudge)
15:33:15 + … samplingPeriod=   20000us … (…, com.google.android.location.collectionlib.SensorScanner$SensorEventListenerAdapter)
```

The system's screen-rotation detection requests 66,667 µs (`SENSOR_DELAY_UI`, [Android source][wol]), so at `NORMAL` the sensor runs at 15.0 Hz, exactly what both values show. Run 4: for less than a second at 15:33:15, a Google Play services component requested 20 ms; it is the only disturbance during the whole `NORMAL` series, and run 4 is the only reading that deviates.

## 5. Consequence for energy use

**Setup:** CPU time of the app process (from `/proc/<pid>/stat`) over 60 s, alternating `GAME` and `NORMAL`, 3 runs each; 100 % means one CPU core busy all the time.

| Setting | Events/s received | CPU time, runs 1–3 | Mean |
|---|---|---|---|
| `GAME` | 50 | 3.27 / 3.12 / 3.21 % | 3.20 % |
| `NORMAL` | 15 | 2.68 / 2.73 / 2.61 % | 2.67 % |

70 % fewer events cost only 16.5 % less CPU time: 83 % of it remained.

**What follows for energy use:**

1. **The requested rate limits the savings, it doesn't guarantee them:** `NORMAL` delivered 15 Hz instead of 5 Hz because a system service asks for more; and at `GAME`, Sensor Lab makes the sensor run at 50 Hz for every client.
2. **For this app, the rate is a small lever.** The larger one is not running at all when nothing is visible, as the app does (Part C, item 1), and as the [`SensorManager` reference][sm] advises: "Always make sure to disable sensors you don't need, especially when your activity is paused." With the screen off, batching would be the lever ([`maxReportLatencyUs`][batch]: it reduces "the number of interrupts the AP (Application Processor) receives, hence reducing power consumption"); the emulator's sensor can't batch ([`sensorservice.txt`][dump], line 8), so this wasn't measured.
3. **`GAME` stays right for the shock warning:** `NORMAL` would save little and could miss short peaks.

Limit: the emulator runs on a PC processor, so only the ratio carries over, not the absolute values. Full version with all excerpts: [`docs/measurements.md`][full-measurements].

[script]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/tools/measure.sh
[data]: https://github.com/dpfx/mobile-systems-and-cloud/tree/main/aufgabe-1/docs/measurements
[steps]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/measurements/barometer-steps.csv
[gpslog]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/measurements/gps-steps.log
[dump]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/measurements/sensorservice.txt
[errors]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/ai-errors.md
[full-measurements]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/measurements.md
[se]: https://developer.android.com/reference/android/hardware/SensorEvent#values
[alt]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/hardware/SensorManager.java#1561
[dist]: https://developer.android.com/reference/android/location/Location#distanceBetween(double,%20double,%20double,%20double,%20float[])
[overview]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
[wol]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/services/core/java/com/android/server/wm/WindowOrientationListener.java#102
[sm]: https://developer.android.com/reference/android/hardware/SensorManager
[batch]: https://developer.android.com/reference/android/hardware/SensorManager#registerListener(android.hardware.SensorEventListener,%20android.hardware.Sensor,%20int,%20int)
