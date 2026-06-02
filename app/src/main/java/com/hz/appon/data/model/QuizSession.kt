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
