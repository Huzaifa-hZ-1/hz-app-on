package com.hz.appon.onboarding

import com.hz.appon.data.model.Category
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.shared.MainCoroutineRule
import com.hz.appon.shared.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val repo = mockk<QuestionRepository>(relaxed = true)
    private val prefs = mockk<UserPreferences>(relaxed = true)

    private val vm by lazy {
        coEvery { repo.getCategories() } returns listOf(
            Category(9, "General Knowledge"),
            Category(10, "Entertainment: Books")
        )
        OnboardingViewModel(repo, prefs)
    }

    @Test
    fun `continue button disabled when no category selected`() = runTest {
        assertFalse(vm.canContinue.value)
    }

    @Test
    fun `continue button enabled after selecting a category`() = runTest {
        vm.toggleCategory(9)
        assertTrue(vm.canContinue.value)
    }

    @Test
    fun `deselecting last category disables continue`() = runTest {
        vm.toggleCategory(9)
        vm.toggleCategory(9)
        assertFalse(vm.canContinue.value)
    }

    @Test
    fun `saveAndContinue persists selection and marks onboarding done`() = runTest {
        vm.toggleCategory(9)
        vm.saveAndContinue()
        coVerify { repo.saveSelectedCategories(listOf(9)) }
        coVerify { prefs.hasCompletedOnboarding = true }
    }
}
