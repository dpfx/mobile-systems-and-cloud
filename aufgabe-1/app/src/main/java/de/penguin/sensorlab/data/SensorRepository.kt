package de.penguin.sensorlab.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import de.penguin.sensorlab.log.ChainLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** One sensor event, copied out of the [SensorEvent] that the framework reuses. */
class SensorSample(
    val values: FloatArray,
    /** Event time in ns, same time base as `SystemClock.elapsedRealtimeNanos()`. */
    val timestampNanos: Long,
)

/**
 * Data source for one sensor type (one instance per sensor). Only this class talks to
 * [SensorManager].
 */
class SensorRepository(
    private val sensorManager: SensorManager,
    sensorType: Int,
    /** Name used in log lines, e.g. "Accelerometer". */
    private val name: String,
) {
    private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    /** False if the device has no such sensor. */
    val isAvailable: Boolean = sensor != null

    /**
     * Sensor events while the flow is collected. The listener is registered when collection
     * starts and unregistered in [awaitClose] when it stops.
     *
     * @param samplingPeriod one of the `SensorManager.SENSOR_DELAY_*` constants.
     */
    fun samples(samplingPeriod: Int): Flow<SensorSample> {
        val sensor = sensor ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                // Only copy and forward; all processing happens downstream.
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(SensorSample(event.values.copyOf(), event.timestamp))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            val registered = sensorManager.registerListener(listener, sensor, samplingPeriod)
            if (registered) {
                ChainLog.state(ChainLog.SENSOR, name, "listener registered (delay $samplingPeriod)")
            } else {
                ChainLog.warning(ChainLog.SENSOR, name, "listener registration failed")
            }
            awaitClose {
                sensorManager.unregisterListener(listener)
                ChainLog.state(ChainLog.SENSOR, name, "listener unregistered")
            }
        }
    }
}
