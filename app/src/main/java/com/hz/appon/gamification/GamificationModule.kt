package com.hz.appon.gamification

/**
 * Contract for a single gamification system (lives, XP, streak, etc.).
 *
 * Add new systems by implementing this interface and passing the instance to
 * [GamificationEngine] — no existing code needs to change.
 */
interface GamificationModule {
    /** Called by [GamificationEngine] for every game event. */
    fun onEvent(event: GameEvent)

    /**
     * Returns this module's current state snapshot.
     * Cast to the concrete state type (e.g. [LivesState]) when reading in a ViewModel.
     */
    fun getState(): Any
}
