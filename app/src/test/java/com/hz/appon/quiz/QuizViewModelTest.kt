package com.hz.appon.quiz

import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.model.QuizSession
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.GameState
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.MainCoroutineRule
import com.hz.appon.shared.NetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuizViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val repo = mockk<QuestionRepository>(relaxed = true)
    private val engine = mockk<GamificationEngine>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private fun makeVm(): QuizViewModel {
        val gameState = MutableStateFlow(GameState(lives = LivesState(current = 5)))
        coEvery { engine.gameState } returns gameState
        coEvery { networkMonitor.isOnline } returns MutableStateFlow(true)
        coEvery { repo.getQuestions(any(), Difficulty.EASY, any()) } returns makeQuestions(4, Difficulty.EASY)
        coEvery { repo.getQuestions(any(), Difficulty.MEDIUM, any()) } returns makeQuestions(3, Difficulty.MEDIUM)
        coEvery { repo.getQuestions(any(), Difficulty.HARD, any()) } returns makeQuestions(3, Difficulty.HARD)
        return QuizViewModel(repo, engine, networkMonitor)
    }

    @Test
    fun `startSession emits Question state with 10 questions`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        val state = vm.uiState.value
        assertTrue(state is QuizViewModel.UiState.Question)
        assertEquals(10, (state as QuizViewModel.UiState.Question).session.totalQuestions)
    }

    @Test
    fun `correct answer fires CorrectAnswer event`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        val state = vm.uiState.value as QuizViewModel.UiState.Question
        vm.submitAnswer(state.currentQuestion.correctAnswer)
        verify { engine.onEvent(GameEvent.CorrectAnswer) }
    }

    @Test
    fun `wrong answer fires WrongAnswer event`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        vm.submitAnswer("definitely_wrong")
        verify { engine.onEvent(GameEvent.WrongAnswer) }
    }

    @Test
    fun `offline session uses bundled questions`() = runTest {
        coEvery { networkMonitor.isOnline } returns MutableStateFlow(false)
        val bundled = makeQuestions(5, Difficulty.EASY, bundled = true)
        coEvery { repo.getBundledQuestions() } returns bundled
        val gameState = MutableStateFlow(GameState(lives = LivesState(current = 5)))
        coEvery { engine.gameState } returns gameState
        val vm = QuizViewModel(repo, engine, networkMonitor)
        vm.startSession(9)
        val state = vm.uiState.value as QuizViewModel.UiState.Question
        assertTrue(state.session.isOffline)
    }

    private fun makeQuestions(n: Int, d: Difficulty, bundled: Boolean = false) = List(n) { i ->
        Question("q$i", 9, "Question $i", listOf("A", "B", "C", "D"), "A", d, bundled)
    }
}
