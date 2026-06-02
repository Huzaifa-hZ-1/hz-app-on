package com.hz.appon.gamification

import com.hz.appon.gamification.lives.LivesModule
import com.hz.appon.shared.UserPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class GamificationEngineTest {

    private fun makeEngine(): Pair<GamificationEngine, LivesModule> {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = 5)
        val lives = LivesModule(prefs)
        val engine = GamificationEngine(listOf(lives))
        return engine to lives
    }

    @Test
    fun `initial state has full hearts`() {
        val (engine, _) = makeEngine()
        assertEquals(5, engine.gameState.value.lives.current)
    }

    @Test
    fun `WrongAnswer decrements lives by 1`() {
        val (engine, _) = makeEngine()
        engine.onEvent(GameEvent.WrongAnswer)
        assertEquals(4, engine.gameState.value.lives.current)
    }

    @Test
    fun `CorrectAnswer does not change lives`() {
        val (engine, _) = makeEngine()
        engine.onEvent(GameEvent.CorrectAnswer)
        assertEquals(5, engine.gameState.value.lives.current)
    }

    @Test
    fun `onEvent fans out to all modules`() {
        val module1 = mockk<GamificationModule>(relaxed = true)
        val module2 = mockk<GamificationModule>(relaxed = true)
        every { module1.getState() } returns LivesState(current = 5)
        every { module2.getState() } returns LivesState(current = 5)
        val engine = GamificationEngine(listOf(module1, module2))

        engine.onEvent(GameEvent.CorrectAnswer)

        verify { module1.onEvent(GameEvent.CorrectAnswer) }
        verify { module2.onEvent(GameEvent.CorrectAnswer) }
    }
}
