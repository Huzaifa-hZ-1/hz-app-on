package com.hz.appon.shared

import android.content.Context
import com.google.gson.Gson
import com.hz.appon.gamification.LivesState

/**
 * Lightweight wrapper around SharedPreferences.
 * Persists user settings and game state that must survive process death.
 */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** True after the user has completed category selection on first launch. */
    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /**
     * Total number of sessions completed (win or loss).
     * Drives the interstitial ad trigger — shown every 3rd session.
     */
    var sessionsPlayedCount: Int
        get() = prefs.getInt(KEY_SESSIONS, 0)
        set(value) = prefs.edit().putInt(KEY_SESSIONS, value).apply()

    /**
     * Persisted lives state so hearts don't refill by restarting the app.
     * Defaults to full hearts on first launch.
     */
    var livesState: LivesState
        get() {
            val json = prefs.getString(KEY_LIVES, null) ?: return LivesState(current = 5)
            return gson.fromJson(json, LivesState::class.java)
        }
        set(value) = prefs.edit().putString(KEY_LIVES, gson.toJson(value)).apply()

    companion object {
        private const val PREFS_NAME = "hz_app_prefs"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_SESSIONS = "sessions_count"
        private const val KEY_LIVES = "lives_state"
    }
}
