# Trivia Quiz — Phase 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the data, network, repository, and gamification layers — fully unit-tested, no UI.

**Architecture:** MVVM with manual DI via AppContainer. GamificationEngine fans out GameEvents to pluggable GamificationModules. QuestionRepository routes online (OpenTDB → Room cache) or offline (bundled 5 questions) transparently.

**Tech Stack:** Room 2.6.1 (KSP), Retrofit 2.11.0 + Gson, Coroutines 1.8.1, Timber 5.0.1, JUnit 4, MockK 1.13.13

---

## File Map

**Create:**
- `app/build.gradle.kts` — add KSP, Room, Retrofit, Coroutines, ViewModel, RecyclerView
- `build.gradle.kts` — add KSP plugin
- `app/src/main/java/com/hz/appon/data/model/Difficulty.kt`
- `app/src/main/java/com/hz/appon/data/model/Category.kt`
- `app/src/main/java/com/hz/appon/data/model/Question.kt`
- `app/src/main/java/com/hz/appon/data/model/QuizSession.kt`
- `app/src/main/java/com/hz/appon/data/local/Converters.kt`
- `app/src/main/java/com/hz/appon/data/local/CategoryDao.kt`
- `app/src/main/java/com/hz/appon/data/local/QuestionDao.kt`
- `app/src/main/java/com/hz/appon/data/local/QuizDatabase.kt`
- `app/src/main/java/com/hz/appon/data/remote/OpenTdbResponse.kt`
- `app/src/main/java/com/hz/appon/data/remote/OpenTdbApi.kt`
- `app/src/main/java/com/hz/appon/data/repository/QuestionRepository.kt`
- `app/src/main/java/com/hz/appon/data/repository/QuestionRepositoryImpl.kt`
- `app/src/main/java/com/hz/appon/shared/UserPreferences.kt`
- `app/src/main/java/com/hz/appon/shared/NetworkMonitor.kt`
- `app/src/main/java/com/hz/appon/shared/AppContainer.kt`
- `app/src/main/java/com/hz/appon/gamification/GamificationModule.kt`
- `app/src/main/java/com/hz/appon/gamification/GameEvent.kt`
- `app/src/main/java/com/hz/appon/gamification/GameState.kt`
- `app/src/main/java/com/hz/appon/gamification/GamificationEngine.kt`
- `app/src/main/java/com/hz/appon/gamification/lives/LivesModule.kt`

**Modify:**
- `app/src/main/java/com/hz/appon/App.kt` — add AppContainer init
- `app/src/main/AndroidManifest.xml` — add INTERNET permission

**Tests:**
- `app/src/test/java/com/hz/appon/shared/MainCoroutineRule.kt`
- `app/src/test/java/com/hz/appon/gamification/GamificationEngineTest.kt`
- `app/src/test/java/com/hz/appon/gamification/lives/LivesModuleTest.kt`
- `app/src/test/java/com/hz/appon/data/repository/QuestionRepositoryImplTest.kt`

---

## Task 1: Build Setup

**Files:** `build.gradle.kts` (root), `app/build.gradle.kts`

- [ ] **Step 1: Add KSP plugin to root build.gradle.kts**

```kotlin
// build.gradle.kts (root) — full file
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.sonarqube") version "6.0.1.5171" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

- [ ] **Step 2: Add all dependencies to app/build.gradle.kts**

Replace the entire `plugins` block and add dependencies:

```kotlin
// app/build.gradle.kts — plugins block
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.sonarqube")
    id("com.google.devtools.ksp")
}
```

Add to `dependencies {}`:
```kotlin
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Test coroutines + arch
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.13")
```

- [ ] **Step 3: Verify build compiles**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts app/build.gradle.kts
git commit -m "Add KSP, Room, Retrofit, Coroutines, ViewModel dependencies"
```

---

## Task 2: Data Models

**Files:** `data/model/Difficulty.kt`, `Category.kt`, `Question.kt`, `QuizSession.kt`, `GameState.kt`

- [ ] **Step 1: Create Difficulty.kt**

`app/src/main/java/com/hz/appon/data/model/Difficulty.kt`
```kotlin
package com.hz.appon.data.model

/** Trivia question difficulty, maps directly to OpenTDB difficulty strings. */
enum class Difficulty {
    EASY, MEDIUM, HARD;

    companion object {
        /** Parses OpenTDB difficulty string; defaults to [EASY] on unknown value. */
        fun from(value: String): Difficulty = when (value.lowercase()) {
            "medium" -> MEDIUM
            "hard" -> HARD
            else -> EASY
        }
    }
}
```

- [ ] **Step 2: Create Category.kt**

`app/src/main/java/com/hz/appon/data/model/Category.kt`
```kotlin
package com.hz.appon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A trivia category fetched from OpenTDB and persisted locally.
 *
 * @param id OpenTDB numeric category ID (9–32)
 * @param name Human-readable category name
 * @param isSelected Whether the user has selected this category during onboarding
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Int,
    val name: String,
    val isSelected: Boolean = false
)
```

- [ ] **Step 3: Create Question.kt**

`app/src/main/java/com/hz/appon/data/model/Question.kt`
```kotlin
package com.hz.appon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single trivia question with pre-shuffled answer options.
 *
 * @param id Stable ID formatted as "{categoryId}_{difficulty}_{index}" or "bundled_{n}"
 * @param options All 4 answer options already shuffled — index of correct answer is not fixed
 * @param isBundled True for the 5 hardcoded offline questions
 */
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: String,
    val categoryId: Int,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
    val difficulty: Difficulty,
    val isBundled: Boolean = false
)
```

- [ ] **Step 4: Create QuizSession.kt**

`app/src/main/java/com/hz/appon/data/model/QuizSession.kt`
```kotlin
package com.hz.appon.data.model

/**
 * In-memory state of an active quiz session. Not persisted to Room.
 *
 * @param questions Ordered list: 4 easy → 3 medium → 3 hard (or 5 bundled if offline)
 */
data class QuizSession(
    val categoryId: Int,
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val isOffline: Boolean = false
) {
    val isComplete: Boolean get() = currentIndex >= questions.size
    val totalQuestions: Int get() = questions.size
}
```

- [ ] **Step 5: Create GameState.kt**

`app/src/main/java/com/hz/appon/gamification/GameState.kt`
```kotlin
package com.hz.appon.gamification

/**
 * Snapshot of the player's gamification state.
 * Slots for [xp] and [streak] are null until Options B and C are implemented —
 * adding them requires zero changes to this class or existing modules.
 */
data class GameState(
    val lives: LivesState,
    val xp: Any? = null,      // Reserved for Option B (XpState)
    val streak: Any? = null   // Reserved for Option C (StreakState)
)

/**
 * Hearts/lives state persisted between sessions.
 *
 * @param current Number of hearts currently available (0–[max])
 * @param lastLostAt Epoch ms when the last heart was lost; used to calculate refill
 */
data class LivesState(
    val current: Int,
    val max: Int = 5,
    val lastLostAt: Long? = null
)
```

- [ ] **Step 6: Run existing tests to confirm no regressions**

```bash
make test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/hz/appon/data/model/ app/src/main/java/com/hz/appon/gamification/GameState.kt
git commit -m "Add data models: Category, Question, Difficulty, QuizSession, GameState"
```

---

## Task 3: Room Database

**Files:** `Converters.kt`, `CategoryDao.kt`, `QuestionDao.kt`, `QuizDatabase.kt`

- [ ] **Step 1: Create Converters.kt**

`app/src/main/java/com/hz/appon/data/local/Converters.kt`
```kotlin
package com.hz.appon.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.hz.appon.data.model.Difficulty

/**
 * Room TypeConverters for non-primitive types.
 * Room cannot store List<String> or enums natively — these converters handle serialisation.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
}
```

- [ ] **Step 2: Create CategoryDao.kt**

`app/src/main/java/com/hz/appon/data/local/CategoryDao.kt`
```kotlin
package com.hz.appon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hz.appon.data.model.Category

/** Database access for trivia categories. */
@Dao
interface CategoryDao {

    /** Returns all categories, selected ones first. */
    @Query("SELECT * FROM categories ORDER BY isSelected DESC, name ASC")
    suspend fun getAll(): List<Category>

    /** Returns only categories the user has selected. */
    @Query("SELECT * FROM categories WHERE isSelected = 1")
    suspend fun getSelected(): List<Category>

    /** Inserts or replaces categories (used on first fetch from OpenTDB). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    /** Marks a single category as selected or deselected. */
    @Query("UPDATE categories SET isSelected = :selected WHERE id = :id")
    suspend fun setSelected(id: Int, selected: Boolean)

    /** Replaces the entire selection — deselects all, then selects given IDs. */
    @Query("UPDATE categories SET isSelected = 0")
    suspend fun clearAllSelections()
}
```

- [ ] **Step 3: Create QuestionDao.kt**

`app/src/main/java/com/hz/appon/data/local/QuestionDao.kt`
```kotlin
package com.hz.appon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question

/** Database access for trivia questions. */
@Dao
interface QuestionDao {

    /**
     * Returns up to [limit] cached questions for a category and difficulty.
     * Results are randomised so repeated calls return different question sets.
     */
    @Query(
        "SELECT * FROM questions WHERE categoryId = :categoryId " +
        "AND difficulty = :difficulty AND isBundled = 0 " +
        "ORDER BY RANDOM() LIMIT :limit"
    )
    suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, limit: Int): List<Question>

    /** Returns all 5 bundled offline questions. */
    @Query("SELECT * FROM questions WHERE isBundled = 1")
    suspend fun getBundled(): List<Question>

    /** Inserts or replaces questions (used after fetching from OpenTDB). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    /** Returns the number of cached questions for a category/difficulty. */
    @Query(
        "SELECT COUNT(*) FROM questions WHERE categoryId = :categoryId " +
        "AND difficulty = :difficulty AND isBundled = 0"
    )
    suspend fun countQuestions(categoryId: Int, difficulty: Difficulty): Int
}
```

- [ ] **Step 4: Create QuizDatabase.kt**

`app/src/main/java/com/hz/appon/data/local/QuizDatabase.kt`
```kotlin
package com.hz.appon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Question

/**
 * Single Room database for the app.
 *
 * Increment [version] and provide a Migration whenever the schema changes.
 * Never use `fallbackToDestructiveMigration` in production.
 */
@Database(entities = [Category::class, Question::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile private var INSTANCE: QuizDatabase? = null

        /** Returns the singleton database instance, creating it if necessary. */
        fun getInstance(context: Context): QuizDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database"
                ).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 5: Verify build compiles**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hz/appon/data/local/
git commit -m "Add Room database: QuizDatabase, CategoryDao, QuestionDao, Converters"
```

---

## Task 4: Network Layer

**Files:** `OpenTdbResponse.kt`, `OpenTdbApi.kt`

- [ ] **Step 1: Create OpenTdbResponse.kt**

`app/src/main/java/com/hz/appon/data/remote/OpenTdbResponse.kt`
```kotlin
package com.hz.appon.data.remote

import com.google.gson.annotations.SerializedName

/** Root response for GET /api_category.php */
data class CategoriesResponse(
    @SerializedName("trivia_categories") val categories: List<CategoryDto>
)

data class CategoryDto(
    val id: Int,
    val name: String
)

/** Root response for GET /api.php */
data class QuestionsResponse(
    @SerializedName("response_code") val responseCode: Int,
    val results: List<QuestionDto>
)

/**
 * Raw question from OpenTDB.
 * HTML encoding in [question], [correctAnswer], and [incorrectAnswers] must be decoded
 * before storing — handled in [QuestionRepositoryImpl].
 */
data class QuestionDto(
    val type: String,
    val difficulty: String,
    val category: String,
    val question: String,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("incorrect_answers") val incorrectAnswers: List<String>
)
```

- [ ] **Step 2: Create OpenTdbApi.kt**

`app/src/main/java/com/hz/appon/data/remote/OpenTdbApi.kt`
```kotlin
package com.hz.appon.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit interface for the Open Trivia Database API. */
interface OpenTdbApi {

    /**
     * Fetches all available trivia categories.
     * Call once on first launch to populate the local category list.
     */
    @GET("api_category.php")
    suspend fun getCategories(): CategoriesResponse

    /**
     * Fetches questions for a specific category and difficulty.
     *
     * @param amount Number of questions to fetch (max 50 per request)
     * @param categoryId OpenTDB category ID
     * @param difficulty "easy", "medium", or "hard"
     * @param type Always "multiple" — we only use 4-option questions
     */
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int,
        @Query("category") categoryId: Int,
        @Query("difficulty") difficulty: String,
        @Query("type") type: String = "multiple"
    ): QuestionsResponse

    companion object {
        private const val BASE_URL = "https://opentdb.com/"

        /** Creates a configured [OpenTdbApi] instance. */
        fun create(): OpenTdbApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenTdbApi::class.java)
    }
}
```

- [ ] **Step 3: Add INTERNET permission to AndroidManifest.xml**

Add inside `<manifest>` before `<application>`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

- [ ] **Step 4: Verify build**

```bash
make build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hz/appon/data/remote/ app/src/main/AndroidManifest.xml
git commit -m "Add OpenTDB Retrofit API client and network permissions"
```

---

## Task 5: Shared Utilities

**Files:** `UserPreferences.kt`, `NetworkMonitor.kt`

- [ ] **Step 1: Create UserPreferences.kt**

`app/src/main/java/com/hz/appon/shared/UserPreferences.kt`
```kotlin
package com.hz.appon.shared

import android.content.Context
import com.google.gson.Gson
import com.hz.appon.gamification.LivesState

/**
 * Lightweight wrapper around SharedPreferences.
 * Persists user settings and game state that must survive process death.
 */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** True after the user has completed category selection on first launch. */
    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /**
     * Total number of sessions completed (win or loss).
     * Drives the interstitial ad trigger — shown every 3rd session.
     */
    var sessionsPlayedCount: Int
        get() = prefs.getInt(KEY_SESSIONS, 0)
        set(value) = prefs.edit().putInt(KEY_SESSIONS, value).apply()

    /**
     * Persisted lives state so hearts don't refill by restarting the app.
     * Defaults to full hearts on first launch.
     */
    var livesState: LivesState
        get() {
            val json = prefs.getString(KEY_LIVES, null) ?: return LivesState(current = 5)
            return gson.fromJson(json, LivesState::class.java)
        }
        set(value) = prefs.edit().putString(KEY_LIVES, gson.toJson(value)).apply()

    companion object {
        private const val PREFS_NAME = "hz_app_prefs"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_SESSIONS = "sessions_count"
        private const val KEY_LIVES = "lives_state"
    }
}
```

- [ ] **Step 2: Create NetworkMonitor.kt**

`app/src/main/java/com/hz/appon/shared/NetworkMonitor.kt`
```kotlin
package com.hz.appon.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Observes network connectivity and exposes it as a [StateFlow].
 *
 * Call [register] in Application.onCreate and [unregister] if needed.
 * Activities and ViewModels collect [isOnline] to react to connectivity changes.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    /** True when the device has an active internet-capable network connection. */
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available: $network")
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            Timber.d("Network lost: $network")
            _isOnline.value = checkCurrentConnectivity()
        }
    }

    /** Registers the network callback. Call once from [App.onCreate]. */
    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        Timber.d("NetworkMonitor registered, online=${_isOnline.value}")
    }

    /** Unregisters the network callback. Call when monitoring is no longer needed. */
    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/hz/appon/shared/UserPreferences.kt app/src/main/java/com/hz/appon/shared/NetworkMonitor.kt
git commit -m "Add UserPreferences and NetworkMonitor shared utilities"
```

---

## Task 6: QuestionRepository

**Files:** `QuestionRepository.kt`, `QuestionRepositoryImpl.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/hz/appon/data/repository/QuestionRepositoryImplTest.kt`
```kotlin
package com.hz.appon.data.repository

import com.hz.appon.data.local.CategoryDao
import com.hz.appon.data.local.QuestionDao
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.remote.OpenTdbApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionRepositoryImplTest {

    private val questionDao = mockk<QuestionDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val api = mockk<OpenTdbApi>(relaxed = true)

    private val repo = QuestionRepositoryImpl(questionDao, categoryDao, api)

    @Test
    fun `getBundledQuestions always returns 5 questions`() = runTest {
        coEvery { questionDao.getBundled() } returns emptyList()
        val result = repo.getBundledQuestions()
        assertEquals(5, result.size)
    }

    @Test
    fun `getBundledQuestions returns only isBundled questions`() = runTest {
        coEvery { questionDao.getBundled() } returns emptyList()
        val result = repo.getBundledQuestions()
        assertTrue(result.all { it.isBundled })
    }

    @Test
    fun `getQuestions fetches from API when cache is empty`() = runTest {
        coEvery { questionDao.countQuestions(9, Difficulty.EASY) } returns 0
        coEvery { api.getQuestions(any(), any(), any(), any()) } returns
            com.hz.appon.data.remote.QuestionsResponse(responseCode = 0, results = emptyList())
        coEvery { questionDao.getQuestions(9, Difficulty.EASY, 4) } returns emptyList()

        repo.getQuestions(9, Difficulty.EASY, 4)

        coVerify { api.getQuestions(any(), 9, "easy", any()) }
    }

    @Test
    fun `getQuestions returns from cache when sufficient questions exist`() = runTest {
        val cached = List(10) { makeQuestion(it) }
        coEvery { questionDao.countQuestions(9, Difficulty.EASY) } returns 10
        coEvery { questionDao.getQuestions(9, Difficulty.EASY, 4) } returns cached.take(4)

        val result = repo.getQuestions(9, Difficulty.EASY, 4)

        assertEquals(4, result.size)
        coVerify(exactly = 0) { api.getQuestions(any(), any(), any(), any()) }
    }

    private fun makeQuestion(i: Int) = Question(
        id = "9_EASY_$i", categoryId = 9, text = "Q$i",
        options = listOf("A", "B", "C", "D"), correctAnswer = "A",
        difficulty = Difficulty.EASY
    )
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.data.repository.QuestionRepositoryImplTest" 2>&1 | tail -8
```
Expected: FAIL — `QuestionRepositoryImpl` does not exist yet.

- [ ] **Step 3: Create QuestionRepository.kt**

`app/src/main/java/com/hz/appon/data/repository/QuestionRepository.kt`
```kotlin
package com.hz.appon.data.repository

import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question

/**
 * Contract for accessing trivia questions and categories.
 * Callers never know whether data comes from the network, cache, or bundled fallback.
 */
interface QuestionRepository {

    /** Returns all available categories, fetching from OpenTDB if the local list is empty. */
    suspend fun getCategories(): List<Category>

    /**
     * Returns [count] questions for the given [categoryId] and [difficulty].
     * Fetches from OpenTDB and caches locally if the cache has fewer than [count] entries.
     */
    suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, count: Int): List<Question>

    /**
     * Returns the 5 hardcoded offline questions. Always available regardless of connectivity.
     * Used when the device has no internet connection.
     */
    suspend fun getBundledQuestions(): List<Question>

    /** Saves the user's selected category IDs, replacing the previous selection. */
    suspend fun saveSelectedCategories(ids: List<Int>)

    /** Returns the categories the user has selected during onboarding. */
    suspend fun getSelectedCategories(): List<Category>
}
```

- [ ] **Step 4: Create QuestionRepositoryImpl.kt**

`app/src/main/java/com/hz/appon/data/repository/QuestionRepositoryImpl.kt`
```kotlin
package com.hz.appon.data.repository

import android.text.Html
import com.hz.appon.data.local.CategoryDao
import com.hz.appon.data.local.QuestionDao
import com.hz.appon.data.model.Category
import com.hz.appon.data.model.Difficulty
import com.hz.appon.data.model.Question
import com.hz.appon.data.remote.OpenTdbApi
import timber.log.Timber

/**
 * Production implementation of [QuestionRepository].
 *
 * Online path: OpenTDB API → shuffle options → insert into Room → return from Room.
 * Offline path: query Room cache → fall back to [BUNDLED_QUESTIONS] if insufficient.
 */
class QuestionRepositoryImpl(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao,
    private val api: OpenTdbApi
) : QuestionRepository {

    override suspend fun getCategories(): List<Category> {
        val cached = categoryDao.getAll()
        if (cached.isNotEmpty()) {
            Timber.d("Returning ${cached.size} cached categories")
            return cached
        }
        Timber.d("Fetching categories from OpenTDB")
        val response = api.getCategories()
        val categories = response.categories.map { Category(id = it.id, name = it.name) }
        categoryDao.insertAll(categories)
        return categories
    }

    override suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, count: Int): List<Question> {
        val cached = questionDao.countQuestions(categoryId, difficulty)
        if (cached >= count) {
            Timber.d("Returning $count cached ${difficulty.name} questions for category $categoryId")
            return questionDao.getQuestions(categoryId, difficulty, count)
        }
        Timber.d("Fetching $count ${difficulty.name} questions for category $categoryId from API")
        return try {
            val response = api.getQuestions(
                amount = maxOf(count, 10), // fetch extra to build cache
                categoryId = categoryId,
                difficulty = difficulty.name.lowercase()
            )
            val questions = response.results.mapIndexed { index, dto ->
                val correct = decode(dto.correctAnswer)
                val options = (dto.incorrectAnswers.map { decode(it) } + correct).shuffled()
                Question(
                    id = "${categoryId}_${difficulty.name}_$index",
                    categoryId = categoryId,
                    text = decode(dto.question),
                    options = options,
                    correctAnswer = correct,
                    difficulty = difficulty
                )
            }
            questionDao.insertAll(questions)
            questionDao.getQuestions(categoryId, difficulty, count)
        } catch (e: Exception) {
            Timber.w(e, "API fetch failed for category $categoryId, returning cache")
            questionDao.getQuestions(categoryId, difficulty, count)
        }
    }

    override suspend fun getBundledQuestions(): List<Question> {
        val stored = questionDao.getBundled()
        if (stored.isNotEmpty()) return stored
        questionDao.insertAll(BUNDLED_QUESTIONS)
        return BUNDLED_QUESTIONS
    }

    override suspend fun saveSelectedCategories(ids: List<Int>) {
        categoryDao.clearAllSelections()
        ids.forEach { categoryDao.setSelected(it, true) }
        Timber.d("Saved ${ids.size} selected categories")
    }

    override suspend fun getSelectedCategories(): List<Category> =
        categoryDao.getSelected()

    @Suppress("DEPRECATION")
    private fun decode(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()

    companion object {
        /** Five general-knowledge questions always available offline. */
        val BUNDLED_QUESTIONS = listOf(
            Question("bundled_0", -1, "What is the capital of France?",
                listOf("Paris", "London", "Berlin", "Madrid"), "Paris", Difficulty.EASY, true),
            Question("bundled_1", -1, "How many sides does a hexagon have?",
                listOf("5", "6", "7", "8"), "6", Difficulty.EASY, true),
            Question("bundled_2", -1, "Which planet is known as the Red Planet?",
                listOf("Venus", "Jupiter", "Mars", "Saturn"), "Mars", Difficulty.EASY, true),
            Question("bundled_3", -1, "What is the chemical symbol for water?",
                listOf("CO2", "H2O", "O2", "NaCl"), "H2O", Difficulty.MEDIUM, true),
            Question("bundled_4", -1, "Who wrote 'Romeo and Juliet'?",
                listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"),
                "William Shakespeare", Difficulty.MEDIUM, true)
        )
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.data.repository.QuestionRepositoryImplTest" 2>&1 | tail -8
```
Expected: `BUILD SUCCESSFUL` with all 4 tests passing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hz/appon/data/repository/ app/src/test/java/com/hz/appon/data/repository/
git commit -m "Add QuestionRepository with online/offline/bundled routing (TDD)"
```

---

## Task 7: Gamification Engine

**Files:** `GamificationModule.kt`, `GameEvent.kt`, `GamificationEngine.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/hz/appon/gamification/GamificationEngineTest.kt`
```kotlin
package com.hz.appon.gamification

import com.hz.appon.gamification.lives.LivesModule
import com.hz.appon.shared.UserPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GamificationEngineTest {

    private fun makeEngine(): Pair<GamificationEngine, LivesModule> {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = 5)
        val lives = LivesModule(prefs)
        val engine = GamificationEngine(listOf(lives))
        return engine to lives
    }

    @Test
    fun `initial state has full hearts`() {
        val (engine, _) = makeEngine()
        assertEquals(5, engine.gameState.value.lives.current)
    }

    @Test
    fun `WrongAnswer decrements lives by 1`() {
        val (engine, _) = makeEngine()
        engine.onEvent(GameEvent.WrongAnswer)
        assertEquals(4, engine.gameState.value.lives.current)
    }

    @Test
    fun `CorrectAnswer does not change lives`() {
        val (engine, _) = makeEngine()
        engine.onEvent(GameEvent.CorrectAnswer)
        assertEquals(5, engine.gameState.value.lives.current)
    }

    @Test
    fun `onEvent fans out to all modules`() {
        val module1 = mockk<GamificationModule>(relaxed = true)
        val module2 = mockk<GamificationModule>(relaxed = true)
        every { module1.getState() } returns LivesState(current = 5)
        every { module2.getState() } returns LivesState(current = 5)
        val engine = GamificationEngine(listOf(module1, module2))

        engine.onEvent(GameEvent.CorrectAnswer)

        verify { module1.onEvent(GameEvent.CorrectAnswer) }
        verify { module2.onEvent(GameEvent.CorrectAnswer) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.gamification.GamificationEngineTest" 2>&1 | tail -5
```
Expected: FAIL — types do not exist yet.

- [ ] **Step 3: Create GamificationModule.kt**

`app/src/main/java/com/hz/appon/gamification/GamificationModule.kt`
```kotlin
package com.hz.appon.gamification

/**
 * Contract for a single gamification system (lives, XP, streak, etc.).
 *
 * Add new systems by implementing this interface and passing the instance to
 * [GamificationEngine] — no existing code needs to change.
 */
interface GamificationModule {
    /** Called by [GamificationEngine] for every game event. */
    fun onEvent(event: GameEvent)

    /**
     * Returns this module's current state snapshot.
     * Cast to the concrete state type (e.g. [LivesState]) when reading in a ViewModel.
     */
    fun getState(): Any
}
```

- [ ] **Step 4: Create GameEvent.kt**

`app/src/main/java/com/hz/appon/gamification/GameEvent.kt`
```kotlin
package com.hz.appon.gamification

/**
 * Events emitted by the quiz flow and consumed by [GamificationEngine].
 * Each [GamificationModule] responds only to the events it cares about.
 */
sealed class GameEvent {
    object SessionStarted : GameEvent()
    object CorrectAnswer : GameEvent()
    object WrongAnswer : GameEvent()
    /** @param score Number of correct answers in the completed session. */
    data class SessionEnded(val score: Int) : GameEvent()
}
```

- [ ] **Step 5: Create GamificationEngine.kt**

`app/src/main/java/com/hz/appon/gamification/GamificationEngine.kt`
```kotlin
package com.hz.appon.gamification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import com.hz.appon.gamification.lives.LivesModule

/**
 * Orchestrates all gamification modules.
 *
 * Receives [GameEvent]s from the quiz layer, fans them out to every registered
 * [GamificationModule], then rebuilds and publishes a fresh [GameState].
 *
 * To add Option B (XP): pass `XpModule()` in [modules] — no changes here.
 */
class GamificationEngine(private val modules: List<GamificationModule>) {

    private val _gameState = MutableStateFlow(buildState())
    /** Current combined state of all gamification systems. */
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /**
     * Dispatches [event] to all modules and refreshes [gameState].
     * Call from [QuizViewModel] for every player action.
     */
    fun onEvent(event: GameEvent) {
        Timber.d("GameEvent: $event")
        modules.forEach { it.onEvent(event) }
        _gameState.value = buildState()
    }

    private fun buildState(): GameState {
        val livesState = modules
            .filterIsInstance<LivesModule>()
            .firstOrNull()
            ?.getState() as? LivesState
            ?: LivesState(current = 5)
        return GameState(lives = livesState)
    }
}
```

- [ ] **Step 6: Run test to verify it passes** (will fail until LivesModule exists — continue to Task 8 first, then re-run)

---

## Task 8: LivesModule + AppContainer

**Files:** `lives/LivesModule.kt`, `shared/AppContainer.kt`, update `App.kt`

- [ ] **Step 1: Write the failing LivesModule test**

`app/src/test/java/com/hz/appon/gamification/lives/LivesModuleTest.kt`
```kotlin
package com.hz.appon.gamification.lives

import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.UserPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class LivesModuleTest {

    private fun makeModule(current: Int = 5, lastLostAt: Long? = null): LivesModule {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = current, lastLostAt = lastLostAt)
        return LivesModule(prefs)
    }

    @Test
    fun `WrongAnswer decrements hearts by 1`() {
        val module = makeModule(current = 5)
        module.onEvent(GameEvent.WrongAnswer)
        assertEquals(4, (module.getState() as LivesState).current)
    }

    @Test
    fun `hearts cannot go below 0`() {
        val module = makeModule(current = 0)
        module.onEvent(GameEvent.WrongAnswer)
        assertEquals(0, (module.getState() as LivesState).current)
    }

    @Test
    fun `addHeart increments hearts by 1`() {
        val module = makeModule(current = 3)
        module.addHeart()
        assertEquals(4, (module.getState() as LivesState).current)
    }

    @Test
    fun `addHeart does not exceed max`() {
        val module = makeModule(current = 5)
        module.addHeart()
        assertEquals(5, (module.getState() as LivesState).current)
    }

    @Test
    fun `WrongAnswer persists state to preferences`() {
        val prefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.livesState } returns LivesState(current = 5)
        val module = LivesModule(prefs)
        module.onEvent(GameEvent.WrongAnswer)
        verify { prefs.livesState = any() }
    }

    @Test
    fun `SessionStarted recalculates refill — 60 minutes gives 2 hearts`() {
        val sixtyMinutesAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        val module = makeModule(current = 2, lastLostAt = sixtyMinutesAgo)
        module.onEvent(GameEvent.SessionStarted)
        assertEquals(4, (module.getState() as LivesState).current)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.gamification.lives.LivesModuleTest" 2>&1 | tail -5
```
Expected: FAIL

- [ ] **Step 3: Create LivesModule.kt**

`app/src/main/java/com/hz/appon/gamification/lives/LivesModule.kt`
```kotlin
package com.hz.appon.gamification.lives

import com.hz.appon.gamification.GameEvent
import com.hz.appon.gamification.GamificationModule
import com.hz.appon.gamification.LivesState
import com.hz.appon.shared.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Manages the hearts/lives system (Option A).
 *
 * Hearts are decremented on wrong answers and refilled at a rate of 1 per 30 minutes.
 * State is persisted to [UserPreferences] so hearts do not refill by restarting the app.
 *
 * To restore a heart after a rewarded ad, call [addHeart] directly.
 */
class LivesModule(private val userPreferences: UserPreferences) : GamificationModule {

    private val _state = MutableStateFlow(userPreferences.livesState)

    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.WrongAnswer -> decrementLives()
            is GameEvent.SessionStarted -> recalculateRefill()
            else -> Unit
        }
    }

    override fun getState(): LivesState = _state.value

    /**
     * Adds one heart, up to [LivesState.max].
     * Call after a rewarded ad completes successfully.
     */
    fun addHeart() {
        val current = _state.value
        if (current.current >= current.max) return
        val updated = current.copy(current = current.current + 1)
        persist(updated)
        Timber.d("Heart restored. Lives: ${updated.current}/${updated.max}")
    }

    private fun decrementLives() {
        val current = _state.value
        val updated = current.copy(
            current = maxOf(0, current.current - 1),
            lastLostAt = System.currentTimeMillis()
        )
        persist(updated)
        Timber.d("Wrong answer. Lives: ${updated.current}/${updated.max}")
    }

    private fun recalculateRefill() {
        val current = _state.value
        if (current.current >= current.max || current.lastLostAt == null) return

        val minutesElapsed = (System.currentTimeMillis() - current.lastLostAt) / 60_000
        val heartsToAdd = (minutesElapsed / REFILL_INTERVAL_MINUTES).toInt()

        if (heartsToAdd > 0) {
            val newCount = minOf(current.max, current.current + heartsToAdd)
            val updated = current.copy(
                current = newCount,
                lastLostAt = if (newCount >= current.max) null else current.lastLostAt
            )
            persist(updated)
            Timber.d("Refilled $heartsToAdd hearts. Lives: ${updated.current}/${updated.max}")
        }
    }

    private fun persist(state: LivesState) {
        _state.value = state
        userPreferences.livesState = state
    }

    companion object {
        /** One heart refills every 30 minutes. */
        private const val REFILL_INTERVAL_MINUTES = 30L
    }
}
```

- [ ] **Step 4: Run all gamification tests to verify they pass**

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat test --tests "com.hz.appon.gamification.*" 2>&1 | tail -8
```
Expected: All tests pass.

- [ ] **Step 5: Create AppContainer.kt**

`app/src/main/java/com/hz/appon/shared/AppContainer.kt`
```kotlin
package com.hz.appon.shared

import android.content.Context
import com.hz.appon.data.local.QuizDatabase
import com.hz.appon.data.remote.OpenTdbApi
import com.hz.appon.data.repository.QuestionRepository
import com.hz.appon.data.repository.QuestionRepositoryImpl
import com.hz.appon.gamification.GamificationEngine
import com.hz.appon.gamification.lives.LivesModule

/**
 * Manual dependency injection container. Created once in [App] and accessed via
 * `(application as App).container` in Activities.
 *
 * Replace individual components in tests by subclassing and overriding properties.
 */
class AppContainer(context: Context) {

    val userPreferences = UserPreferences(context)
    val networkMonitor = NetworkMonitor(context)

    private val database = QuizDatabase.getInstance(context)
    private val api = OpenTdbApi.create()

    val questionRepository: QuestionRepository = QuestionRepositoryImpl(
        questionDao = database.questionDao(),
        categoryDao = database.categoryDao(),
        api = api
    )

    private val livesModule = LivesModule(userPreferences)

    /** Shared engine — same instance used by all screens so state is consistent. */
    val gamificationEngine = GamificationEngine(listOf(livesModule))

    /** Exposes LivesModule so SessionEndViewModel can call addHeart() after a rewarded ad. */
    val livesModule_ = livesModule
}
```

- [ ] **Step 6: Update App.kt**

`app/src/main/java/com/hz/appon/App.kt`
```kotlin
package com.hz.appon

import android.app.Application
import com.hz.appon.shared.AppContainer
import timber.log.Timber

/**
 * Application entry point.
 * Initialises Timber logging and the dependency container.
 */
class App : Application() {

    /** Dependency container — access via `(application as App).container` in Activities. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = AppContainer(this)
        container.networkMonitor.register()
        Timber.d("App initialised")
    }
}
```

- [ ] **Step 7: Run full test suite**

```bash
make test
```
Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/hz/appon/gamification/ app/src/main/java/com/hz/appon/shared/ app/src/main/java/com/hz/appon/App.kt app/src/test/java/com/hz/appon/gamification/
git commit -m "Add GamificationEngine, LivesModule, AppContainer — Phase 1 complete"
```

---

## Phase 1 Complete

At this point the following is fully built and tested:
- Room database with Category and Question entities
- OpenTDB Retrofit client
- QuestionRepository (online/offline/bundled routing)
- UserPreferences and NetworkMonitor
- GamificationEngine with LivesModule (hearts system, refill timer)
- AppContainer (manual DI)

**Next:** `docs/superpowers/plans/2026-06-03-trivia-quiz-phase2-screens.md`
