package de.penguin.sensorlab.ui

import android.hardware.SensorManager
import androidx.compose.runtime.Immutable
import de.penguin.sensorlab.processing.ChartSegment

/** Requested sensor rate (spec section 1). */
enum class SensorRate(val sensorDelay: Int, val constantName: String, val periodMillis: Int) {
    Game(SensorManager.SENSOR_DELAY_GAME, "SENSOR_DELAY_GAME", 20),
    Normal(SensorManager.SENSOR_DELAY_NORMAL, "SENSOR_DELAY_NORMAL", 200),
    ;

    val hz: Double get() = 1000.0 / periodMillis
}

/** Everything the screen shows. Produced by [SensorLabViewModel] at most 10 times per second. */
@Immutable
data class SensorLabUiState(
    val accelerometer: AccelerometerUiState,
    val barometer: BarometerUiState,
    val gps: GpsUiState,
    /** Latest shock warning for the banner, or null. */
    val shockWarning: ShockWarning?,
    val charts: ChartsUiState,
)

@Immutable
data class AccelerometerUiState(
    val available: Boolean,
    /** Latest values in m/s², null before the first event. */
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val magnitude: Float?,
    val requestedRate: SensorRate,
    val measuredRateHz: Double,
    val shockAlertsOn: Boolean,
    val notificationsAllowed: Boolean,
)

@Immutable
data class BarometerUiState(
    val available: Boolean,
    val pressureHpa: Float?,
    /** Altitude change since start or reset, in m. */
    val altitudeChangeMeters: Float?,
    val requestedRate: SensorRate,
    val measuredRateHz: Double,
)

enum class GpsStatus {
    /** The device has no GPS. */
    NotAvailable,

    /** No location permission yet: show "Enable GPS". */
    NeedsPermission,

    /** The user denied the permission: show an explanation and "Grant". */
    PermissionDenied,

    /** Permission granted, but GPS is switched off. */
    GpsOff,

    /** Permission granted and GPS on. */
    On,
}

@Immutable
data class GpsUiState(
    val status: GpsStatus,
    /** True if only approximate location is granted: no distance. */
    val approximateOnly: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    /** Distance since start or reset in m; null with approximate location only. */
    val distanceMeters: Double?,
    val measuredRateHz: Double,
    /** What happened to the latest fix, e.g. "counted: step 12.3 m > accuracy 4.0 m". */
    val lastFixDecision: String?,
)

@Immutable
data class ShockWarning(
    /** Wall-clock time of detection (ms since 1970). */
    val timeMillis: Long,
    /** Highest |a| of this shock in m/s². */
    val peakMagnitude: Float,
)

/** Chart data; null means the chart is hidden because the sensor is missing. */
@Immutable
data class ChartsUiState(
    val magnitude: List<ChartSegment>?,
    val altitudeChange: List<ChartSegment>?,
    val distance: List<ChartSegment>?,
)
