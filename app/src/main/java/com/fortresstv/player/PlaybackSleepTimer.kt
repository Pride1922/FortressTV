package com.fortresstv.player

internal data class PlaybackSleepTimerState(
    val minutes: Int? = null,
    val remainingMinutes: Long? = null
)

/** Activity-owned deadline: replacing a player or hiding fullscreen cannot cancel it. */
internal class PlaybackSleepTimer(
    private val elapsedRealtime: () -> Long,
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    private val onStateChanged: (PlaybackSleepTimerState) -> Unit,
    private val onExpired: () -> Unit
) {
    var state = PlaybackSleepTimerState()
        private set
    var expired = false
        private set
    private var deadline: Long? = null
    private var cancelTick: (() -> Unit)? = null

    fun select(minutes: Int?, programmeRemainingMs: Long? = null) {
        if (expired) return
        cancelTick?.invoke()
        cancelTick = null
        deadline = when {
            minutes != null && minutes > 0 -> elapsedRealtime() + minutes * 60_000L
            minutes == -1 && programmeRemainingMs != null -> elapsedRealtime() + programmeRemainingMs.coerceAtLeast(0L)
            else -> null
        }
        state = PlaybackSleepTimerState(minutes)
        tick()
    }

    fun mediaEnded() {
        if (state.minutes == -1 && deadline == null) expire()
    }

    fun checkDeadline() {
        if (deadline?.let { elapsedRealtime() >= it } == true) expire()
    }

    fun dispose() {
        cancelTick?.invoke()
        cancelTick = null
    }

    private fun tick() {
        if (expired) return
        val remaining = deadline?.minus(elapsedRealtime())
        if (remaining != null && remaining <= 0L) {
            expire()
            return
        }
        state = state.copy(remainingMinutes = remaining?.let { (it + 59_999L) / 60_000L })
        onStateChanged(state)
        if (remaining != null) cancelTick = schedule(minOf(1_000L, remaining)) { tick() }
    }

    private fun expire() {
        if (expired) return
        expired = true
        dispose()
        state = PlaybackSleepTimerState()
        onStateChanged(state)
        onExpired()
    }
}
