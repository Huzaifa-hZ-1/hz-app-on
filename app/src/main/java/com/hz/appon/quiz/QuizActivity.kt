package com.hz.appon.quiz

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hz.appon.App
import com.hz.appon.R
import com.hz.appon.databinding.ActivityQuizBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/** Active quiz screen. Presents one question at a time with a 15-second countdown. */
class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private val container by lazy { (application as App).container }

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModel.Factory(
            container.questionRepository,
            container.gamificationEngine,
            container.networkMonitor
        )
    }

    private val answerButtons by lazy {
        listOf(binding.btnAnswer0, binding.btnAnswer1, binding.btnAnswer2, binding.btnAnswer3)
    }

    private val answerColors = listOf(R.color.answer_a, R.color.answer_b, R.color.answer_c, R.color.answer_d)
    private val answerLabels = listOf("A", "B", "C", "D")

    private lateinit var quizMode: QuizMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
        quizMode = QuizMode.valueOf(
            intent.getStringExtra(EXTRA_QUIZ_MODE) ?: QuizMode.PROGRESSIVE.name
        )

        binding.textCategory.text = categoryName
        answerButtons.forEach { btn ->
            btn.setOnClickListener { viewModel.submitAnswer(btn.tag as? String ?: "") }
        }

        observeState()
        viewModel.startSession(categoryId, quizMode)
        Timber.d("QuizActivity started: $categoryName mode=$quizMode")
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is QuizViewModel.UiState.Loading -> setButtonsEnabled(false)
                    is QuizViewModel.UiState.Question -> renderQuestion(state)
                    is QuizViewModel.UiState.SessionComplete -> navigateToEnd(state)
                    is QuizViewModel.UiState.Error -> Timber.e("Quiz error: ${state.message}")
                }
            }
        }
    }

    private fun renderQuestion(state: QuizViewModel.UiState.Question) {
        val q = state.currentQuestion
        binding.textQuestion.text = q.text
        binding.textCounter.text = getString(
            R.string.quiz_question_counter,
            state.session.currentIndex + 1,
            state.session.totalQuestions
        )
        binding.textHearts.text = heartsDisplay(state.lives.current, state.lives.max)

        binding.progressTimer.progress = state.timeRemaining
        val timerColor = when {
            state.timeRemaining > 10 -> R.color.timer_progress
            state.timeRemaining > 5 -> R.color.timer_warning
            else -> R.color.timer_critical
        }
        binding.progressTimer.progressTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, timerColor))

        answerButtons.forEachIndexed { i, btn ->
            val answer = if (i < q.options.size) q.options[i] else ""
            btn.text = if (i < q.options.size) "${answerLabels[i]}   $answer" else ""
            btn.tag = answer
            btn.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, answerColors[i]))
            btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        state.selectedAnswer?.let { selected ->
            setButtonsEnabled(false)
            answerButtons.forEach { btn ->
                val answer = btn.tag as? String ?: ""
                when {
                    answer == q.correctAnswer ->
                        btn.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.correct_green)
                        )
                    answer == selected ->
                        btn.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.wrong_red)
                        )
                }
            }
        } ?: setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) =
        answerButtons.forEach { it.isEnabled = enabled }

    private fun navigateToEnd(state: QuizViewModel.UiState.SessionComplete) {
        startActivity(
            SessionEndActivity.newIntent(
                context = this,
                score = state.session.correctCount,
                total = state.session.totalQuestions,
                categoryId = state.session.categoryId,
                categoryName = binding.textCategory.text.toString(),
                isGameOver = state.isGameOver,
                mode = quizMode
            )
        )
        finish()
    }

    companion object {
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_CATEGORY_NAME = "extra_category_name"
        private const val EXTRA_QUIZ_MODE = "extra_quiz_mode"

        /** Creates an Intent to start a quiz for the given category and difficulty mode. */
        fun newIntent(
            context: Context,
            categoryId: Int,
            categoryName: String,
            mode: QuizMode = QuizMode.PROGRESSIVE
        ): Intent = Intent(context, QuizActivity::class.java)
            .putExtra(EXTRA_CATEGORY_ID, categoryId)
            .putExtra(EXTRA_CATEGORY_NAME, categoryName)
            .putExtra(EXTRA_QUIZ_MODE, mode.name)
    }
}

/** Returns filled/empty heart chars: "♥♥♥♡♡" */
internal fun heartsDisplay(current: Int, max: Int) =
    "♥".repeat(current.coerceAtLeast(0)) + "♡".repeat((max - current).coerceAtLeast(0))
