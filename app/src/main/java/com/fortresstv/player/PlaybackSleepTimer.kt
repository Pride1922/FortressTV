package com.fortresstv.player

internal data class PlaybackSleepTimerState(
    val minutes: Int? = null,
    val remainingMinutes: Long? = null,
    val awaitingConfirmation: Boolean = false,
    val confirmationSeconds: Int? = null
)

/** Activity-owned deadline: replacing a player or hiding fullscreen cannot cancel it. */
internal class PlaybackSleepTimer(
    private val elapsedRealtime: () -> Long,
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    private val onStateChanged: (PlaybackSleepTimerState) -> Unit,
    private val onExpired: () -> Unit,
    private val onAutoStop: () -> Unit = {}
) {
    var state = PlaybackSleepTimerState()
        private set
    var expired = false
        private set
    private var deadline: Long? = null
    private var confirmationDeadline: Long? = null
    private var cancelTick: (() -> Unit)? = null

    fun select(minutes: Int?, programmeRemainingMs: Long? = null) {
        cancelTick?.invoke()
        cancelTick = null
        expired = false
        confirmationDeadline = null
        deadline = when {
            minutes != null && minutes > 0 -> elapsedRealtime() + minutes * 60_000L
            minutes == -1 && programmeRemainingMs != null -> elapsedRealtime() + programmeRemainingMs.coerceAtLeast(0L)
            else -> null
        }
        state = PlaybackSleepTimerState(minutes = minutes)
        tick()
    }

    fun mediaEnded() {
        if (state.minutes == -1 && deadline == null) expire()
    }

    fun checkDeadline() {
        if (expired && confirmationDeadline?.let { elapsedRealtime() >= it } == true) {
            completeStop()
        } else if (deadline?.let { elapsedRealtime() >= it } == true) {
            expire()
        }
    }

    fun dispose() {
        cancelTick?.invoke()
        cancelTick = null
    }

    fun stopNow() {
        if (expired) completeStop()
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
        deadline = null
        confirmationDeadline = elapsedRealtime() + CONFIRMATION_SECONDS * 1_000L
        state = state.copy(
            remainingMinutes = null,
            awaitingConfirmation = true,
            confirmationSeconds = CONFIRMATION_SECONDS
        )
        onStateChanged(state)
        onExpired()
        scheduleConfirmationTick()
    }

    private fun scheduleConfirmationTick() {
        val remainingMs = confirmationDeadline?.minus(elapsedRealtime()) ?: 0L
        if (remainingMs <= 0L) {
            completeStop()
            return
        }
        val seconds = ((remainingMs + 999L) / 1_000L).toInt()
        state = state.copy(confirmationSeconds = seconds)
        onStateChanged(state)
        cancelTick = schedule(minOf(1_000L, remainingMs)) {
            if (!expired) return@schedule
            scheduleConfirmationTick()
        }
    }

    private fun completeStop() {
        if (!expired) return
        expired = false
        confirmationDeadline = null
        dispose()
        state = PlaybackSleepTimerState()
        onStateChanged(state)
        onAutoStop()
    }

    private companion object {
        const val CONFIRMATION_SECONDS = 30
    }
}
