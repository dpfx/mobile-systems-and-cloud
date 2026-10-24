package de.penguin.sensorlab.processing

/**
 * Measures the event rate in Hz over the last 2 s, from event timestamps (spec section 1).
 * Holds at most the timestamps of the last 2 s, so memory stays bounded.
 */
class RateMeter {
    private val timestamps = ArrayDeque<Long>()

    fun add(timestampNanos: Long) {
        timestamps.addLast(timestampNanos)
        while (timestampNanos - timestamps.first() > WINDOW_NANOS) {
            timestamps.removeFirst()
        }
    }

    /**
     * Events per second in the 2 s before the newest event: (events − 1) / time span.
     * Returns 0 if there are fewer than two events or the newest one is older than 2 s at [nowNanos].
     */
    fun rateHz(nowNanos: Long): Double {
        if (timestamps.size < 2 || nowNanos - timestamps.last() > WINDOW_NANOS) return 0.0
        val spanNanos = timestamps.last() - timestamps.first()
        if (spanNanos <= 0) return 0.0
        return (timestamps.size - 1) * NANOS_PER_SECOND / spanNanos
    }

    fun clear() = timestamps.clear()

    private companion object {
        const val WINDOW_NANOS = 2_000_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
