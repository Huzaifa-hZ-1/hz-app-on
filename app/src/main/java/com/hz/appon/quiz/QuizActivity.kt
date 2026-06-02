package com.hz.appon.quiz

import android.content.Context
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""

        binding.textCategory.text = categoryName
        answerButtons.forEach { btn -> btn.setOnClickListener { viewModel.submitAnswer(btn.text.toString()) } }

        observeState()
        viewModel.startSession(categoryId)
        Timber.d("QuizActivity started for category: $categoryName ($categoryId)")
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
        binding.textHearts.text = getString(R.string.quiz_hearts, state.lives.current)
        binding.progressTimer.progress = state.timeRemaining

        answerButtons.forEachIndexed { i, btn ->
            btn.text = if (i < q.options.size) q.options[i] else ""
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        }

        state.selectedAnswer?.let { selected ->
            setButtonsEnabled(false)
            answerButtons.forEach { btn ->
                when {
                    btn.text == q.correctAnswer ->
                        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_green))
                    btn.text == selected ->
                        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong_red))
                }
            }
        } ?: setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) =
        answerButtons.forEach { it.isEnabled = enabled }

    private fun navigateToEnd(state: QuizViewModel.UiState.SessionComplete) {
        val intent = SessionEndActivity.newIntent(
            context = this,
            score = state.session.correctCount,
            total = state.session.totalQuestions,
            categoryId = state.session.categoryId,
            categoryName = binding.textCategory.text.toString(),
            isGameOver = state.isGameOver
        )
        startActivity(intent)
        finish()
    }

    companion object {
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_CATEGORY_NAME = "extra_category_name"

        /** Creates an Intent to start a quiz for the given category. */
        fun newIntent(context: Context, categoryId: Int, categoryName: String): Intent =
            Intent(context, QuizActivity::class.java)
                .putExtra(EXTRA_CATEGORY_ID, categoryId)
                .putExtra(EXTRA_CATEGORY_NAME, categoryName)
    }
}
