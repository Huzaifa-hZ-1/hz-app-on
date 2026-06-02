package com.hz.appon.data.repository

import android.text.Html
import com.hz.appon.data.local.CategoryDao
import com.hz.appon.data.local.QuestionDao
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.remote.OpenTdbApi
import timber.log.Timber

/**
 * Production implementation of [QuestionRepository].
 *
 * Online path: OpenTDB API → shuffle options → insert into Room → return from Room.
 * Offline path: query Room cache → fall back to [BUNDLED_QUESTIONS] if insufficient.
 */
class QuestionRepositoryImpl(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao,
    private val api: OpenTdbApi
) : QuestionRepository {

    override suspend fun getCategories(): List<Category> {
        val cached = categoryDao.getAll()
        if (cached.isNotEmpty()) {
            Timber.d("Returning ${cached.size} cached categories")
            return cached
        }
        Timber.d("Fetching categories from OpenTDB")
        val response = api.getCategories()
        val categories = response.categories.map { Category(id = it.id, name = it.name) }
        categoryDao.insertAll(categories)
        return categories
    }

    override suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, count: Int): List<Question> {
        val cached = questionDao.countQuestions(categoryId, difficulty)
        if (cached >= count) {
            Timber.d("Returning $count cached ${difficulty.name} questions for category $categoryId")
            return questionDao.getQuestions(categoryId, difficulty, count)
        }
        Timber.d("Fetching $count ${difficulty.name} questions for category $categoryId from API")
        return try {
            val response = api.getQuestions(
                amount = maxOf(count, 10),
                categoryId = categoryId,
                difficulty = difficulty.name.lowercase()
            )
            val questions = response.results.mapIndexed { index, dto ->
                val correct = decode(dto.correctAnswer)
                val options = (dto.incorrectAnswers.map { decode(it) } + correct).shuffled()
                Question(
                    id = "${categoryId}_${difficulty.name}_$index",
                    categoryId = categoryId,
                    text = decode(dto.question),
                    options = options,
                    correctAnswer = correct,
                    difficulty = difficulty
                )
            }
            questionDao.insertAll(questions)
            questionDao.getQuestions(categoryId, difficulty, count)
        } catch (e: Exception) {
            Timber.w(e, "API fetch failed for category $categoryId, returning cache")
            questionDao.getQuestions(categoryId, difficulty, count)
        }
    }

    override suspend fun getBundledQuestions(): List<Question> {
        val stored = questionDao.getBundled()
        if (stored.isNotEmpty()) return stored
        questionDao.insertAll(BUNDLED_QUESTIONS)
        return BUNDLED_QUESTIONS
    }

    override suspend fun saveSelectedCategories(ids: List<Int>) {
        categoryDao.clearAllSelections()
        ids.forEach { categoryDao.setSelected(it, true) }
        Timber.d("Saved ${ids.size} selected categories")
    }

    override suspend fun getSelectedCategories(): List<Category> =
        categoryDao.getSelected()

    @Suppress("DEPRECATION")
    private fun decode(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()

    companion object {
        /** Five general-knowledge questions always available offline. */
        val BUNDLED_QUESTIONS = listOf(
            Question("bundled_0", -1, "What is the capital of France?",
                listOf("Paris", "London", "Berlin", "Madrid"), "Paris", Difficulty.EASY, true),
            Question("bundled_1", -1, "How many sides does a hexagon have?",
                listOf("5", "6", "7", "8"), "6", Difficulty.EASY, true),
            Question("bundled_2", -1, "Which planet is known as the Red Planet?",
                listOf("Venus", "Jupiter", "Mars", "Saturn"), "Mars", Difficulty.EASY, true),
            Question("bundled_3", -1, "What is the chemical symbol for water?",
                listOf("CO2", "H2O", "O2", "NaCl"), "H2O", Difficulty.MEDIUM, true),
            Question("bundled_4", -1, "Who wrote 'Romeo and Juliet'?",
                listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"),
                "William Shakespeare", Difficulty.MEDIUM, true)
        )
    }
}
