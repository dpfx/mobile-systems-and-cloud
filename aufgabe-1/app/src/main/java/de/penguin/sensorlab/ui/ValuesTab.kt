package de.penguin.sensorlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.penguin.sensorlab.data.LocationRepository
import de.penguin.sensorlab.log.ChainLog

/** Tab "Values": one card per sensor with values, requested/measured rate and status. */
@Composable
fun ValuesTab(
    state: SensorLabUiState,
    onLowRateChange: (Boolean) -> Unit,
    onShockAlertsChange: (Boolean) -> Unit,
    onEnableGps: () -> Unit,
    onGrantLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccelerometerCard(state.accelerometer, onLowRateChange, onShockAlertsChange)
        BarometerCard(state.barometer)
        GpsCard(state.gps, onEnableGps, onGrantLocation)
    }
}

@Composable
private fun AccelerometerCard(
    state: AccelerometerUiState,
    onLowRateChange: (Boolean) -> Unit,
    onShockAlertsChange: (Boolean) -> Unit,
) {
    SensorCard(title = "Accelerometer") {
        if (!state.available) {
            NotAvailable()
        } else {
            val x = formatValue(state.x, 2, " m/s²")
            val y = formatValue(state.y, 2, " m/s²")
            val z = formatValue(state.z, 2, " m/s²")
            val magnitude = formatValue(state.magnitude, 2, " m/s²")
            val magnitudeG = formatValue(state.magnitude?.let(::toG), 3, " g")
            val measured = formatValue(state.measuredRateHz, 1, " Hz")
            val lowRate = state.requestedRate == SensorRate.Normal

            ValueRow("x", x)
            ValueRow("y", y)
            ValueRow("z", z)
            ValueRow("|a|", "$magnitude = $magnitudeG")
            RequestedRateRow(state.requestedRate)
            ValueRow("Measured", measured)
            ValueRow("Status", sourceStatus(state.magnitude != null, state.measuredRateHz))

            SwitchRow(
                label = "Low rate (${SensorRate.Normal.constantName})",
                checked = lowRate,
                onCheckedChange = onLowRateChange,
            )
            if (lowRate) {
                Note(
                    "Reduced reliability: at 5 Hz, a short shock can fall between two " +
                        "samples and be missed.",
                )
            }
            SwitchRow(
                label = "Shock alerts (notification at |a| ≥ " +
                    "${formatNumber(SensorLabViewModel.SHOCK_THRESHOLD, 0)} m/s²)",
                checked = state.shockAlertsOn,
                onCheckedChange = onShockAlertsChange,
            )
            if (state.shockAlertsOn && !state.notificationsAllowed) {
                Note(
                    "Notifications are blocked, so shocks are only shown as a banner in the app. " +
                        "You can allow notifications for Sensor Lab in the system settings.",
                )
            }
            SideEffect {
                ChainLog.sampled(ChainLog.UI, "Accelerometer") {
                    "shown x=$x y=$y z=$z |a|=$magnitude = $magnitudeG, measured $measured"
                }
            }
        }
    }
}

@Composable
private fun BarometerCard(state: BarometerUiState) {
    SensorCard(title = "Barometer") {
        if (!state.available) {
            NotAvailable()
        } else {
            val pressure = formatValue(state.pressureHpa, 2, " hPa")
            val altitudeChange = formatValue(state.altitudeChangeMeters, 2, " m")
            val measured = formatValue(state.measuredRateHz, 1, " Hz")

            ValueRow("Pressure", pressure)
            ValueRow("Δh since start", altitudeChange)
            RequestedRateRow(state.requestedRate)
            ValueRow("Measured", measured)
            ValueRow("Status", sourceStatus(state.pressureHpa != null, state.measuredRateHz))

            SideEffect {
                ChainLog.sampled(ChainLog.UI, "Barometer") {
                    "shown p=$pressure Δh=$altitudeChange, measured $measured"
                }
            }
        }
    }
}

@Composable
private fun GpsCard(state: GpsUiState, onEnableGps: () -> Unit, onGrantLocation: () -> Unit) {
    SensorCard(title = "GPS") {
        when (state.status) {
            GpsStatus.NotAvailable -> NotAvailable()

            GpsStatus.NeedsPermission -> {
                Note(
                    "GPS needs the location permission to show your position and the distance " +
                        "walked. Location is only used while this screen is visible.",
                )
                Button(onClick = onEnableGps) { Text("Enable GPS") }
            }

            GpsStatus.PermissionDenied -> {
                Note(
                    "Location permission was denied, so position and distance can't be shown. " +
                        "The other sensors keep working. Tap Grant to allow location.",
                )
                Button(onClick = onGrantLocation) { Text("Grant") }
            }

            GpsStatus.GpsOff, GpsStatus.On -> GpsValues(state)
        }
    }
}

@Composable
private fun GpsValues(state: GpsUiState) {
    val latitude = formatValue(state.latitude, 5, "°")
    val longitude = formatValue(state.longitude, 5, "°")
    val accuracy = formatValue(state.accuracyMeters, 1, " m")
    val distance = formatValue(state.distanceMeters, 1, " m")
    val measured = formatValue(state.measuredRateHz, 1, " Hz")

    if (state.status == GpsStatus.GpsOff) {
        Note("GPS is off. Turn on location in the system settings to get fixes.")
    }
    ValueRow("Latitude", latitude)
    ValueRow("Longitude", longitude)
    ValueRow("Accuracy", accuracy)
    ValueRow("Distance since start", distance)
    if (state.approximateOnly) {
        Note(
            "Approximate location only: the distance is not measured. Allow precise location " +
                "in the system settings to measure it.",
        )
    }
    ValueRow("Requested", "1 Hz (1 fix every ${LocationRepository.FIX_INTERVAL_MS} ms)")
    ValueRow("Measured", measured)
    ValueRow("Last fix", state.lastFixDecision ?: "–")
    ValueRow(
        "Status",
        when {
            state.status == GpsStatus.GpsOff -> "GPS off"
            state.measuredRateHz > 0 -> "Running"
            state.latitude != null -> "No fix in the last 2 s"
            else -> "Waiting for fix"
        },
    )

    SideEffect {
        // Positions in the log are rounded to 3 decimals (spec section 6).
        ChainLog.sampled(ChainLog.UI, "GPS") {
            "shown lat=${formatNumber(state.latitude, 3)} lon=${formatNumber(state.longitude, 3)} " +
                "accuracy=$accuracy distance=$distance, measured $measured"
        }
    }
}

// ---------------------------------------------------------------- Building blocks

@Composable
private fun SensorCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RequestedRateRow(rate: SensorRate) {
    ValueRow(
        "Requested",
        "${formatNumber(rate.hz, 0)} Hz (${rate.constantName}, ${rate.periodMillis} ms)",
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotAvailable() {
    Text("Not available on this device", style = MaterialTheme.typography.bodyMedium)
}

private fun sourceStatus(hasValue: Boolean, measuredRateHz: Double): String = when {
    measuredRateHz > 0 -> "Running"
    hasValue -> "No events in the last 2 s"
    else -> "Waiting for data"
}
