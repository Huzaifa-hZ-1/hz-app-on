package com.hz.appon.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hz.appon.App
import com.hz.appon.databinding.ActivityOnboardingBinding
import com.hz.appon.home.HomeActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Category selection screen, shown on first launch and when re-opening from Home.
 *
 * Passes [EXTRA_RESELECT] = true when launched from Home so the user can change their
 * categories without going through a "first launch" flow.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val container by lazy { (application as App).container }

    // viewModels delegates ViewModel creation to the Factory, scoped to this Activity's lifecycle
    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModel.Factory(container.questionRepository, container.userPreferences)
    }

    private val adapter = CategoryAdapter { categoryId -> viewModel.toggleCategory(categoryId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        observeState()

        binding.btnContinue.setOnClickListener { viewModel.saveAndContinue() }
        Timber.d("OnboardingActivity created, reselect=${intent.getBooleanExtra(EXTRA_RESELECT, false)}")
    }

    private fun setupRecycler() {
        binding.recyclerCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerCategories.adapter = adapter
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.categories.collectLatest { adapter.submitList(it) }
        }
        lifecycleScope.launch {
            viewModel.canContinue.collectLatest { enabled ->
                binding.btnContinue.isEnabled = enabled
                binding.textSelectHint.visibility =
                    if (enabled) android.view.View.GONE else android.view.View.VISIBLE
            }
        }
        lifecycleScope.launch {
            viewModel.navigateAway.collectLatest { navigate ->
                if (navigate) navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            // Clear the back stack so Back from Home exits the app, not returns to Onboarding
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    companion object {
        const val EXTRA_RESELECT = "extra_reselect"

        /** Creates an Intent for re-opening category selection from Home. */
        fun newReselect(context: Context): Intent =
            Intent(context, OnboardingActivity::class.java)
                .putExtra(EXTRA_RESELECT, true)
    }
}
