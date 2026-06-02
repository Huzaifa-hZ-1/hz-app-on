package com.hz.appon.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hz.appon.App
import com.hz.appon.R
import com.hz.appon.databinding.ActivityHomeBinding
import com.hz.appon.onboarding.OnboardingActivity
import com.hz.appon.quiz.QuizActivity
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
        Timber.d("Starting quiz for category: ${category.name}")
        startActivity(QuizActivity.newIntent(this, category.id, category.name))
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

    // onResume is called every time the Activity becomes visible — including when returning
    // from Onboarding after re-selecting categories.
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
                binding.textHearts.text = getString(
                    R.string.home_hearts_label, lives.current, lives.max
                )
            }
        }
    }
}
