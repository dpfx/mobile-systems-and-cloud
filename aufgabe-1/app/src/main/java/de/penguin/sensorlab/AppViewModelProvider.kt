package de.penguin.sensorlab

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.penguin.sensorlab.data.LocationRepository
import de.penguin.sensorlab.data.SensorRepository
import de.penguin.sensorlab.data.ShockNotifier
import de.penguin.sensorlab.ui.SensorLabViewModel

/**
 * Manual dependency injection: creates the data sources from the application context and passes
 * them into the ViewModel's constructor. The ViewModel itself never sees a Context.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = checkNotNull(this[APPLICATION_KEY])
            val sensorManager = application.getSystemService(SensorManager::class.java)
            SensorLabViewModel(
                accelerometer = SensorRepository(sensorManager, Sensor.TYPE_ACCELEROMETER, "Accelerometer"),
                barometer = SensorRepository(sensorManager, Sensor.TYPE_PRESSURE, "Barometer"),
                location = LocationRepository(application),
                shockNotifier = ShockNotifier(application),
            )
        }
    }
}
