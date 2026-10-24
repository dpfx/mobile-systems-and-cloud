package de.penguin.sensorlab.log

import android.os.SystemClock
import android.util.Log
import de.penguin.sensorlab.BuildConfig

/**
 * Logcat output of the processing chain (spec section 6), one tag per stage:
 * sensor event -> processing -> display. Only active in debug builds.
 *
 * Normal lines are sampled: at most one line per source (e.g. "Accelerometer") and tag per
 * second. Warnings and state changes are always logged.
 */
object ChainLog {
    const val SENSOR = "SensorLab.Sensor"
    const val PROCESS = "SensorLab.Process"
    const val UI = "SensorLab.UI"

    private const val MIN_LINE_INTERVAL_MS = 1_000L

    private val enabled = BuildConfig.DEBUG

    // Time of the last line per "tag/source", for sampling.
    private val lastLineMillis = HashMap<String, Long>()

    /** Logs at most one line per [tag] and [source] per second. [message] is only built if logged. */
    @Synchronized
    fun sampled(tag: String, source: String, message: () -> String) {
        if (!enabled) return
        val key = "$tag/$source"
        val now = SystemClock.elapsedRealtime()
        val last = lastLineMillis[key]
        if (last != null && now - last < MIN_LINE_INTERVAL_MS) return
        lastLineMillis[key] = now
        Log.d(tag, "$source: ${message()}")
    }

    /** Logs a state change (always, not sampled). */
    fun state(tag: String, source: String, message: String) {
        if (enabled) Log.i(tag, "$source: $message")
    }

    /** Logs a warning (always, not sampled). */
    fun warning(tag: String, source: String, message: String) {
        if (enabled) Log.w(tag, "$source: $message")
    }
}
