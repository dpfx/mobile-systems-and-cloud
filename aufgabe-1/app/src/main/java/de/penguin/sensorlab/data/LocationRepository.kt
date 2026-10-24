package de.penguin.sensorlab.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import de.penguin.sensorlab.log.ChainLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** One GPS fix, copied out of [Location]. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    /** Horizontal accuracy in m, or null if the fix has none. */
    val accuracyMeters: Float?,
    /** Fix time in ns, same time base as `SystemClock.elapsedRealtimeNanos()`. */
    val elapsedRealtimeNanos: Long,
)

/** What the location flow reports. */
sealed interface LocationEvent {
    data class Fix(val fix: LocationFix) : LocationEvent

    /** The GPS provider was switched on or off (also sent once when the flow starts). */
    data class GpsSwitched(val enabled: Boolean) : LocationEvent
}

/** Location permission granted by the user. */
enum class LocationPermission { None, Approximate, Precise }

/** Data source for GPS fixes. Only this class talks to [LocationManager]. */
class LocationRepository(private val context: Context) {
    private val locationManager: LocationManager? =
        context.getSystemService(LocationManager::class.java)

    /** False if the device has no GPS provider. */
    val hasGps: Boolean =
        locationManager != null &&
            LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER)

    fun permission(): LocationPermission = when {
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationPermission.Precise
        isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationPermission.Approximate
        else -> LocationPermission.None
    }

    fun isGpsEnabled(): Boolean =
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

    /**
     * GPS fixes (1 per second) and GPS on/off changes while the flow is collected. Updates are
     * requested when collection starts and removed in [awaitClose] when it stops.
     * Only call this with a location permission.
     */
    // The permission is checked by the caller (ViewModel) and again below.
    @SuppressLint("MissingPermission")
    fun updates(): Flow<LocationEvent> {
        val manager = locationManager ?: return emptyFlow()
        return callbackFlow {
            val listener = object : LocationListenerCompat {
                // Only copy and forward; all processing happens downstream.
                override fun onLocationChanged(location: Location) {
                    trySend(LocationEvent.Fix(location.toFix()))
                }

                override fun onProviderEnabled(provider: String) {
                    trySend(LocationEvent.GpsSwitched(enabled = true))
                }

                override fun onProviderDisabled(provider: String) {
                    trySend(LocationEvent.GpsSwitched(enabled = false))
                }
            }
            val request = LocationRequestCompat.Builder(FIX_INTERVAL_MS)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()

            trySend(LocationEvent.GpsSwitched(isGpsEnabled()))
            if (permission() == LocationPermission.None) {
                ChainLog.warning(ChainLog.SENSOR, NAME, "no location permission, no updates")
            } else {
                try {
                    LocationManagerCompat.requestLocationUpdates(
                        manager,
                        LocationManager.GPS_PROVIDER,
                        request,
                        listener,
                        Looper.getMainLooper(),
                    )
                    ChainLog.state(ChainLog.SENSOR, NAME, "updates requested (every $FIX_INTERVAL_MS ms)")
                } catch (e: SecurityException) {
                    ChainLog.warning(ChainLog.SENSOR, NAME, "updates refused: ${e.message}")
                }
            }
            awaitClose {
                LocationManagerCompat.removeUpdates(manager, listener)
                ChainLog.state(ChainLog.SENSOR, NAME, "updates removed")
            }
        }
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun Location.toFix() = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
    )

    companion object {
        /** Requested rate: 1 fix per second (spec section 1). */
        const val FIX_INTERVAL_MS = 1_000L
        private const val NAME = "GPS"
    }
}
