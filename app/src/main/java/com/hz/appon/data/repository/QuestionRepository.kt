package com.hz.appon.data.repository

import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question

/**
 * Contract for accessing trivia questions and categories.
 * Callers never know whether data comes from the network, cache, or bundled fallback.
 */
interface QuestionRepository {

    /** Returns all available categories, fetching from OpenTDB if the local list is empty. */
    suspend fun getCategories(): List<Category>

    /**
     * Returns [count] questions for the given [categoryId] and [difficulty].
     * Fetches from OpenTDB and caches locally if the cache has fewer than [count] entries.
     */
    suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, count: Int): List<Question>

    /**
     * Returns the 5 hardcoded offline questions. Always available regardless of connectivity.
     * Used when the device has no internet connection.
     */
    suspend fun getBundledQuestions(): List<Question>

    /** Saves the user's selected category IDs, replacing the previous selection. */
    suspend fun saveSelectedCategories(ids: List<Int>)

    /** Returns the categories the user has selected during onboarding. */
    suspend fun getSelectedCategories(): List<Category>
}
