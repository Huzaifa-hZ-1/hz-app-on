package com.hz.appon.quiz

/** Controls which questions are fetched for a quiz session. */
enum class QuizMode {
    EASY,        // 10 easy questions
    MEDIUM,      // 10 medium questions
    HARD,        // 10 hard questions
    PROGRESSIVE  // 4 easy → 3 medium → 3 hard (default)
}
