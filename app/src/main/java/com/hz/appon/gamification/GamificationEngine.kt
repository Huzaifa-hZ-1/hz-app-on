package com.hz.appon.gamification

import com.hz.appon.gamification.lives.LivesModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Orchestrates all gamification modules.
 *
 * Receives [GameEvent]s from the quiz layer, fans them out to every registered
 * [GamificationModule], then rebuilds and publishes a fresh [GameState].
 *
 * To add Option B (XP): pass `XpModule()` in [modules] — no changes here.
 */
class GamificationEngine(private val modules: List<GamificationModule>) {

    private val _gameState = MutableStateFlow(buildState())
    /** Current combined state of all gamification systems. */
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /**
     * Dispatches [event] to all modules and refreshes [gameState].
     * Call from [QuizViewModel] for every player action.
     */
    fun onEvent(event: GameEvent) {
        Timber.d("GameEvent: $event")
        modules.forEach { it.onEvent(event) }
        _gameState.value = buildState()
    }

    private fun buildState(): GameState {
        val livesState = modules
            .filterIsInstance<LivesModule>()
            .firstOrNull()
            ?.getState() as? LivesState
            ?: LivesState(current = 5)
        return GameState(lives = livesState)
    }
}
