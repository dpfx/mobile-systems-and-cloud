# Part E – Sensor data and AI

**Scenario:** the app is to recognise sitting, walking or climbing stairs from the accelerometer (read at 50 Hz, as now) and the barometer (one floor ≈ 0.36 hPa, Part D 2.2). The public [UCI HAR dataset][uci] uses the same kind of data at the same rate (smartphone accelerometer, plus gyroscope, at 50 Hz) for these activities, in windows of 2.56 s.

## 1. Edge or cloud?

**Recommendation:** run the recognition on the device. The cloud is only used to train the model and to distribute model updates; users' raw sensor data doesn't leave the phone.

| Criterion | On the device | In the cloud | Evidence |
|---|---|---|---|
| Latency | Result as soon as a window of data is complete and classified; no network involved | Same window, plus upload, processing and response | Window length: [UCI HAR][uci] |
| Energy | Sensor data stays in memory; the work is computation only. For comparison, the whole app at 50 Hz uses 3.20 % of one CPU core (emulator) | The raw values alone are 600 B/s (3 axes × 4-byte `float` × 50 Hz), 2.2 MB per hour, sent continuously: "When fully active, the wireless radio consumes significant power, but when inactive or in standby, the radio consumes very little power" | Part D 5; [`SensorEvent.values`][values]; [radio state machine][radio] |
| Offline | Works wherever the phone is | Recognises nothing without a connection; the app is a hiking companion, used outdoors, where coverage can't be assumed | Spec, first line |
| Privacy | Raw data stays on the phone; at most the result leaves it, if the user wants that | The raw accelerometer and barometer stream goes to a server. Accelerometer data alone can reveal health, body features and identity (section 2), while the recognition needs only the result | [Kröger et al.][kroeger] |

The sensors run in both variants, so the difference is computing on the phone vs. transmitting continuously. Latency decides least: activities last seconds to minutes, and both variants need a full window first. Training needs labelled data from many people who agreed to it, which fits the cloud; it doesn't need the users' sensor data. Android's [Activity Recognition Transition API][transition] knows only "IN_VEHICLE ON_BICYCLE RUNNING STILL WALKING", so stairs and sitting need an app-specific model.

## 2. Personal data

**Yardstick:** GDPR Art. 4(1): "'personal data' means any information relating to an identified or identifiable natural person …; an identifiable natural person is one who can be identified, directly or indirectly, in particular by reference to an identifier such as a name, an identification number, location data …" ([GDPR][gdpr]).

| Data in Sensor Lab | Personal? | What it reveals about the person | Evidence |
|---|---|---|---|
| GPS position (shown with 5 decimals ≈ 1 m) | Yes, directly: "location data" | Where the person is; over time home, workplace, routes, routines | GDPR Art. 4(1) |
| Distance since Reset | Yes, derived from the positions | How far and how fast the person moves | Spec section 1 |
| Positions in the debug log (3 decimals ≈ 100 m; debug builds only) | Yes, still location data | Where the person was and when, to about 100 m | Part C, item 8 |
| Accelerometer x, y, z and \|a\| | Yes, it allows conclusions | "a device holder's location, activities, health condition, body features, gender, age, personality traits, and emotional state"; it can "uniquely identify a person based on biometric movement patterns" | [Kröger et al.][kroeger], abstract |
| Shock warnings (time, peak \|a\|) | Yes | When the phone was hit or dropped (with GPS: where) | Spec section 3 |
| Barometer pressure and Δh | Yes, mainly in combination | Floor changes, so the floor within a building; with GPS, which floor of which building | Part D 2.2 |
| Activity labels of the planned feature | Yes | Daily routine, how much the person sits or moves; possibly the health condition | [Kröger et al.][kroeger] |

Not personal on their own: the measured event rates; they depend on the phone and on other apps (Part D 4).

**Conclusion:** every sensor in the app produces personal data, GPS directly, the accelerometer and barometer through what can be inferred from them, most strongly in combination. Android's permissions don't reflect this: Sensor Lab needs one only for location (and notifications), while the accelerometer and barometer can be read without asking; Android only limits the rate of motion sensors "to protect potentially sensitive information about users" ([Sensor rate-limiting][ratelimit]). Sensor Lab keeps all values in memory and stores nothing (spec section 5); the planned recognition fits this only if it runs on the device.

**Sources:** [GDPR, Art. 4(1)][gdpr] · J. L. Kröger, P. Raschke, T. R. Bhuiyan: "Privacy Implications of Accelerometer Data: A Review of Possible Inferences", ICCSP '19, 2019, [doi:10.1145/3309074.3309076][kroeger] · [UCI HAR dataset][uci] · Android Developers: [Activity Recognition Transition API][transition], [radio state machine][radio], [`SensorEvent.values`][values], [Sensor rate-limiting][ratelimit]. Full version: [`docs/assessment.md`][full-assessment].

[gdpr]: https://eur-lex.europa.eu/eli/reg/2016/679/oj
[kroeger]: https://doi.org/10.1145/3309074.3309076
[uci]: https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones
[transition]: https://developer.android.com/develop/sensors-and-location/location/transitions
[radio]: https://developer.android.com/develop/connectivity/network-ops/network-access-optimization
[values]: https://developer.android.com/reference/android/hardware/SensorEvent#values
[ratelimit]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
[full-assessment]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/assessment.md
