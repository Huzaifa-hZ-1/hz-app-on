# Trivia Quiz — Phase 2: Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build all 5 screens, wire them to Phase 1 foundation, and integrate AdMob.

**Prerequisite:** Phase 1 plan must be complete.

**Architecture:** Each screen is an Activity + ViewModel pair. ViewModels receive deps via a companion `Factory`. AppContainer provides all shared dependencies.

**Tech Stack:** ViewBinding, ViewModel + StateFlow, Coroutines, AdMob (play-services-ads:23.6.0)

---

## File Map

**Create:**
- `app/src/main/java/com/hz/appon/SplashActivity.kt`
- `app/src/main/java/com/hz/appon/onboarding/OnboardingActivity.kt`
- `app/src/main/java/com/hz/appon/onboarding/OnboardingViewModel.kt`
- `app/src/main/java/com/hz/appon/onboarding/CategoryAdapter.kt`
- `app/src/main/java/com/hz/appon/home/HomeActivity.kt`
- `app/src/main/java/com/hz/appon/home/HomeViewModel.kt`
- `app/src/main/java/com/hz/appon/home/CategoryCardAdapter.kt`
- `app/src/main/java/com/hz/appon/quiz/QuizActivity.kt`
- `app/src/main/java/com/hz/appon/quiz/QuizViewModel.kt`
- `app/src/main/java/com/hz/appon/quiz/SessionEndActivity.kt`
- `app/src/main/java/com/hz/appon/quiz/SessionEndViewModel.kt`
- `app/src/main/java/com/hz/appon/ads/AdManager.kt`
- `app/src/main/res/layout/activity_splash.xml`
- `app/src/main/res/layout/activity_onboarding.xml`
- `app/src/main/res/layout/item_category_select.xml`
- `app/src/main/res/layout/activity_home.xml`
- `app/src/main/res/layout/item_category_card.xml`
- `app/src/main/res/layout/activity_quiz.xml`
- `app/src/main/res/layout/activity_session_end.xml`

**Modify:**
- `app/src/main/AndroidManifest.xml` — register all activities, set SplashActivity as launcher
- `app/src/main/res/values/strings.xml` — add all string resources
- `app/src/main/res/values/colors.xml` — add heart red, correct green, wrong red
- `app/src/main/java/com/hz/appon/shared/AppContainer.kt` — add AdManager

**Tests:**
- `app/src/test/java/com/hz/appon/onboarding/OnboardingViewModelTest.kt`
- `app/src/test/java/com/hz/appon/quiz/QuizViewModelTest.kt`

---

## Task 9: Manifest + Splash Screen

- [ ] **Step 1: Add all string resources**

`app/src/main/res/values/strings.xml` — full replacement:
```xml
<resources>
    <string name="app_name">Hz App On</string>

    <!-- Onboarding -->
    <string name="onboarding_title">What are you into?</string>
    <string name="onboarding_subtitle">Pick your categories to get started</string>
    <string name="onboarding_continue">Continue</string>
    <string name="onboarding_select_hint">Select at least 1 category</string>

    <!-- Home -->
    <string name="home_title">Quiz</string>
    <string name="home_offline_banner">You\'re offline — 5 questions available</string>
    <string name="home_hearts_label">Hearts: %d / %d</string>
    <string name="home_tap_to_play">Tap a category to play</string>

    <!-- Quiz -->
    <string name="quiz_question_counter">%d / %d</string>
    <string name="quiz_hearts">♥ %d</string>
    <string name="quiz_time_up">Time\'s up!</string>

    <!-- Session End -->
    <string name="session_end_score">%d / %d</string>
    <string name="session_end_correct">Correct: %d</string>
    <string name="session_end_wrong">Wrong: %d</string>
    <string name="session_end_game_over">You ran out of hearts</string>
    <string name="session_end_watch_ad">Watch ad to restore 1 heart ♥</string>
    <string name="session_end_play_again">Play Again</string>
    <string name="session_end_go_home">Go Home</string>
    <string name="session_end_great_job">Great job!</string>

    <!-- Errors -->
    <string name="error_load_questions">Could not load questions. Please try again.</string>
</resources>
```

- [ ] **Step 2: Add color resources**

Add to `app/src/main/res/values/colors.xml`:
```xml
    <color name="correct_green">#FF4CAF50</color>
    <color name="wrong_red">#FFF44336</color>
    <color name="heart_red">#FFE53935</color>
    <color name="offline_banner_bg">#FFFFA000</color>
    <color name="timer_progress">#FF6200EE</color>
```

- [ ] **Step 3: Create activity_splash.xml**

`app/src/main/res/layout/activity_splash.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorPrimary">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textColor="@color/white"
        android:textSize="36sp"
        android:textStyle="bold"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 4: Create SplashActivity.kt**

`app/src/main/java/com/hz/appon/SplashActivity.kt`
```kotlin
package com.hz.appon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hz.appon.home.HomeActivity
import com.hz.appon.onboarding.OnboardingActivity
import timber.log.Timber

/**
 * Entry point of the app. Routes to Onboarding on first launch, Home on subsequent launches.
 *
 * In Android, an Activity whose intent-filter has ACTION_MAIN + CATEGORY_LAUNCHER is the
 * process entry point. Splash does its work in onCreate and immediately finishes so it
 * never appears in the back stack.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = (application as App).container.userPreferences

        val destination = if (prefs.hasCompletedOnboarding) {
            Timber.d("Onboarding done — routing to Home")
            Intent(this, HomeActivity::class.java)
        } else {
            Timber.d("First launch — routing to Onboarding")
            Intent(this, OnboardingActivity::class.java)
        }

        startActivity(destination)
        // finish() removes Splash from the back stack — pressing Back from Home exits the app
        finish()
    }
}
```

- [ ] **Step 5: Update AndroidManifest.xml**

Full `<application>` block — move the launcher intent-filter to SplashActivity, register all activities:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.HzAppOn">

        <!-- Replace with your real AdMob App ID from admob.google.com -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>

        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.HzAppOn">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".onboarding.OnboardingActivity" android:exported="false" />
        <activity android:name=".home.HomeActivity" android:exported="false" />
        <activity android:name=".quiz.QuizActivity" android:exported="false" />
        <activity android:name=".quiz.SessionEndActivity" android:exported="false" />

    </application>

</manifest>
```

- [ ] **Step 6: Build to verify manifest is valid**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/hz/appon/SplashActivity.kt app/src/main/res/layout/activity_splash.xml app/src/main/res/values/
git commit -m "Add SplashActivity as launcher, register all activities, add string/color resources"
```

---

## Task 10: Onboarding Screen

- [ ] **Step 1: Write failing OnboardingViewModel test**

`app/src/test/java/com/hz/appon/onboarding/OnboardingViewModelTest.kt`
```kotlin
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
```

- [ ] **Step 2: Create MainCoroutineRule.kt** (needed by all ViewModel tests)

`app/src/test/java/com/hz/appon/shared/MainCoroutineRule.kt`
```kotlin
package com.hz.appon.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** JUnit rule that replaces the Main dispatcher with a test dispatcher for ViewModel tests. */
class MainCoroutineRule : TestWatcher() {
    val testDispatcher = UnconfinedTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.onboarding.OnboardingViewModelTest" 2>&1 | tail -5
```
Expected: FAIL

- [ ] **Step 4: Create OnboardingViewModel.kt**

`app/src/main/java/com/hz/appon/onboarding/OnboardingViewModel.kt`
```kotlin
package com.hz.appon.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Category
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.shared.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
```

- [ ] **Step 5: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.onboarding.OnboardingViewModelTest" 2>&1 | tail -5
```
Expected: All tests pass.

- [ ] **Step 6: Create CategoryAdapter.kt**

`app/src/main/java/com/hz/appon/onboarding/CategoryAdapter.kt`
```kotlin
package com.hz.appon.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hz.appon.R
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ItemCategorySelectBinding

/**
 * RecyclerView adapter for the onboarding category selection list.
 *
 * In Android, RecyclerView.Adapter recycles view holders — only the data changes,
 * not the views themselves. ListAdapter uses DiffUtil to compute minimal updates.
 */
class CategoryAdapter(
    private val onToggle: (categoryId: Int) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategorySelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.textCategoryName.text = category.name
            binding.checkboxCategory.isChecked = category.isSelected
            binding.root.setOnClickListener { onToggle(category.id) }
            binding.checkboxCategory.setOnClickListener { onToggle(category.id) }

            val bgColor = if (category.isSelected)
                ContextCompat.getColor(binding.root.context, R.color.purple_200)
            else
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            binding.root.setBackgroundColor(bgColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
        override fun areContentsTheSame(old: Category, new: Category) = old == new
    }
}
```

- [ ] **Step 7: Create item_category_select.xml**

`app/src/main/res/layout/item_category_select.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:clickable="true"
    android:focusable="true">

    <CheckBox
        android:id="@+id/checkboxCategory"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_vertical"
        android:clickable="false"
        android:focusable="false" />

    <TextView
        android:id="@+id/textCategoryName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_gravity="center_vertical"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:textSize="16sp" />

</LinearLayout>
```

- [ ] **Step 8: Create activity_onboarding.xml**

`app/src/main/res/layout/activity_onboarding.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="24dp"
        android:text="@string/onboarding_title"
        android:textSize="28sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="24dp"
        android:layout_marginBottom="16dp"
        android:text="@string/onboarding_subtitle"
        android:textSize="16sp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerCategories"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <TextView
        android:id="@+id/textSelectHint"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:padding="8dp"
        android:text="@string/onboarding_select_hint"
        android:textSize="13sp"
        android:textColor="?attr/colorPrimary" />

    <Button
        android:id="@+id/btnContinue"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        android:enabled="false"
        android:text="@string/onboarding_continue" />

</LinearLayout>
```

- [ ] **Step 9: Create OnboardingActivity.kt**

`app/src/main/java/com/hz/appon/onboarding/OnboardingActivity.kt`
```kotlin
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
```

- [ ] **Step 10: Build and verify**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/hz/appon/onboarding/ app/src/main/res/layout/activity_onboarding.xml app/src/main/res/layout/item_category_select.xml app/src/test/java/com/hz/appon/onboarding/ app/src/test/java/com/hz/appon/shared/MainCoroutineRule.kt
git commit -m "Add Onboarding screen: category selection with TDD (OnboardingViewModel)"
```

---

## Task 11: Home Screen

- [ ] **Step 1: Create HomeViewModel.kt**

`app/src/main/java/com/hz/appon/home/HomeViewModel.kt`
```kotlin
package com.hz.appon.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Category
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.NetworkMonitor
import kotlinx.coroutines.Dispatchers
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

    /** Current lives state — reflects any hearts gained/lost since last session. */
    val livesState: StateFlow<LivesState>
        get() = MutableStateFlow(gamificationEngine.gameState.value.lives).also { flow ->
            viewModelScope.launch {
                gamificationEngine.gameState.collect { flow.value = it.lives }
            }
        }

    init {
        loadSelectedCategories()
    }

    /** Refreshes selected categories — call in onResume so changes from Onboarding are reflected. */
    fun loadSelectedCategories() {
        viewModelScope.launch(Dispatchers.IO) {
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
```

- [ ] **Step 2: Create CategoryCardAdapter.kt**

`app/src/main/java/com/hz/appon/home/CategoryCardAdapter.kt`
```kotlin
package com.hz.appon.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hz.appon.data.model.Category
import com.hz.appon.databinding.ItemCategoryCardBinding

/** Adapter for the home screen category cards. Each card starts a quiz on tap. */
class CategoryCardAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryCardAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemCategoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.textCategoryName.text = category.name
            binding.root.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
        override fun areContentsTheSame(old: Category, new: Category) = old == new
    }
}
```

- [ ] **Step 3: Create item_category_card.xml**

`app/src/main/res/layout/item_category_card.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp">

    <TextView
        android:id="@+id/textCategoryName"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:padding="24dp"
        android:textSize="18sp"
        android:textStyle="bold" />

</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 4: Create activity_home.xml**

`app/src/main/res/layout/activity_home.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- Offline banner — hidden when online -->
    <TextView
        android:id="@+id/bannerOffline"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/offline_banner_bg"
        android:gravity="center"
        android:padding="8dp"
        android:text="@string/home_offline_banner"
        android:textColor="@color/white"
        android:textStyle="bold"
        android:visibility="gone" />

    <!-- Toolbar row: title + hearts + edit icon -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/home_title"
            android:textSize="24sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/textHearts"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_vertical"
            android:layout_marginEnd="12dp"
            android:textColor="@color/heart_red"
            android:textSize="16sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/btnEdit"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:contentDescription="Edit categories"
            android:src="@android:drawable/ic_menu_edit" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerCategories"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="8dp" />

    <!-- AdMob banner container -->
    <FrameLayout
        android:id="@+id/adBannerContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

</LinearLayout>
```

- [ ] **Step 5: Create HomeActivity.kt**

`app/src/main/java/com/hz/appon/home/HomeActivity.kt`
```kotlin
package com.hz.appon.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hz.appon.App
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
                    com.hz.appon.R.string.home_hearts_label, lives.current, lives.max
                )
            }
        }
    }
}
```

- [ ] **Step 6: Add AdManager to AppContainer**

Update `AppContainer.kt` — add this property:
```kotlin
    val adManager = AdManager()  // AdManager created in Task 14 — add this after that task
```
*(Skip for now — wire in Task 14)*

- [ ] **Step 7: Build and verify**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/hz/appon/home/ app/src/main/res/layout/activity_home.xml app/src/main/res/layout/item_category_card.xml
git commit -m "Add Home screen: category cards, hearts display, offline banner"
```

---

## Task 12: Quiz Screen

- [ ] **Step 1: Write failing QuizViewModel test**

`app/src/test/java/com/hz/appon/quiz/QuizViewModelTest.kt`
```kotlin
package com.hz.appon.quiz

import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.model.QuizSession
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.GameState
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.MainCoroutineRule
import com.hz.appon.shared.NetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuizViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val repo = mockk<QuestionRepository>(relaxed = true)
    private val engine = mockk<GamificationEngine>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private fun makeVm(): QuizViewModel {
        val gameState = MutableStateFlow(GameState(lives = LivesState(current = 5)))
        coEvery { engine.gameState } returns gameState
        coEvery { networkMonitor.isOnline } returns MutableStateFlow(true)
        coEvery { repo.getQuestions(any(), Difficulty.EASY, any()) } returns makeQuestions(4, Difficulty.EASY)
        coEvery { repo.getQuestions(any(), Difficulty.MEDIUM, any()) } returns makeQuestions(3, Difficulty.MEDIUM)
        coEvery { repo.getQuestions(any(), Difficulty.HARD, any()) } returns makeQuestions(3, Difficulty.HARD)
        return QuizViewModel(repo, engine, networkMonitor)
    }

    @Test
    fun `startSession emits Question state with 10 questions`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        val state = vm.uiState.value
        assertTrue(state is QuizViewModel.UiState.Question)
        assertEquals(10, (state as QuizViewModel.UiState.Question).session.totalQuestions)
    }

    @Test
    fun `correct answer fires CorrectAnswer event`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        val state = vm.uiState.value as QuizViewModel.UiState.Question
        vm.submitAnswer(state.currentQuestion.correctAnswer)
        verify { engine.onEvent(GameEvent.CorrectAnswer) }
    }

    @Test
    fun `wrong answer fires WrongAnswer event`() = runTest {
        val vm = makeVm()
        vm.startSession(9)
        vm.submitAnswer("definitely_wrong")
        verify { engine.onEvent(GameEvent.WrongAnswer) }
    }

    @Test
    fun `offline session uses bundled questions`() = runTest {
        coEvery { networkMonitor.isOnline } returns MutableStateFlow(false)
        val bundled = makeQuestions(5, Difficulty.EASY, bundled = true)
        coEvery { repo.getBundledQuestions() } returns bundled
        val gameState = MutableStateFlow(GameState(lives = LivesState(current = 5)))
        coEvery { engine.gameState } returns gameState
        val vm = QuizViewModel(repo, engine, networkMonitor)
        vm.startSession(9)
        val state = vm.uiState.value as QuizViewModel.UiState.Question
        assertTrue(state.session.isOffline)
    }

    private fun makeQuestions(n: Int, d: Difficulty, bundled: Boolean = false) = List(n) { i ->
        Question("q$i", 9, "Question $i", listOf("A", "B", "C", "D"), "A", d, bundled)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.quiz.QuizViewModelTest" 2>&1 | tail -5
```
Expected: FAIL

- [ ] **Step 3: Create QuizViewModel.kt**

`app/src/main/java/com/hz/appon/quiz/QuizViewModel.kt`
```kotlin
package com.hz.appon.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
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

    /** Loads questions and starts the session for the given [categoryId]. */
    fun startSession(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            engine.onEvent(GameEvent.SessionStarted)
            try {
                val isOffline = !networkMonitor.isOnline.value
                val questions = if (isOffline) {
                    Timber.d("Offline — loading bundled questions")
                    repository.getBundledQuestions()
                } else {
                    Timber.d("Online — loading 10 questions for category $categoryId")
                    repository.getQuestions(categoryId, Difficulty.EASY, 4) +
                    repository.getQuestions(categoryId, Difficulty.MEDIUM, 3) +
                    repository.getQuestions(categoryId, Difficulty.HARD, 3)
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
            submitAnswer("") // Empty = timeout = wrong answer
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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.quiz.QuizViewModelTest" 2>&1 | tail -5
```
Expected: All tests pass.

- [ ] **Step 5: Create activity_quiz.xml**

`app/src/main/res/layout/activity_quiz.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Header: hearts | counter -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/textHearts"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@color/heart_red"
            android:textSize="18sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/textCategory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp" />

        <TextView
            android:id="@+id/textCounter"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:textSize="14sp" />
    </LinearLayout>

    <!-- Timer progress bar -->
    <ProgressBar
        android:id="@+id/progressTimer"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="8dp"
        android:layout_marginTop="8dp"
        android:max="15"
        android:progress="15"
        android:progressTint="@color/timer_progress" />

    <!-- Question text -->
    <TextView
        android:id="@+id/textQuestion"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:gravity="center"
        android:padding="16dp"
        android:textSize="20sp"
        android:textStyle="bold" />

    <!-- Answer buttons -->
    <Button
        android:id="@+id/btnAnswer0"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

    <Button
        android:id="@+id/btnAnswer1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

    <Button
        android:id="@+id/btnAnswer2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

    <Button
        android:id="@+id/btnAnswer3"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

</LinearLayout>
```

- [ ] **Step 6: Create QuizActivity.kt**

`app/src/main/java/com/hz/appon/quiz/QuizActivity.kt`
```kotlin
package com.hz.appon.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        // Reset button colours and text
        answerButtons.forEachIndexed { i, btn ->
            btn.text = if (i < q.options.size) q.options[i] else ""
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        }

        // Show answer feedback if an answer was selected
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
```

- [ ] **Step 7: Build and run tests**

```bash
make test && make build
```
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/hz/appon/quiz/QuizActivity.kt app/src/main/java/com/hz/appon/quiz/QuizViewModel.kt app/src/main/res/layout/activity_quiz.xml app/src/test/java/com/hz/appon/quiz/QuizViewModelTest.kt
git commit -m "Add Quiz screen: questions, timer, answer feedback, game over detection (TDD)"
```

---

## Task 13: Session End Screen

- [ ] **Step 1: Create SessionEndViewModel.kt**

`app/src/main/java/com/hz/appon/quiz/SessionEndViewModel.kt`
```kotlin
package com.hz.appon.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.LivesState
import com.hz.appon.gamification.lives.LivesModule
import com.hz.appon.shared.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the session end screen.
 *
 * Increments the session counter on creation (drives interstitial ad cadence).
 * Exposes whether to show the "Watch ad → restore heart" button.
 */
class SessionEndViewModel(
    private val engine: GamificationEngine,
    private val livesModule: LivesModule,
    private val userPreferences: UserPreferences
) : ViewModel() {

    /** True when player has < max hearts and should be offered the rewarded ad. */
    val canRestoreHeart: StateFlow<Boolean>
        get() = MutableStateFlow(engine.gameState.value.lives.current < engine.gameState.value.lives.max)

    /** Current lives state for display. */
    val livesState: LivesState get() = engine.gameState.value.lives

    /**
     * Whether an interstitial ad should be shown.
     * True every 3rd completed session.
     */
    val shouldShowInterstitial: Boolean
        get() = userPreferences.sessionsPlayedCount % 3 == 0 &&
                userPreferences.sessionsPlayedCount > 0

    init {
        userPreferences.sessionsPlayedCount += 1
        Timber.d("Session count: ${userPreferences.sessionsPlayedCount}")
    }

    /**
     * Adds one heart after a rewarded ad completes successfully.
     * Call this from the Activity's rewarded ad callback.
     */
    fun onRewardedAdComplete() {
        livesModule.addHeart()
        Timber.d("Heart restored via rewarded ad. Lives: ${engine.gameState.value.lives.current}")
    }

    class Factory(
        private val engine: GamificationEngine,
        private val livesModule: LivesModule,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionEndViewModel(engine, livesModule, userPreferences) as T
    }
}
```

- [ ] **Step 2: Create activity_session_end.xml**

`app/src/main/res/layout/activity_session_end.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:id="@+id/textResult"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="28sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp"/>

    <TextView
        android:id="@+id/textScore"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="48sp"
        android:textStyle="bold"
        android:textColor="?attr/colorPrimary"
        android:layout_marginBottom="8dp"/>

    <TextView
        android:id="@+id/textCorrect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:layout_marginBottom="4dp"/>

    <TextView
        android:id="@+id/textWrong"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:layout_marginBottom="32dp"/>

    <Button
        android:id="@+id/btnWatchAd"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:text="@string/session_end_watch_ad"
        android:visibility="gone" />

    <Button
        android:id="@+id/btnPlayAgain"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:text="@string/session_end_play_again" />

    <Button
        android:id="@+id/btnGoHome"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/session_end_go_home"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"/>

</LinearLayout>
```

- [ ] **Step 3: Create SessionEndActivity.kt**

`app/src/main/java/com/hz/appon/quiz/SessionEndActivity.kt`
```kotlin
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
```

- [ ] **Step 4: Build and verify**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hz/appon/quiz/SessionEndActivity.kt app/src/main/java/com/hz/appon/quiz/SessionEndViewModel.kt app/src/main/res/layout/activity_session_end.xml
git commit -m "Add Session End screen: score display, rewarded ad hook, interstitial trigger"
```

---

## Task 14: AdManager + Final Wiring

- [ ] **Step 1: Create AdManager.kt**

`app/src/main/java/com/hz/appon/ads/AdManager.kt`
```kotlin
package com.hz.appon.ads

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import timber.log.Timber

/**
 * Central manager for all AdMob ad types: banner, interstitial, and rewarded.
 *
 * Uses Google's public test ad unit IDs — replace with real IDs from admob.google.com
 * before publishing. Real IDs belong in a build config or remote config, not hardcoded here.
 *
 * Android note: AdMob initialisation is asynchronous. Ads should be pre-loaded
 * before they're needed — done here in [init] and [preloadInterstitial].
 */
class AdManager(context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        MobileAds.initialize(context) {
            Timber.d("AdMob initialised")
            preloadInterstitial(context)
            preloadRewarded(context)
        }
    }

    /**
     * Inflates a banner ad into [container].
     * Call from Activity.onCreate — the banner remains for the Activity's lifetime.
     */
    fun loadBanner(container: FrameLayout) {
        val adView = AdView(container.context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = TEST_BANNER_ID
        }
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
        Timber.d("Banner ad loaded")
    }

    /**
     * Shows an interstitial ad if one is loaded. Pre-loads the next one after display.
     * Safe to call even if no ad is ready — it silently skips.
     */
    fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            Timber.w("Interstitial not ready — skipping")
            preloadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
            }
        }
        ad.show(activity)
        Timber.d("Interstitial shown")
    }

    /**
     * Shows a rewarded ad. Calls [onRewarded] only if the user earns the reward.
     * Safe to call if no ad is ready — it silently skips and pre-loads.
     */
    fun showRewarded(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Timber.w("Rewarded ad not ready — skipping")
            preloadRewarded(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewarded(activity)
            }
        }
        ad.show(activity) { _ ->
            Timber.d("Rewarded ad — reward earned")
            onRewarded()
        }
    }

    private fun preloadInterstitial(context: Context) {
        InterstitialAd.load(context, TEST_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Timber.d("Interstitial pre-loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Interstitial failed to load: ${error.message}")
                }
            })
    }

    private fun preloadRewarded(context: Context) {
        RewardedAd.load(context, TEST_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Timber.d("Rewarded ad pre-loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: ${error.message}")
                }
            })
    }

    companion object {
        // Google's public test IDs — safe to use during development, never earn real revenue
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
```

- [ ] **Step 2: Add AdManager to AppContainer**

In `AppContainer.kt`, add as the last property:
```kotlin
    val adManager = AdManager(context)
```
Also update the constructor to pass `context` to AdManager — `context` is already available as the constructor parameter.

- [ ] **Step 3: Delete MainActivity.kt** (replaced by SplashActivity)

```bash
git rm app/src/main/java/com/hz/appon/MainActivity.kt
git rm app/src/main/res/layout/activity_main.xml
git rm app/src/test/java/com/hz/appon/MainActivityTest.kt
```

- [ ] **Step 4: Run full test suite**

```bash
make test
```
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Run on emulator**

```bash
make run
```
Expected: App launches → Splash → Onboarding (first time) or Home (returning). All screens navigate correctly.

- [ ] **Step 6: Final commit**

```bash
git add app/src/main/java/com/hz/appon/ads/ app/src/main/java/com/hz/appon/shared/AppContainer.kt
git commit -m "Add AdManager (banner/interstitial/rewarded), wire all screens — Phase 2 complete"
```

---

## Self-Review

**Spec coverage check:**
- ✅ Splash routing (onboarding vs home)
- ✅ Onboarding: category selection, ≥1 required, re-entry pre-checks
- ✅ Home: cards, hearts, offline banner, edit icon
- ✅ Quiz: 10 questions (4/3/3), timer, feedback colours, game over on hearts=0
- ✅ Offline: 5 bundled questions, same flow
- ✅ Session End: score, game-over message, rewarded ad, interstitial every 3rd
- ✅ AdMob: banner (home), interstitial (session end), rewarded (restore heart)
- ✅ GamificationEngine extensible: modules list, fan-out pattern
- ✅ KDocs on all public/internal APIs
- ✅ Timber logging at all feature boundaries

**Type consistency check:**
- `QuizViewModel.UiState.Question.session` is `QuizSession` — matches `QuizSession` defined in Task 2 ✅
- `LivesModule.addHeart()` called in `SessionEndViewModel.onRewardedAdComplete()` ✅
- `AppContainer.livesModule_` exposes `LivesModule` for `SessionEndViewModel.Factory` ✅
- `AdManager` constructor takes `Context` — `AppContainer` passes `context` ✅

**Placeholder scan:** None found.

---

## Phase 2 Complete

Full app buildable and runnable. All screens connected. Ad placements wired.
