package com.hz.appon.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hz.appon.App
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ActivityHomeBinding
import com.hz.appon.databinding.BottomSheetDifficultyBinding
import com.hz.appon.onboarding.OnboardingActivity
import com.hz.appon.quiz.QuizActivity
import com.hz.appon.quiz.QuizMode
import com.hz.appon.quiz.heartsDisplay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/** Main hub screen. Shows selected categories as tappable cards. */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val container by lazy { (application as App).container }

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            container.questionRepository,
            container.gamificationEngine,
            container.networkMonitor
        )
    }

    private val adapter = CategoryCardAdapter { category ->
        showDifficultyPicker(category)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerCategories.adapter = adapter

        binding.btnEdit.setOnClickListener {
            startActivity(OnboardingActivity.newReselect(this))
        }

        observeState()
        container.adManager.loadBanner(binding.adBannerContainer)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSelectedCategories()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.categories.collectLatest { adapter.submitList(it) }
        }
        lifecycleScope.launch {
            viewModel.networkMonitor.isOnline.collectLatest { online ->
                binding.bannerOffline.visibility = if (online) View.GONE else View.VISIBLE
            }
        }
        lifecycleScope.launch {
            viewModel.livesState.collectLatest { lives ->
                binding.textHearts.text = heartsDisplay(lives.current, lives.max)
            }
        }
    }

    private fun showDifficultyPicker(category: Category) {
        val sheet = BottomSheetDialog(this)
        val sheetBinding = BottomSheetDifficultyBinding.inflate(layoutInflater)
        sheetBinding.textSheetCategoryName.text = category.name

        fun pick(mode: QuizMode) {
            sheet.dismiss()
            Timber.d("Starting quiz: ${category.name}, mode=$mode")
            startActivity(QuizActivity.newIntent(this, category.id, category.name, mode))
        }

        sheetBinding.btnEasy.setOnClickListener { pick(QuizMode.EASY) }
        sheetBinding.btnMedium.setOnClickListener { pick(QuizMode.MEDIUM) }
        sheetBinding.btnHard.setOnClickListener { pick(QuizMode.HARD) }
        sheetBinding.btnProgressive.setOnClickListener { pick(QuizMode.PROGRESSIVE) }

        sheet.setContentView(sheetBinding.root)
        sheet.show()
    }
}
