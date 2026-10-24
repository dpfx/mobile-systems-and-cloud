package de.penguin.sensorlab.processing

/** Fixed-size buffer: when it is full, a new item overwrites the oldest one. */
class RingBuffer<T>(private val capacity: Int) {
    private val items = arrayOfNulls<Any>(capacity)
    private var start = 0 // index of the oldest item
    private var size = 0

    fun add(item: T) {
        items[(start + size) % capacity] = item
        if (size < capacity) {
            size++
        } else {
            start = (start + 1) % capacity // the oldest item was overwritten
        }
    }

    fun last(): T? = if (size == 0) null else get(size - 1)

    fun replaceLast(item: T) {
        check(size > 0) { "Buffer is empty" }
        items[(start + size - 1) % capacity] = item
    }

    /** Items from oldest to newest. */
    fun toList(): List<T> = List(size) { get(it) }

    fun clear() {
        items.fill(null)
        start = 0
        size = 0
    }

    @Suppress("UNCHECKED_CAST") // only items of type T are stored
    private fun get(index: Int): T = items[(start + index) % capacity] as T
}
