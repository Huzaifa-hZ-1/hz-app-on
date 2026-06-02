package com.hz.appon.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.QuizSession
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the active quiz session.
 *
 * Owns the session lifecycle: load questions → present one at a time → handle answers →
 * track timer → emit SessionComplete when done or when hearts reach 0.
 */
class QuizViewModel(
    private val repository: QuestionRepository,
    private val engine: GamificationEngine,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Question(
            val session: QuizSession,
            val currentQuestion: com.hz.appon.data.model.Question,
            val selectedAnswer: String? = null,
            val timeRemaining: Int = TIMER_SECONDS,
            val lives: LivesState
        ) : UiState()
        data class SessionComplete(val session: QuizSession, val isGameOver: Boolean) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    /** UI state for QuizActivity to render. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Loads questions and starts the session for the given [categoryId] and [mode].
     * Offline sessions always use the 5 bundled questions regardless of [mode].
     */
    fun startSession(categoryId: Int, mode: QuizMode = QuizMode.PROGRESSIVE) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            engine.onEvent(GameEvent.SessionStarted)
            try {
                val isOffline = !networkMonitor.isOnline.value
                val questions = if (isOffline) {
                    Timber.d("Offline — loading bundled questions")
                    repository.getBundledQuestions()
                } else {
                    Timber.d("Online — mode=$mode, category=$categoryId")
                    when (mode) {
                        QuizMode.EASY -> repository.getQuestions(categoryId, Difficulty.EASY, 10)
                        QuizMode.MEDIUM -> repository.getQuestions(categoryId, Difficulty.MEDIUM, 10)
                        QuizMode.HARD -> repository.getQuestions(categoryId, Difficulty.HARD, 10)
                        QuizMode.PROGRESSIVE ->
                            repository.getQuestions(categoryId, Difficulty.EASY, 4) +
                            repository.getQuestions(categoryId, Difficulty.MEDIUM, 3) +
                            repository.getQuestions(categoryId, Difficulty.HARD, 3)
                    }
                }
                val session = QuizSession(categoryId, questions, isOffline = isOffline)
                presentQuestion(session)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start session for category $categoryId")
                _uiState.value = UiState.Error("Failed to load questions")
            }
        }
    }

    /**
     * Submits an answer for the current question.
     * Pass an empty string to treat as a timeout (wrong answer with no selection).
     */
    fun submitAnswer(answer: String) {
        val state = _uiState.value as? UiState.Question ?: return
        timerJob?.cancel()

        val isCorrect = answer.isNotEmpty() && answer == state.currentQuestion.correctAnswer
        engine.onEvent(if (isCorrect) GameEvent.CorrectAnswer else GameEvent.WrongAnswer)

        val newCorrectCount = if (isCorrect) state.session.correctCount + 1 else state.session.correctCount
        _uiState.value = state.copy(selectedAnswer = answer)

        // Delay briefly so the user sees the correct/wrong colour feedback before advancing
        viewModelScope.launch {
            delay(ANSWER_FEEDBACK_MS)

            val lives = engine.gameState.value.lives
            val updatedSession = state.session.copy(
                currentIndex = state.session.currentIndex + 1,
                correctCount = newCorrectCount
            )

            when {
                lives.current == 0 -> {
                    Timber.d("Game over — hearts exhausted")
                    engine.onEvent(GameEvent.SessionEnded(newCorrectCount))
                    _uiState.value = UiState.SessionComplete(updatedSession, isGameOver = true)
                }
                updatedSession.isComplete -> {
                    Timber.d("Session complete — score $newCorrectCount/${updatedSession.totalQuestions}")
                    engine.onEvent(GameEvent.SessionEnded(newCorrectCount))
                    _uiState.value = UiState.SessionComplete(updatedSession, isGameOver = false)
                }
                else -> presentQuestion(updatedSession)
            }
        }
    }

    private fun presentQuestion(session: QuizSession) {
        val question = session.questions[session.currentIndex]
        _uiState.value = UiState.Question(
            session = session,
            currentQuestion = question,
            timeRemaining = TIMER_SECONDS,
            lives = engine.gameState.value.lives
        )
        startTimer(session)
        Timber.d("Question ${session.currentIndex + 1}/${session.totalQuestions}: ${question.difficulty}")
    }

    private fun startTimer(session: QuizSession) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in TIMER_SECONDS downTo 1) {
                val state = _uiState.value as? UiState.Question ?: return@launch
                _uiState.value = state.copy(timeRemaining = remaining)
                delay(1_000)
            }
            Timber.d("Timer expired")
            submitAnswer("")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val repository: QuestionRepository,
        private val engine: GamificationEngine,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(repository, engine, networkMonitor) as T
    }

    companion object {
        const val TIMER_SECONDS = 15
        private const val ANSWER_FEEDBACK_MS = 1_000L
    }
}
