package de.penguin.sensorlab.ui

import android.hardware.SensorManager
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.penguin.sensorlab.data.LocationEvent
import de.penguin.sensorlab.data.LocationFix
import de.penguin.sensorlab.data.LocationPermission
import de.penguin.sensorlab.data.LocationRepository
import de.penguin.sensorlab.data.SensorRepository
import de.penguin.sensorlab.data.SensorSample
import de.penguin.sensorlab.data.ShockNotifier
import de.penguin.sensorlab.log.ChainLog
import de.penguin.sensorlab.processing.DistanceTracker
import de.penguin.sensorlab.processing.FixDecision
import de.penguin.sensorlab.processing.History
import de.penguin.sensorlab.processing.RateMeter
import java.util.Locale
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Processes the sensor and GPS data and produces the screen state.
 *
 * Lifecycle: the sensors and GPS run only while [uiState] is collected. The UI collects it only
 * while visible; `WhileSubscribed(5000)` stops the sources 5 s after the screen is left, so a
 * rotation doesn't restart them. The values below live in the ViewModel, so Δh, distance and the
 * chart history continue after a pause.
 *
 * Threading: sensor and GPS callbacks arrive on the main thread and are processed in
 * [viewModelScope] (main thread), so the plain `var`s below are only touched by one thread.
 */
class SensorLabViewModel(
    private val accelerometer: SensorRepository,
    private val barometer: SensorRepository,
    private val location: LocationRepository,
    private val shockNotifier: ShockNotifier,
) : ViewModel() {

    private class Acceleration(val x: Float, val y: Float, val z: Float, val magnitude: Float)

    // --- Settings and permissions (changed by UI actions) ---
    private val accelerometerRate = MutableStateFlow(SensorRate.Game)
    private val locationPermission = MutableStateFlow(location.permission())
    private var locationPermissionDenied = false
    private var shockAlertsOn = false // off by default (spec section 3)
    private var notificationsAllowed = shockNotifier.canNotify()

    // --- Accelerometer ---
    private var acceleration: Acceleration? = null
    private val accelerometerRateMeter = RateMeter()
    private val magnitudeHistory = History(keepPeak = true)
    private var shockInProgress = false
    private var shockPeak = 0f
    private var lastShockWarningNanos: Long? = null
    private var shockWarning: ShockWarning? = null

    // --- Barometer ---
    private var pressureHpa: Float? = null
    private var startAltitudeMeters: Float? = null
    private var altitudeChangeMeters: Float? = null
    private val barometerRateMeter = RateMeter()
    private val altitudeHistory = History(keepPeak = false)

    // --- GPS ---
    private var gpsEnabled = location.isGpsEnabled()
    private var latestFix: LocationFix? = null
    private var lastFixDecision: FixDecision? = null
    private val gpsRateMeter = RateMeter()
    private val distanceTracker = DistanceTracker()
    private val distanceHistory = History(keepPeak = false)

    /**
     * Screen state. While collected, the three sources run in parallel and every event is
     * processed; a snapshot for the UI is published every 100 ms, independent of the sensor rates.
     */
    val uiState: StateFlow<SensorLabUiState> = channelFlow {
        launch { runAccelerometer() }
        launch { runBarometer() }
        launch { runGps() }
        while (true) {
            send(buildUiState())
            delay(UI_UPDATE_INTERVAL_MS)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = buildUiState(),
    )

    // ---------------------------------------------------------------- UI actions

    fun setLowAccelerometerRate(low: Boolean) {
        accelerometerRate.value = if (low) SensorRate.Normal else SensorRate.Game
        ChainLog.state(ChainLog.PROCESS, ACC, "requested rate ${accelerometerRate.value.constantName}")
    }

    fun setShockAlerts(on: Boolean) {
        shockAlertsOn = on
        ChainLog.state(ChainLog.PROCESS, SHOCK, "Shock alerts switched ${if (on) "on" else "off"}")
    }

    fun dismissShockWarning() {
        shockWarning = null
    }

    /** Restarts Δh and distance (spec section 1). */
    fun reset() {
        startAltitudeMeters = pressureHpa?.let(::altitudeOf)
        altitudeChangeMeters = if (startAltitudeMeters != null) 0f else null
        altitudeHistory.clear()
        distanceTracker.reset()
        distanceHistory.clear()
        ChainLog.state(ChainLog.PROCESS, "Reset", "Δh and distance restarted")
    }

    /** Called whenever the screen becomes visible: permissions may have changed in the settings. */
    fun recheckPermissions() {
        val permission = location.permission()
        if (permission != LocationPermission.None) locationPermissionDenied = false
        if (permission != locationPermission.value) {
            ChainLog.state(ChainLog.PROCESS, GPS, "location permission: $permission")
            locationPermission.value = permission // starts or stops the GPS updates
        }
        val gpsOn = location.isGpsEnabled()
        if (gpsOn != gpsEnabled) onGpsSwitched(gpsOn)

        val allowed = shockNotifier.canNotify()
        if (allowed != notificationsAllowed) {
            notificationsAllowed = allowed
            ChainLog.state(ChainLog.PROCESS, SHOCK, "notifications ${if (allowed) "allowed" else "blocked"}")
        }
    }

    /** Result of the location permission request. */
    fun onLocationPermissionResult() {
        locationPermissionDenied = location.permission() == LocationPermission.None
        if (locationPermissionDenied) ChainLog.state(ChainLog.PROCESS, GPS, "location permission denied")
        recheckPermissions()
    }

    // ---------------------------------------------------------------- Sources

    private suspend fun runAccelerometer() {
        if (!accelerometer.isAvailable) {
            ChainLog.state(ChainLog.SENSOR, ACC, "not available on this device")
            return
        }
        magnitudeHistory.startNewSegment() // gap in the chart after a pause
        // A new rate re-registers the listener.
        accelerometerRate.collectLatest { rate ->
            accelerometerRateMeter.clear()
            accelerometer.samples(rate.sensorDelay).collect { onAccelerometerSample(it) }
        }
    }

    private suspend fun runBarometer() {
        if (!barometer.isAvailable) {
            ChainLog.state(ChainLog.SENSOR, BARO, "not available on this device")
            return
        }
        altitudeHistory.startNewSegment()
        barometerRateMeter.clear()
        barometer.samples(BAROMETER_RATE.sensorDelay).collect { onBarometerSample(it) }
    }

    private suspend fun runGps() {
        if (!location.hasGps) {
            ChainLog.state(ChainLog.SENSOR, GPS, "not available on this device")
            return
        }
        distanceHistory.startNewSegment()
        // Updates run only with a location permission; a new permission restarts them.
        locationPermission.collectLatest { permission ->
            if (permission == LocationPermission.None) return@collectLatest
            gpsRateMeter.clear()
            distanceTracker.restartFromNextFix() // distance resumes from the next fix
            location.updates().collect { event ->
                when (event) {
                    is LocationEvent.GpsSwitched -> onGpsSwitched(event.enabled)
                    is LocationEvent.Fix ->
                        onLocationFix(event.fix, precise = permission == LocationPermission.Precise)
                }
            }
        }
    }

    // ---------------------------------------------------------------- Processing

    private fun onAccelerometerSample(sample: SensorSample) {
        val x = sample.values[0]
        val y = sample.values[1]
        val z = sample.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        accelerometerRateMeter.add(sample.timestampNanos)
        acceleration = Acceleration(x, y, z, magnitude)
        magnitudeHistory.add(sample.timestampNanos, magnitude)

        ChainLog.sampled(ChainLog.SENSOR, ACC) {
            "x=%.3f y=%.3f z=%.3f m/s², t=%d ns, measured %.1f Hz".format(
                Locale.ROOT, x, y, z, sample.timestampNanos,
                accelerometerRateMeter.rateHz(sample.timestampNanos),
            )
        }
        ChainLog.sampled(ChainLog.PROCESS, ACC) {
            "|a|=%.3f m/s² = %.3f g".format(Locale.ROOT, magnitude, toG(magnitude))
        }
        checkForShock(magnitude, sample.timestampNanos)
    }

    /** Warning rule (spec section 3): |a| ≥ 25 m/s², at most one warning per 30 s. */
    private fun checkForShock(magnitude: Float, timestampNanos: Long) {
        if (magnitude < SHOCK_THRESHOLD) {
            if (shockInProgress) {
                shockInProgress = false
                ChainLog.state(
                    ChainLog.PROCESS, SHOCK,
                    "shock ended, peak |a|=%.2f m/s²".format(Locale.ROOT, shockPeak),
                )
            }
            return
        }
        if (shockInProgress) {
            // The same shock goes on: the banner shows its peak.
            if (magnitude > shockPeak) {
                shockPeak = magnitude
                shockWarning = shockWarning?.copy(peakMagnitude = magnitude)
            }
            return
        }
        val lastWarning = lastShockWarningNanos
        if (lastWarning != null && timestampNanos - lastWarning < SHOCK_MIN_INTERVAL_NANOS) {
            ChainLog.sampled(ChainLog.PROCESS, SHOCK) {
                "|a|=%.2f m/s² >= %.0f m/s², but last warning %.1f s ago: no new warning".format(
                    Locale.ROOT, magnitude, SHOCK_THRESHOLD,
                    (timestampNanos - lastWarning) / NANOS_PER_SECOND,
                )
            }
            return
        }

        shockInProgress = true
        shockPeak = magnitude
        lastShockWarningNanos = timestampNanos
        val warning = ShockWarning(timeMillis = System.currentTimeMillis(), peakMagnitude = magnitude)
        shockWarning = warning

        val notificationResult = when {
            !shockAlertsOn -> "no notification (Shock alerts off)"
            shockNotifier.notifyShock(notificationText(warning)) -> "notification posted"
            else -> "no notification (notifications blocked)"
        }
        ChainLog.warning(
            ChainLog.PROCESS, SHOCK,
            "Strong shock detected: |a|=%.2f m/s² (%.2f g) >= %.0f m/s², banner shown, %s".format(
                Locale.ROOT, magnitude, toG(magnitude), SHOCK_THRESHOLD, notificationResult,
            ),
        )
    }

    private fun notificationText(warning: ShockWarning): String =
        "|a| = ${formatNumber(warning.peakMagnitude, 2)} m/s² " +
            "(${formatNumber(toG(warning.peakMagnitude), 2)} g) at ${formatClockTime(warning.timeMillis)}"

    private fun onBarometerSample(sample: SensorSample) {
        val pressure = sample.values[0]
        barometerRateMeter.add(sample.timestampNanos)
        pressureHpa = pressure

        val altitude = altitudeOf(pressure)
        val start = startAltitudeMeters ?: altitude.also {
            startAltitudeMeters = it
            ChainLog.state(
                ChainLog.PROCESS, BARO,
                "start point: p=%.2f hPa, h=%.2f m".format(Locale.ROOT, pressure, altitude),
            )
        }
        val change = altitude - start
        altitudeChangeMeters = change
        altitudeHistory.add(sample.timestampNanos, change)

        ChainLog.sampled(ChainLog.SENSOR, BARO) {
            "p=%.3f hPa, t=%d ns, measured %.1f Hz".format(
                Locale.ROOT, pressure, sample.timestampNanos,
                barometerRateMeter.rateHz(sample.timestampNanos),
            )
        }
        ChainLog.sampled(ChainLog.PROCESS, BARO) {
            "h=%.2f m (standard atmosphere), Δh=%.2f m".format(Locale.ROOT, altitude, change)
        }
    }

    private fun onGpsSwitched(enabled: Boolean) {
        if (enabled == gpsEnabled) return
        gpsEnabled = enabled
        ChainLog.state(ChainLog.PROCESS, GPS, "GPS switched ${if (enabled) "on" else "off"}")
    }

    private fun onLocationFix(fix: LocationFix, precise: Boolean) {
        gpsRateMeter.add(fix.elapsedRealtimeNanos)
        latestFix = fix
        // Positions in the log are rounded to 3 decimals (spec section 6).
        ChainLog.sampled(ChainLog.SENSOR, GPS) {
            "fix lat=%.3f lon=%.3f accuracy=%s m, t=%d ns, measured %.1f Hz".format(
                Locale.ROOT, fix.latitude, fix.longitude, formatNumber(fix.accuracyMeters, 1),
                fix.elapsedRealtimeNanos, gpsRateMeter.rateHz(fix.elapsedRealtimeNanos),
            )
        }
        if (!precise) {
            lastFixDecision = null
            ChainLog.sampled(ChainLog.PROCESS, GPS) { "approximate location only: no distance" }
            return
        }
        val decision = distanceTracker.add(fix)
        lastFixDecision = decision
        distanceHistory.add(fix.elapsedRealtimeNanos, distanceTracker.distanceMeters.toFloat())
        ChainLog.sampled(ChainLog.PROCESS, GPS) {
            "${decision.description}, distance=%.1f m".format(Locale.ROOT, distanceTracker.distanceMeters)
        }
    }

    // ---------------------------------------------------------------- UI state

    private fun buildUiState(): SensorLabUiState {
        val now = SystemClock.elapsedRealtimeNanos()
        val permission = locationPermission.value
        val acceleration = acceleration
        val fix = latestFix
        return SensorLabUiState(
            accelerometer = AccelerometerUiState(
                available = accelerometer.isAvailable,
                x = acceleration?.x,
                y = acceleration?.y,
                z = acceleration?.z,
                magnitude = acceleration?.magnitude,
                requestedRate = accelerometerRate.value,
                measuredRateHz = accelerometerRateMeter.rateHz(now),
                shockAlertsOn = shockAlertsOn,
                notificationsAllowed = notificationsAllowed,
            ),
            barometer = BarometerUiState(
                available = barometer.isAvailable,
                pressureHpa = pressureHpa,
                altitudeChangeMeters = altitudeChangeMeters,
                requestedRate = BAROMETER_RATE,
                measuredRateHz = barometerRateMeter.rateHz(now),
            ),
            gps = GpsUiState(
                status = gpsStatus(permission),
                approximateOnly = permission == LocationPermission.Approximate,
                latitude = fix?.latitude,
                longitude = fix?.longitude,
                accuracyMeters = fix?.accuracyMeters,
                distanceMeters =
                    if (permission == LocationPermission.Precise) distanceTracker.distanceMeters else null,
                measuredRateHz = gpsRateMeter.rateHz(now),
                lastFixDecision = lastFixDecision?.description,
            ),
            shockWarning = shockWarning,
            charts = ChartsUiState(
                magnitude = if (accelerometer.isAvailable) magnitudeHistory.segments(now) else null,
                altitudeChange = if (barometer.isAvailable) altitudeHistory.segments(now) else null,
                distance = if (location.hasGps) distanceHistory.segments(now) else null,
            ),
        )
    }

    private fun gpsStatus(permission: LocationPermission): GpsStatus = when {
        !location.hasGps -> GpsStatus.NotAvailable
        permission == LocationPermission.None ->
            if (locationPermissionDenied) GpsStatus.PermissionDenied else GpsStatus.NeedsPermission
        !gpsEnabled -> GpsStatus.GpsOff
        else -> GpsStatus.On
    }

    /** Altitude in m from pressure in hPa, standard atmosphere (platform formula). */
    private fun altitudeOf(pressureHpa: Float): Float =
        SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureHpa)

    companion object {
        /** Warning threshold for |a| in m/s² (≈ 2.5 g). */
        const val SHOCK_THRESHOLD = 25f
        private const val SHOCK_MIN_INTERVAL_NANOS = 30_000_000_000L // 30 s
        private const val UI_UPDATE_INTERVAL_MS = 100L // ≤ 10 UI updates per second
        private const val STOP_TIMEOUT_MS = 5_000L // sources stop 5 s after leaving the screen
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private val BAROMETER_RATE = SensorRate.Normal

        // Source names in log lines
        private const val ACC = "Accelerometer"
        private const val BARO = "Barometer"
        private const val GPS = "GPS"
        private const val SHOCK = "Shock"
    }
}
