package com.hz.appon.gamification

/**
 * Events emitted by the quiz flow and consumed by [GamificationEngine].
 * Each [GamificationModule] responds only to the events it cares about.
 */
sealed class GameEvent {
    object SessionStarted : GameEvent()
    object CorrectAnswer : GameEvent()
    object WrongAnswer : GameEvent()
    /** @param score Number of correct answers in the completed session. */
    data class SessionEnded(val score: Int) : GameEvent()
}
