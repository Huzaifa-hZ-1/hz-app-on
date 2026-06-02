package com.hz.appon.gamification.lives

import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.UserPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class LivesModuleTest {

    private fun makeModule(current: Int = 5, lastLostAt: Long? = null): LivesModule {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = current, lastLostAt = lastLostAt)
        return LivesModule(prefs)
    }

    @Test
    fun `WrongAnswer decrements hearts by 1`() {
        val module = makeModule(current = 5)
        module.onEvent(GameEvent.WrongAnswer)
        assertEquals(4, (module.getState() as LivesState).current)
    }

    @Test
    fun `hearts cannot go below 0`() {
        val module = makeModule(current = 0)
        module.onEvent(GameEvent.WrongAnswer)
        assertEquals(0, (module.getState() as LivesState).current)
    }

    @Test
    fun `addHeart increments hearts by 1`() {
        val module = makeModule(current = 3)
        module.addHeart()
        assertEquals(4, (module.getState() as LivesState).current)
    }

    @Test
    fun `addHeart does not exceed max`() {
        val module = makeModule(current = 5)
        module.addHeart()
        assertEquals(5, (module.getState() as LivesState).current)
    }

    @Test
    fun `WrongAnswer persists state to preferences`() {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = 5)
        val module = LivesModule(prefs)
        module.onEvent(GameEvent.WrongAnswer)
        verify { prefs.livesState = any() }
    }

    @Test
    fun `SessionStarted recalculates refill — 60 minutes gives 2 hearts`() {
        val sixtyMinutesAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        val module = makeModule(current = 2, lastLostAt = sixtyMinutesAgo)
        module.onEvent(GameEvent.SessionStarted)
        assertEquals(4, (module.getState() as LivesState).current)
    }
}
