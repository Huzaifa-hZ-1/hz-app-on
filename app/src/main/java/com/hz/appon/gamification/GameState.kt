package com.hz.appon.gamification

/**
 * Snapshot of the player's gamification state.
 * Slots for [xp] and [streak] are null until Options B and C are implemented —
 * adding them requires zero changes to this class or existing modules.
 */
data class GameState(
    val lives: LivesState,
    val xp: Any? = null,      // Reserved for Option B (XpState)
    val streak: Any? = null   // Reserved for Option C (StreakState)
)

/**
 * Hearts/lives state persisted between sessions.
 *
 * @param current Number of hearts currently available (0–[max])
 * @param lastLostAt Epoch ms when the last heart was lost; used to calculate refill
 */
data class LivesState(
    val current: Int,
    val max: Int = 5,
    val lastLostAt: Long? = null
)
