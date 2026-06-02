package com.hz.appon.gamification.lives

import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.GamificationModule
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Manages the hearts/lives system (Option A).
 *
 * Hearts are decremented on wrong answers and refilled at a rate of 1 per 30 minutes.
 * State is persisted to [UserPreferences] so hearts do not refill by restarting the app.
 *
 * To restore a heart after a rewarded ad, call [addHeart] directly.
 */
class LivesModule(private val userPreferences: UserPreferences) : GamificationModule {

    private val _state = MutableStateFlow(userPreferences.livesState)

    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.WrongAnswer -> decrementLives()
            is GameEvent.SessionStarted -> recalculateRefill()
            else -> Unit
        }
    }

    override fun getState(): LivesState = _state.value

    /**
     * Adds one heart, up to [LivesState.max].
     * Call after a rewarded ad completes successfully.
     */
    fun addHeart() {
        val current = _state.value
        if (current.current >= current.max) return
        val updated = current.copy(current = current.current + 1)
        persist(updated)
        Timber.d("Heart restored. Lives: ${updated.current}/${updated.max}")
    }

    private fun decrementLives() {
        val current = _state.value
        val updated = current.copy(
            current = maxOf(0, current.current - 1),
            lastLostAt = System.currentTimeMillis()
        )
        persist(updated)
        Timber.d("Wrong answer. Lives: ${updated.current}/${updated.max}")
    }

    private fun recalculateRefill() {
        val current = _state.value
        if (current.current >= current.max || current.lastLostAt == null) return

        val minutesElapsed = (System.currentTimeMillis() - current.lastLostAt) / 60_000
        val heartsToAdd = (minutesElapsed / REFILL_INTERVAL_MINUTES).toInt()

        if (heartsToAdd > 0) {
            val newCount = minOf(current.max, current.current + heartsToAdd)
            val updated = current.copy(
                current = newCount,
                lastLostAt = if (newCount >= current.max) null else current.lastLostAt
            )
            persist(updated)
            Timber.d("Refilled $heartsToAdd hearts. Lives: ${updated.current}/${updated.max}")
        }
    }

    private fun persist(state: LivesState) {
        _state.value = state
        userPreferences.livesState = state
    }

    companion object {
        /** One heart refills every 30 minutes. */
        private const val REFILL_INTERVAL_MINUTES = 30L
    }
}
