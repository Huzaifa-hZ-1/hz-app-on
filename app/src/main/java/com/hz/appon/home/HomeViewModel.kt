package com.hz.appon.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Category
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** ViewModel for the Home screen. Owns selected categories, lives display, and network state. */
class HomeViewModel(
    private val repository: QuestionRepository,
    private val gamificationEngine: GamificationEngine,
    val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    /** Selected categories to display as playable cards. */
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _livesState = MutableStateFlow(gamificationEngine.gameState.value.lives)
    /** Current lives state — reflects any hearts gained/lost since last session. */
    val livesState: StateFlow<LivesState> = _livesState.asStateFlow()

    init {
        loadSelectedCategories()
        viewModelScope.launch {
            gamificationEngine.gameState.collect { _livesState.value = it.lives }
        }
    }

    /** Refreshes selected categories — call in onResume so changes from Onboarding are reflected. */
    fun loadSelectedCategories() {
        viewModelScope.launch {
            try {
                val selected = repository.getSelectedCategories()
                _categories.value = selected
                Timber.d("Loaded ${selected.size} selected categories")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load selected categories")
            }
        }
    }

    class Factory(
        private val repository: QuestionRepository,
        private val engine: GamificationEngine,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, engine, networkMonitor) as T
    }
}
