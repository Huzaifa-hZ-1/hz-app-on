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
