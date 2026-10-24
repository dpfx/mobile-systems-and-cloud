package de.penguin.sensorlab.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.penguin.sensorlab.AppViewModelProvider
import de.penguin.sensorlab.log.ChainLog

private val tabTitles = listOf("Values", "Chart")

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** The app's only screen: header, shock banner and the tabs Values and Chart. */
@Composable
fun SensorLabScreen(
    viewModel: SensorLabViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Collected only while the screen is visible (at least STARTED).
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Each time the screen becomes visible: permissions may have changed in the system settings
    // while the app was in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.recheckPermissions()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onLocationPermissionResult() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.recheckPermissions() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Header(onReset = viewModel::reset)
            state.shockWarning?.let { warning ->
                ShockBanner(warning = warning, onDismiss = viewModel::dismissShockWarning)
            }
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> ValuesTab(
                    state = state,
                    onLowRateChange = viewModel::setLowAccelerometerRate,
                    onShockAlertsChange = { on ->
                        viewModel.setShockAlerts(on)
                        // Asked in context, only when the user turns the alerts on. If the user
                        // denies, the switch shows a hint and only the banner is used.
                        if (on && !state.accelerometer.notificationsAllowed &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onEnableGps = { locationPermissionLauncher.launch(locationPermissions) },
                    onGrantLocation = {
                        // After a permanent denial the system shows no dialog any more,
                        // so the app settings are opened instead.
                        val canAskAgain = activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            )
                        if (canAskAgain) {
                            locationPermissionLauncher.launch(locationPermissions)
                        } else {
                            openAppSettings(context)
                        }
                    },
                )
                else -> ChartTab(state = state)
            }
        }
    }
}

@Composable
private fun Header(onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Sensor Lab", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onReset) { Text("Reset") }
    }
}

/** In-app banner "Strong shock detected" with time and peak |a| (spec section 3). */
@Composable
private fun ShockBanner(warning: ShockWarning, onDismiss: () -> Unit) {
    val details = formatShockDetails(warning)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Strong shock detected", style = MaterialTheme.typography.titleMedium)
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
    SideEffect {
        ChainLog.sampled(ChainLog.UI, "Shock banner") { "shown: $details" }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}
