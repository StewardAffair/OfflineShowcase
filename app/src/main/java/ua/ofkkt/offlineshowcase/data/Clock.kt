package ua.ofkkt.offlineshowcase.data

/** Годинник із можливістю «перемотати час» — щоб не чекати 30 хвилин у демо. */
class Clock {
    @Volatile
    var offsetMs: Long = 0L
        private set

    fun now(): Long = System.currentTimeMillis() + offsetMs

    fun advance(ms: Long) {
        offsetMs += ms
    }
}
