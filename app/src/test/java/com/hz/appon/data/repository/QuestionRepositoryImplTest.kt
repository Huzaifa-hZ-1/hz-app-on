package com.hz.appon.data.repository

import com.hz.appon.data.local.CategoryDao
import com.hz.appon.data.local.QuestionDao
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.remote.OpenTdbApi
import com.hz.appon.data.remote.QuestionsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionRepositoryImplTest {

    private val questionDao = mockk<QuestionDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val api = mockk<OpenTdbApi>(relaxed = true)

    private val repo = QuestionRepositoryImpl(questionDao, categoryDao, api)

    @Test
    fun `getBundledQuestions always returns 5 questions`() = runTest {
        coEvery { questionDao.getBundled() } returns emptyList()
        val result = repo.getBundledQuestions()
        assertEquals(5, result.size)
    }

    @Test
    fun `getBundledQuestions returns only isBundled questions`() = runTest {
        coEvery { questionDao.getBundled() } returns emptyList()
        val result = repo.getBundledQuestions()
        assertTrue(result.all { it.isBundled })
    }

    @Test
    fun `getQuestions fetches from API when cache is empty`() = runTest {
        coEvery { questionDao.countQuestions(9, Difficulty.EASY) } returns 0
        coEvery { api.getQuestions(any(), any(), any(), any()) } returns
            QuestionsResponse(responseCode = 0, results = emptyList())
        coEvery { questionDao.getQuestions(9, Difficulty.EASY, 4) } returns emptyList()

        repo.getQuestions(9, Difficulty.EASY, 4)

        coVerify { api.getQuestions(any(), 9, "easy", any()) }
    }

    @Test
    fun `getQuestions returns from cache when sufficient questions exist`() = runTest {
        val cached = List(10) { makeQuestion(it) }
        coEvery { questionDao.countQuestions(9, Difficulty.EASY) } returns 10
        coEvery { questionDao.getQuestions(9, Difficulty.EASY, 4) } returns cached.take(4)

        val result = repo.getQuestions(9, Difficulty.EASY, 4)

        assertEquals(4, result.size)
        coVerify(exactly = 0) { api.getQuestions(any(), any(), any(), any()) }
    }

    private fun makeQuestion(i: Int) = Question(
        id = "9_EASY_$i", categoryId = 9, text = "Q$i",
        options = listOf("A", "B", "C", "D"), correctAnswer = "A",
        difficulty = Difficulty.EASY
    )
}
