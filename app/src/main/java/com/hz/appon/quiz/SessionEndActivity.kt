package com.hz.appon.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hz.appon.App
import com.hz.appon.R
import com.hz.appon.databinding.ActivitySessionEndBinding
import com.hz.appon.home.HomeActivity
import timber.log.Timber

/** Score summary screen shown after every quiz session (win or game-over). */
class SessionEndActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionEndBinding
    private val container by lazy { (application as App).container }

    private val viewModel: SessionEndViewModel by viewModels {
        SessionEndViewModel.Factory(
            container.gamificationEngine,
            container.livesModule_,
            container.userPreferences
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionEndBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val total = intent.getIntExtra(EXTRA_TOTAL, 0)
        val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
        val isGameOver = intent.getBooleanExtra(EXTRA_GAME_OVER, false)

        renderResults(score, total, isGameOver)
        setupButtons(categoryId, categoryName)

        if (viewModel.shouldShowInterstitial) {
            container.adManager.showInterstitial(this)
        }

        Timber.d("SessionEnd: score=$score/$total, gameOver=$isGameOver")
    }

    private fun renderResults(score: Int, total: Int, isGameOver: Boolean) {
        binding.textResult.text = if (isGameOver)
            getString(R.string.session_end_game_over)
        else
            getString(R.string.session_end_great_job)

        binding.textScore.text = getString(R.string.session_end_score, score, total)
        binding.textCorrect.text = getString(R.string.session_end_correct, score)
        binding.textWrong.text = getString(R.string.session_end_wrong, total - score)

        val lives = viewModel.livesState
        binding.btnWatchAd.visibility =
            if (lives.current < lives.max) View.VISIBLE else View.GONE
    }

    private fun setupButtons(categoryId: Int, categoryName: String) {
        binding.btnWatchAd.setOnClickListener {
            container.adManager.showRewarded(this) {
                viewModel.onRewardedAdComplete()
                binding.btnWatchAd.visibility = View.GONE
            }
        }
        binding.btnPlayAgain.setOnClickListener {
            startActivity(QuizActivity.newIntent(this, categoryId, categoryName))
            finish()
        }
        binding.btnGoHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    companion object {
        private const val EXTRA_SCORE = "extra_score"
        private const val EXTRA_TOTAL = "extra_total"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_CATEGORY_NAME = "extra_category_name"
        private const val EXTRA_GAME_OVER = "extra_game_over"

        fun newIntent(
            context: Context,
            score: Int,
            total: Int,
            categoryId: Int,
            categoryName: String,
            isGameOver: Boolean
        ): Intent = Intent(context, SessionEndActivity::class.java)
            .putExtra(EXTRA_SCORE, score)
            .putExtra(EXTRA_TOTAL, total)
            .putExtra(EXTRA_CATEGORY_ID, categoryId)
            .putExtra(EXTRA_CATEGORY_NAME, categoryName)
            .putExtra(EXTRA_GAME_OVER, isGameOver)
    }
}
