# Assessment – Task 1 (Part E)

**Scenario:** Sensor Lab is to recognise what the person is doing, for example sitting, walking or climbing stairs. The input would be the accelerometer, which the app already reads at 50 Hz (`SENSOR_DELAY_GAME`), and the barometer for stairs: one floor of 3 m is 0.36 hPa (`measurements.md`, section 2.2). The public [UCI HAR dataset](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones) uses a similar input from a smartphone, with a gyroscope in addition: "we captured 3-axial linear acceleration and 3-axial angular velocity at a constant rate of 50Hz", with the activities "WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING", evaluated in "fixed-width sliding windows of 2.56 sec".

## 1. Edge or cloud?

**Recommendation:** run the recognition on the device. The cloud is only used to train the model and to distribute model updates; users' raw sensor data doesn't leave the phone.

| Criterion | On the device | In the cloud | Evidence |
|---|---|---|---|
| Latency | Result as soon as a window of data is complete and classified; no network involved | Same window, plus upload, processing and response. If the radio was idle, waking it adds time: "Returning to full power from the low state takes around 1.5 seconds, and moving from standby to full power can take over 2 seconds" | [Android: radio state machine][radio] (figures for "a typical 3G network radio") |
| Energy | Sensor data stays in memory; the work is computation only. For comparison, the whole app at 50 Hz uses 3.20 % of one CPU core (emulator) | The raw values alone are 600 B/s (3 axes × 4-byte `float` × 50 Hz), 2.2 MB per hour, before timestamps and protocol overhead. A continuous upload keeps the radio active: "When fully active, the wireless radio consumes significant power, but when inactive or in standby, the radio consumes very little power" | `measurements.md`, section 5; [`SensorEvent.values`][values]; [radio state machine][radio] |
| Offline | Works wherever the phone is | Recognises nothing without a connection. The app is a hiking companion, so it is used outdoors, where coverage can't be assumed | Spec, first line |
| Privacy | Raw data stays on the phone; at most the result leaves it, and only if the user wants that | The raw accelerometer stream (and barometer) is sent to a server. Accelerometer data alone can reveal health, body features and identity (section 2). The GDPR requires data to be "limited to what is necessary" for the purpose, and the recognition needs only the result, not the raw stream on a server | [GDPR Art. 5(1)(c)][gdpr]; [Kröger et al.][kroeger] |

The sensors run in both variants, so their own energy use is the same. The difference is computing on the phone vs. transmitting continuously. Latency is the least decisive criterion: activities last seconds to minutes, and both variants need a full window of data first.

**What the cloud is used for:** training needs labelled data from many people. The UCI HAR study, for example, recorded 30 volunteers whose sessions "have been video-recorded to label the data manually" ([UCI HAR][uci]). That fits the cloud, using data from people who agreed to it, and so does distributing new model versions to the app. Neither needs the users' sensor data.

**Own model needed:** Android already offers activity recognition to apps, but its [Activity Recognition Transition API][transition] "supports the following activities: IN_VEHICLE ON_BICYCLE RUNNING STILL WALKING", with no stairs and no sitting as a separate class. Recognising the PDF's examples therefore needs an app-specific model.

## 2. Personal data

**Yardstick:** the GDPR's definition, Art. 4(1): "'personal data' means any information relating to an identified or identifiable natural person …; an identifiable natural person is one who can be identified, directly or indirectly, in particular by reference to an identifier such as a name, an identification number, location data, an online identifier or to one or more factors specific to the physical, physiological, genetic, mental, economic, cultural or social identity of that natural person". Art. 4(15) defines "data concerning health" as personal data "which reveal information about his or her health status" ([GDPR][gdpr]).

| Data in Sensor Lab | Personal? | What it reveals about the person | Evidence |
|---|---|---|---|
| GPS position (latitude/longitude, shown with 5 decimals ≈ 1 m) | Yes, directly: "location data" is named in Art. 4(1) | Where the person is; over time home, workplace, routes, routines | GDPR Art. 4(1) |
| Distance since Reset | Yes, derived from the positions | How far and how fast the person moves | Spec section 1 |
| Positions in the debug log (3 decimals ≈ 100 m; debug builds only) | Yes, still location data | Where the person was and when, to about 100 m | `review.md`, item 8 |
| Accelerometer x, y, z and \|a\| | Yes, it allows conclusions | "a device holder's location, activities, health condition, body features, gender, age, personality traits, and emotional state"; it can even "uniquely identify a person based on biometric movement patterns" | [Kröger et al.][kroeger], abstract |
| Shock warnings (time, peak \|a\|) | Yes | Falls or accidents, so they can become data concerning health (Art. 4(15)) | Spec section 3; GDPR Art. 4(15) |
| Barometer pressure and Δh | Yes, mainly in combination | Floor changes (1 floor ≈ 0.36 hPa, `measurements.md` 2.2), so the floor within a building; with GPS, which floor of which building | `measurements.md`, section 2.2 |
| Activity labels of the planned feature | Yes | Daily routine, how much the person sits or moves, so possibly health | [Kröger et al.][kroeger] (activities, health condition) |

Not personal on their own: the measured event rates; they depend on the phone and on other apps (`measurements.md`, section 4).

**Conclusion:** every sensor in the app produces personal data. GPS does so directly; the accelerometer and barometer do so through what can be inferred from them, most strongly in combination. Android's permission model doesn't reflect this: Sensor Lab needs a permission only for location (and notifications); the accelerometer and barometer can be read without asking (`AndroidManifest.xml`). Kröger et al. make the same point: "third-party apps can access accelerometer data without requiring security permission". Android at least limits the rate of motion sensors "to protect potentially sensitive information about users" ([Sensors Overview › Sensor rate-limiting][ratelimit]). Sensor Lab keeps all values in memory, stores nothing, has no background location and logs rounded positions in debug builds only (spec sections 5 and 6, `review.md` item 8). The planned recognition fits this design only if it runs on the device (section 1).

## Sources

- Regulation (EU) 2016/679 (GDPR), Art. 4(1), 4(15), 5(1)(c): https://eur-lex.europa.eu/eli/reg/2016/679/oj
- J. L. Kröger, P. Raschke, T. R. Bhuiyan: "Privacy Implications of Accelerometer Data: A Review of Possible Inferences". ICCSP '19, pp. 81–87, 2019: https://doi.org/10.1145/3309074.3309076
- D. Anguita et al.: Human Activity Recognition Using Smartphones (dataset), UCI Machine Learning Repository: https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones
- Android Developers, Detect when users start or end an activity: https://developer.android.com/develop/sensors-and-location/location/transitions
- Android Developers, Optimize network access › The radio state machine: https://developer.android.com/develop/connectivity/network-ops/network-access-optimization
- Android Developers, `SensorEvent.values`: https://developer.android.com/reference/android/hardware/SensorEvent#values
- Android Developers, Sensors Overview › Sensor rate-limiting: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

[gdpr]: https://eur-lex.europa.eu/eli/reg/2016/679/oj
[kroeger]: https://doi.org/10.1145/3309074.3309076
[uci]: https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones
[transition]: https://developer.android.com/develop/sensors-and-location/location/transitions
[radio]: https://developer.android.com/develop/connectivity/network-ops/network-access-optimization
[values]: https://developer.android.com/reference/android/hardware/SensorEvent#values
[ratelimit]: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
