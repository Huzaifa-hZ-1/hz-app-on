# Trivia Quiz App — Design Spec

**Date:** 2026-06-02  
**Status:** Approved  
**Platform:** Android only (minSdk 26)  
**Monetisation:** AdMob (banner, interstitial, rewarded)

---

## 1. Product Goal

A gamified trivia quiz app that earns ad revenue through repeated daily use. Free to build and publish. Users select their interest categories on first launch and answer questions with increasing difficulty. A lives/hearts system creates tension and drives rewarded ad engagement. The architecture is designed to extend to XP (Option B) and daily streaks (Option C) without modifying existing code.

---

## 2. User Flow

```
First launch:
  Splash → Onboarding (category selection) → Home

Every subsequent launch:
  Splash → Home

Re-selecting categories:
  Home (edit icon, top-right) → Onboarding (pre-checked) → Home

Playing:
  Home → Quiz → Session End → Home
              ↘ (hearts = 0) → Session End (game over) → Home
```

---

## 3. Screens

### 3.1 Splash
- Checks `hasCompletedOnboarding` flag
- Checks network connectivity (sets online/offline state)
- Routes to Onboarding (first launch) or Home (returning)
- No user interaction — auto-navigates after check completes

### 3.2 Onboarding
- Displays all OpenTDB categories as a selectable grid/list
- User must select at least 1 category to proceed
- "Continue" button enabled only when ≥ 1 category selected
- On re-entry from Home: pre-checks previously selected categories
- On save: persists selection to Room, sets `hasCompletedOnboarding = true`

### 3.3 Home
- Shows selected categories as tappable cards
- Hearts/lives display (top of screen)
- Offline banner (persistent, top): *"You're offline — 5 questions available"* — hidden when online
- Edit icon (top-right) → opens Onboarding for re-selection
- Tapping a category card → starts Quiz session for that category
- Banner ad at bottom (always visible)

### 3.4 Quiz
- Header: category name, hearts display, question counter (e.g. "3 / 10")
- 15-second countdown timer per question (progress bar)
- Question text
- 4 answer buttons — disabled immediately on tap
- Visual feedback: correct answer turns green, wrong turns red + correct highlighted
- On wrong answer: 1 heart deducted, brief shake animation
- On timer expiry: treated as wrong answer
- If hearts reach 0 mid-session: navigate to Session End (game over state)
- 10 questions per session: 4 easy → 3 medium → 3 hard
- Offline session: 5 bundled questions only (same difficulty mix, proportionally fewer)

### 3.5 Session End
- Shows: score (e.g. "7 / 10"), correct count, wrong count, category name
- Game over variant: shows "You ran out of hearts" message
- "Watch ad to restore 1 heart" button — visible only if hearts < max AND a rewarded ad is loaded
- "Play Again" button (same category)
- "Go Home" button
- Interstitial ad shown automatically on every 3rd completed session

---

## 4. Package Structure

```
com.hz.appon/
├── onboarding/
│   ├── OnboardingActivity.kt
│   ├── OnboardingViewModel.kt
│   └── CategoryAdapter.kt
├── home/
│   ├── HomeActivity.kt
│   └── HomeViewModel.kt
├── quiz/
│   ├── QuizActivity.kt
│   ├── QuizViewModel.kt
│   ├── SessionEndActivity.kt
│   └── SessionEndViewModel.kt
├── gamification/
│   ├── GamificationEngine.kt
│   ├── GamificationModule.kt       (interface)
│   ├── GameEvent.kt                (sealed class)
│   ├── GameState.kt                (data class)
│   └── lives/
│       └── LivesModule.kt
├── data/
│   ├── local/
│   │   ├── QuizDatabase.kt
│   │   ├── QuestionDao.kt
│   │   └── CategoryDao.kt
│   ├── remote/
│   │   ├── OpenTdbApi.kt
│   │   └── OpenTdbResponse.kt
│   ├── repository/
│   │   ├── QuestionRepository.kt   (interface)
│   │   └── QuestionRepositoryImpl.kt
│   └── model/
│       ├── Question.kt
│       ├── Category.kt
│       ├── Difficulty.kt
│       └── QuizSession.kt
├── ads/
│   └── AdManager.kt
└── shared/
    ├── NetworkMonitor.kt
    └── UserPreferences.kt
```

---

## 5. Data Models

```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Int,
    val name: String,
    val isSelected: Boolean = false
)

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: String,         // "{categoryId}_{difficulty}_{index}"
    val categoryId: Int,
    val text: String,
    val options: List<String>,          // 4 options, shuffled at fetch time — needs @TypeConverter
    val correctAnswer: String,
    val difficulty: Difficulty,
    val isBundled: Boolean = false
)

enum class Difficulty { EASY, MEDIUM, HARD }

// In-memory only — not a Room entity
data class QuizSession(
    val categoryId: Int,
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val isOffline: Boolean = false
)

// GameState: slots for B and C exist now, null until implemented
data class GameState(
    val lives: LivesState,
    val xp: XpState? = null,            // Option B — future
    val streak: StreakState? = null     // Option C — future
)

data class LivesState(
    val current: Int,
    val max: Int = 5,
    val lastLostAt: Long? = null        // epoch ms, drives refill timer
)
```

---

## 6. Gamification Architecture

```kotlin
interface GamificationModule {
    fun onEvent(event: GameEvent)
    fun getState(): Any             // LivesState / XpState / StreakState
}

sealed class GameEvent {
    object SessionStarted : GameEvent()
    object CorrectAnswer : GameEvent()
    object WrongAnswer : GameEvent()
    data class SessionEnded(val score: Int) : GameEvent()
}

class GamificationEngine(private val modules: List<GamificationModule>) {
    private val _gameState = MutableStateFlow(buildInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun onEvent(event: GameEvent) {
        modules.forEach { it.onEvent(event) }
        _gameState.value = buildCurrentState()
    }
}
```

**Adding Option B later:**
1. Create `XpState` data class
2. Create `XpModule : GamificationModule`
3. Pass `XpModule()` into `GamificationEngine` constructor
4. Update `GameState` to populate `xp` field
5. Zero changes to `LivesModule`, `QuizViewModel`, or any existing code

---

## 7. Repository Contract

```kotlin
interface QuestionRepository {
    suspend fun getCategories(): List<Category>
    suspend fun getQuestions(categoryId: Int, difficulty: Difficulty, count: Int): List<Question>
    suspend fun getBundledQuestions(): List<Question>    // always 5, always available
    suspend fun saveSelectedCategories(ids: List<Int>)
    suspend fun getSelectedCategories(): List<Category>
}
```

**Online path:** OpenTDB API → shuffle options → insert into Room → return  
**Offline path:** Query Room cache → if insufficient, fall back to bundled questions  
`QuizViewModel` never knows which path was taken.

---

## 8. Ad Flow

| Placement | Trigger | Type |
|---|---|---|
| Home — bottom | `HomeActivity.onCreate` | Banner (always visible) |
| Session End | Every 3rd `sessionsPlayedCount` | Interstitial (auto) |
| Session End — "Watch ad" button | User tap, only if hearts < 5 | Rewarded → `LivesModule.addHeart()` |

`sessionsPlayedCount` stored in `UserPreferences`, incremented by `SessionEndViewModel` on every session completion (regardless of win/loss).

---

## 9. Offline Behaviour

- `NetworkMonitor` exposes `isOnline: StateFlow<Boolean>` via `ConnectivityManager`
- `HomeViewModel` observes it → shows/hides offline banner
- `QuizViewModel` observes it → passes `isOffline` flag to `QuestionRepositoryImpl`
- Bundled questions: 5 hardcoded `Question` objects in `QuestionRepositoryImpl`, one per category of the selected categories or generic if unmatched
- Offline session follows identical flow — same screens, same hearts system, just fewer questions

---

## 10. Testing Strategy

| Layer | Tool | What gets tested |
|---|---|---|
| `GamificationEngine` + `LivesModule` | JUnit + MockK | Event fan-out, heart decrement/refill logic, state transitions |
| `QuestionRepositoryImpl` | JUnit + MockK | Online/offline routing, cache logic, fallback to bundled |
| `QuizViewModel` | JUnit + MockK | Session progression, navigation events, game over trigger |
| `OnboardingViewModel` | JUnit + MockK | Category toggle, save, validation (≥1 selected) |
| `NetworkMonitor` | Robolectric | Connectivity state changes |
| Quiz happy path | Espresso | Full session from Home → Quiz → Session End |

---

## 11. Out of Scope (This Iteration)

- XP / levelling system (Option B)
- Daily streaks (Option C)
- Leaderboards
- User accounts / cloud sync
- Push notifications
- Question reporting / feedback
- Resume interrupted session
