package de.penguin.sensorlab.processing

import android.location.Location
import de.penguin.sensorlab.data.LocationFix
import java.util.Locale

/** What the [DistanceTracker] did with a fix. */
sealed interface FixDecision {
    val description: String

    /** Accuracy worse than 20 m (or unknown): not used. */
    data class Rejected(val accuracyMeters: Float?) : FixDecision {
        override val description = if (accuracyMeters == null) {
            "rejected: no accuracy"
        } else {
            "rejected: accuracy %.1f m > %.0f m".format(Locale.ROOT, accuracyMeters, MAX_ACCURACY_METERS)
        }
    }

    /** First usable fix (after start, reset or pause): new reference point, no distance yet. */
    data object StartPoint : FixDecision {
        override val description = "start point"
    }

    /** Step not larger than the fix's accuracy: probably jitter, not counted. */
    data class Ignored(val stepMeters: Float, val accuracyMeters: Float) : FixDecision {
        override val description =
            "ignored: step %.1f m <= accuracy %.1f m".format(Locale.ROOT, stepMeters, accuracyMeters)
    }

    /** Step larger than the fix's accuracy: added to the distance. */
    data class Counted(val stepMeters: Float, val accuracyMeters: Float) : FixDecision {
        override val description =
            "counted: step %.1f m > accuracy %.1f m".format(Locale.ROOT, stepMeters, accuracyMeters)
    }
}

/** Max. accuracy of a fix that is used for the distance (spec section 1). */
const val MAX_ACCURACY_METERS = 20f

/**
 * Distance walked (spec section 1): uses only fixes with accuracy ≤ 20 m and grows only when the
 * step from the last counted fix exceeds the new fix's accuracy.
 */
class DistanceTracker {
    var distanceMeters = 0.0
        private set

    private var lastCounted: LocationFix? = null

    fun add(fix: LocationFix): FixDecision {
        val accuracy = fix.accuracyMeters
        if (accuracy == null || accuracy > MAX_ACCURACY_METERS) return FixDecision.Rejected(accuracy)

        val last = lastCounted
        if (last == null) {
            lastCounted = fix
            return FixDecision.StartPoint
        }
        val step = distanceBetween(last, fix)
        if (step <= accuracy) return FixDecision.Ignored(step, accuracy)

        distanceMeters += step
        lastCounted = fix
        return FixDecision.Counted(step, accuracy)
    }

    /** After a pause: the distance stays, the next usable fix becomes the new reference point. */
    fun restartFromNextFix() {
        lastCounted = null
    }

    fun reset() {
        distanceMeters = 0.0
        lastCounted = null
    }

    /** Distance in m on the WGS84 ellipsoid (platform formula). */
    private fun distanceBetween(from: LocationFix, to: LocationFix): Float {
        val result = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
        return result[0]
    }
}
