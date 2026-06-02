package com.hz.appon.shared

import android.content.Context
import com.hz.appon.ads.AdManager
import com.hz.appon.data.local.QuizDatabase
import com.hz.appon.data.remote.OpenTdbApi
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.data.repository.QuestionRepositoryImpl
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.lives.LivesModule

/**
 * Manual dependency injection container. Created once in [App] and accessed via
 * `(application as App).container` in Activities.
 *
 * Replace individual components in tests by subclassing and overriding properties.
 */
class AppContainer(context: Context) {

    val userPreferences = UserPreferences(context)
    val networkMonitor = NetworkMonitor(context)

    private val database = QuizDatabase.getInstance(context)
    private val api = OpenTdbApi.create()

    val questionRepository: QuestionRepository = QuestionRepositoryImpl(
        questionDao = database.questionDao(),
        categoryDao = database.categoryDao(),
        api = api
    )

    private val livesModule = LivesModule(userPreferences)

    /** Shared engine — same instance used by all screens so state is consistent. */
    val gamificationEngine = GamificationEngine(listOf(livesModule))

    /** Exposes LivesModule so SessionEndViewModel can call addHeart() after a rewarded ad. */
    val livesModule_ = livesModule

    val adManager = AdManager(context)
}
