package de.penguin.sensorlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import de.penguin.sensorlab.log.ChainLog
import de.penguin.sensorlab.processing.ChartSegment
import de.penguin.sensorlab.processing.History
import kotlin.math.roundToInt

/**
 * Tab "Chart": one line chart each for |a|, Δh and distance over the last 60 s (spec section 2).
 * The chart of a missing sensor is hidden.
 */
@Composable
fun ChartTab(state: SensorLabUiState) {
    val charts = state.charts
    val distanceMeasured = state.gps.status == GpsStatus.On && !state.gps.approximateOnly
    ProvideVicoTheme(rememberM3VicoTheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Last 60 s, x axis in seconds before now. A gap means tracking was paused.",
                style = MaterialTheme.typography.bodySmall,
            )
            charts.magnitude?.let { HistoryChart("|a| (m/s²)", it, "No data in the last 60 s") }
            charts.altitudeChange?.let { HistoryChart("Δh (m)", it, "No data in the last 60 s") }
            charts.distance?.let {
                HistoryChart(
                    title = "Distance (m)",
                    segments = it,
                    emptyText = if (distanceMeasured) {
                        "No data in the last 60 s"
                    } else {
                        "No data: the distance needs GPS on and precise location"
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryChart(title: String, segments: List<ChartSegment>, emptyText: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (segments.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium)
            } else {
                // One Vico series per segment, so pauses show up as gaps.
                val model = remember(segments) {
                    CartesianChartModel(
                        LineCartesianLayerModel.build {
                            segments.forEach { segment -> series(segment.x, segment.y) }
                        },
                    )
                }
                CartesianChartHost(
                    chart = rememberHistoryChart(),
                    model = model,
                    modifier = Modifier.fillMaxWidth(),
                    // Fixed 60 s window: no scrolling, the whole window fits the width.
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    chartAreaHeight = 160.dp,
                )
            }
        }
    }
    SideEffect {
        ChainLog.sampled(ChainLog.UI, "Chart $title") {
            val latest = segments.lastOrNull()?.y?.lastOrNull()
            "shown ${segments.sumOf { it.x.size }} points in ${segments.size} segment(s), " +
                "latest ${formatNumber(latest, 2)}"
        }
    }
}

/** Line chart with x from −60 s to 0 s (x values are tenths of a second) and labels every 10 s. */
@Composable
private fun rememberHistoryChart(): CartesianChart {
    val line = LineCartesianLayer.rememberLine()
    val layer = rememberLineCartesianLayer(
        // The same line style for every segment
        lineProvider = LineCartesianLayer.LineProvider.series(line),
        rangeProvider = remember {
            CartesianLayerRangeProvider.fixed(minX = -History.WINDOW_TENTHS.toDouble(), maxX = 0.0)
        },
    )
    val secondsFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${(value / 10).roundToInt()} s" }
    }
    val tenSecondLabels = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 100 }) }
    return rememberCartesianChart(
        layer,
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = secondsFormatter,
            itemPlacer = tenSecondLabels,
        ),
        getXStep = { _, _, _ -> 1.0 }, // one step = 0.1 s
    )
}
