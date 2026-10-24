package de.penguin.sensorlab.processing

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.roundToLong

/** One connected piece of a chart line. x = time before now in 0.1 s steps (−600 … 0). */
@Immutable
data class ChartSegment(val x: List<Double>, val y: List<Double>)

/**
 * Chart history of one quantity (spec section 2): a ring buffer for the last 60 s with at most
 * 10 points per second, so 600 points.
 *
 * @param keepPeak if true, a point keeps the highest value of its 100 ms (so short shocks stay
 *   visible in the |a| chart); otherwise it keeps the latest value.
 */
class History(private val keepPeak: Boolean) {
    private data class Point(val timeNanos: Long, val value: Float, val segment: Int)

    private val points = RingBuffer<Point>(CAPACITY)
    private var segment = 0

    /** The next point starts a new line segment, so the chart shows a gap (e.g. after a pause). */
    fun startNewSegment() {
        segment++
    }

    fun add(timeNanos: Long, value: Float) {
        val last = points.last()
        val sameSlot = last != null && last.segment == segment &&
            timeNanos - last.timeNanos < MIN_POINT_INTERVAL_NANOS
        if (last != null && sameSlot) {
            val newValue = if (keepPeak) max(last.value, value) else value
            points.replaceLast(last.copy(value = newValue))
        } else {
            points.add(Point(timeNanos, value, segment))
        }
    }

    fun clear() = points.clear()

    /** The points of the last 60 s before [nowNanos], split into segments at gaps. */
    fun segments(nowNanos: Long): List<ChartSegment> {
        val result = mutableListOf<ChartSegment>()
        var x = mutableListOf<Double>()
        var y = mutableListOf<Double>()
        var currentSegment: Int? = null
        for (point in points.toList()) {
            val ageTenths = ((nowNanos - point.timeNanos) / NANOS_PER_TENTH).roundToLong()
                .coerceAtLeast(0)
            if (ageTenths > WINDOW_TENTHS) continue // older than 60 s
            if (point.segment != currentSegment && x.isNotEmpty()) {
                result += ChartSegment(x, y)
                x = mutableListOf()
                y = mutableListOf()
            }
            currentSegment = point.segment
            x += -ageTenths.toDouble()
            y += point.value.toDouble()
        }
        if (x.isNotEmpty()) result += ChartSegment(x, y)
        return result
    }

    companion object {
        /** Chart window: 60 s in steps of 0.1 s. */
        const val WINDOW_TENTHS = 600
        private const val CAPACITY = 600 // 60 s × 10 points/s
        private const val MIN_POINT_INTERVAL_NANOS = 100_000_000L // 10 points/s
        private const val NANOS_PER_TENTH = 100_000_000.0
    }
}
