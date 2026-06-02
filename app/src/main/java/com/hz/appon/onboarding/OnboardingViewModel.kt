package com.hz.appon.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Category
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.shared.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the category selection screen (Onboarding).
 *
 * Used both on first launch and when the user re-opens category selection from Home.
 * Pre-selects previously chosen categories when re-opened.
 */
class OnboardingViewModel(
    private val repository: QuestionRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    /** All available categories with their current selection state. */
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _canContinue = MutableStateFlow(false)
    /** True when at least one category is selected. Drives the Continue button state. */
    val canContinue: StateFlow<Boolean> = _canContinue.asStateFlow()

    /** Emitted once when saving is complete — Activity observes this to navigate away. */
    private val _navigateAway = MutableStateFlow(false)
    val navigateAway: StateFlow<Boolean> = _navigateAway.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val all = repository.getCategories()
                _categories.value = all
                _canContinue.value = all.any { it.isSelected }
                Timber.d("Loaded ${all.size} categories")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load categories")
            }
        }
    }

    /** Toggles the selected state of a category by its ID. */
    fun toggleCategory(categoryId: Int) {
        _categories.value = _categories.value.map { cat ->
            if (cat.id == categoryId) cat.copy(isSelected = !cat.isSelected) else cat
        }
        _canContinue.value = _categories.value.any { it.isSelected }
    }

    /** Persists the selection to the database and marks onboarding as complete. */
    fun saveAndContinue() {
        viewModelScope.launch {
            val selectedIds = _categories.value.filter { it.isSelected }.map { it.id }
            repository.saveSelectedCategories(selectedIds)
            userPreferences.hasCompletedOnboarding = true
            Timber.d("Saved ${selectedIds.size} selected categories")
            _navigateAway.value = true
        }
    }

    class Factory(
        private val repository: QuestionRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(repository, userPreferences) as T
    }
}
