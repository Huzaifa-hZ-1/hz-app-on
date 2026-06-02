package com.hz.appon.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.LivesState
import com.hz.appon.gamification.lives.LivesModule
import com.hz.appon.shared.UserPreferences
import timber.log.Timber

/**
 * ViewModel for the session end screen.
 *
 * Increments the session counter on creation (drives interstitial ad cadence).
 * Exposes whether to show the "Watch ad → restore heart" button.
 */
class SessionEndViewModel(
    private val engine: GamificationEngine,
    private val livesModule: LivesModule,
    private val userPreferences: UserPreferences
) : ViewModel() {

    /** True when player has < max hearts and should be offered the rewarded ad. */
    val canRestoreHeart: Boolean
        get() = engine.gameState.value.lives.current < engine.gameState.value.lives.max

    /** Current lives state for display. */
    val livesState: LivesState get() = engine.gameState.value.lives

    /**
     * Whether an interstitial ad should be shown.
     * True every 3rd completed session.
     */
    val shouldShowInterstitial: Boolean
        get() = userPreferences.sessionsPlayedCount % 3 == 0 &&
                userPreferences.sessionsPlayedCount > 0

    init {
        userPreferences.sessionsPlayedCount += 1
        Timber.d("Session count: ${userPreferences.sessionsPlayedCount}")
    }

    /**
     * Adds one heart after a rewarded ad completes successfully.
     * Call this from the Activity's rewarded ad callback.
     */
    fun onRewardedAdComplete() {
        livesModule.addHeart()
        Timber.d("Heart restored via rewarded ad. Lives: ${engine.gameState.value.lives.current}")
    }

    class Factory(
        private val engine: GamificationEngine,
        private val livesModule: LivesModule,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionEndViewModel(engine, livesModule, userPreferences) as T
    }
}
