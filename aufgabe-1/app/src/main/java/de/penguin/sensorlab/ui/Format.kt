package de.penguin.sensorlab.ui

import android.hardware.SensorManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Number and time formats shared by the screen, the notification and the UI log,
// so all three show exactly the same text.

/** Fixed number of decimals with a decimal point; "–" if there is no value. */
fun formatNumber(value: Number?, decimals: Int): String =
    if (value == null) "–" else "%.${decimals}f".format(Locale.ROOT, value.toDouble())

/** Like [formatNumber], followed by [unit] (which includes its leading space, e.g. " m"). */
fun formatValue(value: Number?, decimals: Int, unit: String): String =
    if (value == null) "–" else formatNumber(value, decimals) + unit

/** Acceleration in g (1 g = 9.80665 m/s², `SensorManager.STANDARD_GRAVITY`). */
fun toG(metersPerSecondSquared: Float): Float = metersPerSecondSquared / SensorManager.STANDARD_GRAVITY

/** Local wall-clock time, e.g. "14:03:12". */
fun formatClockTime(timeMillis: Long): String =
    clockFormatter.format(Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()))

/** Details of a shock warning, e.g. "at 14:03:12, peak |a| = 27.30 m/s² (2.78 g)". */
fun formatShockDetails(warning: ShockWarning): String =
    "at ${formatClockTime(warning.timeMillis)}, peak |a| = " +
        "${formatNumber(warning.peakMagnitude, 2)} m/s² (${formatNumber(toG(warning.peakMagnitude), 2)} g)"

private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
