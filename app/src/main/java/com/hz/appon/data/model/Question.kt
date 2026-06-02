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
